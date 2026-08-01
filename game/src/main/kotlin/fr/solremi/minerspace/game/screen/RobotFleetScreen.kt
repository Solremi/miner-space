package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ScreenUtils
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.data.transaction.RobotStateTransactionCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.robot.PendingDeposit
import fr.solremi.minerspace.domain.robot.QueueTask
import fr.solremi.minerspace.domain.robot.RobotAutomationEngine
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.robot.RobotCommandResult
import fr.solremi.minerspace.domain.robot.RobotFamily
import fr.solremi.minerspace.domain.robot.RobotInstance
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import ktx.app.KtxScreen

class RobotFleetScreen(
    private val services: GameServices,
    private val onBack: () -> Unit,
) : KtxScreen {
    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val definitions = RobotContentLoader().load(services.content)
    private val engine = RobotAutomationEngine(definitions)
    private val ui = RobotFleetUi(engine)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val fleetCodec = RobotFleetCodec()
    private val transactions = RobotStateTransactionCoordinator(
        services.save,
        services.clock,
        services.logger,
    )

    private var main = loadMain()
    private var fleet = loadFleet()
    private var selected = fleet.robots.values.first().id
    private var message = "Automatisation active"
    private var lastTick = services.clock.monotonicMillis()
    private var layout: RobotFleetUi.Layout? = null
    private var persistenceBlocked = false

    private val lifecycle = LifecycleObserver {
        if (it == LifecycleState.BACKGROUND && !persistenceBlocked) saveFleet()
    }
    private val input = object : InputAdapter() {
        override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
            touch(ui.unproject(x, y))
            return true
        }
    }

    override fun show() {
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
        main = loadMain()
        fleet = engine.normalize(loadFleet(), now())
        persistenceBlocked = false
        rebalance()
        transfer(force = true)
        lastTick = services.clock.monotonicMillis()
    }

    override fun hide() {
        if (!persistenceBlocked) saveFleet()
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) = ui.resize(width, height)

    override fun render(delta: Float) {
        val tick = services.clock.monotonicMillis()
        if (!persistenceBlocked && tick - lastTick >= 1_000L) {
            transfer(force = false)
            lastTick = tick
        }
        ScreenUtils.clear(BACKGROUND)
        val robot = fleet.robots.getValue(selected)
        layout = ui.draw(fleet, robot, tasks(robot), message)
    }

    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)

    private fun initialMain() = ManufacturingGameState(
        economy.initialState(),
        RefiningState.empty(),
        AssemblyState.empty(),
    )

    private fun loadMain(): ManufacturingGameState = services.save.loadLatest()?.let { payload ->
        runCatching {
            require(payload.contentVersion == economyDefinitions.contentVersion)
            mainCodec.decode(payload)
        }.onFailure {
            services.logger.warning(TAG, "Unable to load manufacturing state for robots.", it)
        }.getOrNull()
    } ?: initialMain()

    private fun loadFleet(): RobotAutomationState = services.save.loadLatest(RobotFleetCodec.SLOT_ID)?.let { payload ->
        runCatching {
            require(payload.contentVersion == definitions.contentVersion)
            fleetCodec.decode(payload)
        }.onFailure {
            services.logger.warning(TAG, "Unable to load robot fleet state.", it)
        }.getOrNull()
    } ?: engine.initialState(now())

    private fun saveMain(value: ManufacturingGameState): Boolean = services.save.save(
        mainCodec.encode(value, economyDefinitions.contentVersion, savedAtEpochMillis = now()),
    ) == SaveWriteStatus.WRITTEN

    private fun saveFleet(value: RobotAutomationState = fleet): Boolean = services.save.save(
        fleetCodec.encode(value, definitions.contentVersion, now()),
    ) == SaveWriteStatus.WRITTEN

    private fun commitBoth(
        nextMain: ManufacturingGameState,
        nextFleet: RobotAutomationState,
        reason: String,
    ): Boolean {
        val savedAt = now()
        val result = transactions.commit(
            main = nextMain,
            robots = nextFleet,
            mainContentVersion = economyDefinitions.contentVersion,
            robotContentVersion = definitions.contentVersion,
            reason = reason,
            savedAtEpochMillis = savedAt,
        )
        if (!result.committed) {
            persistenceBlocked = true
            message = "Transaction en attente · relancez l'application"
            services.haptic.warning()
            return false
        }
        main = nextMain
        fleet = nextFleet
        return true
    }

    private fun transfer(force: Boolean) {
        val result = engine.advanceLogistics(
            state = fleet,
            deposits = economyDefinitions.deposits.values.map { definition ->
                PendingDeposit(
                    definition.id,
                    definition.resourceId,
                    main.economy.deposits.getValue(definition.id).pendingCollection,
                )
            },
            inventory = main.economy.inventory,
            storageCapacities = economyDefinitions.resources.mapValues { it.value.storageCapacity },
            unitSalePrices = economyDefinitions.resources.mapValues { it.value.unitSalePrice },
            nowEpochMillis = now(),
        )
        if (result.totalMoved == 0L) {
            val previous = fleet
            fleet = result.automation
            if (force && !saveFleet()) {
                fleet = previous
                message = "État robot non sauvegardé"
            }
            return
        }

        val nextMain = main.copy(
            economy = main.economy.copy(
                inventory = result.inventory,
                deposits = main.economy.deposits.mapValues { (id, state) ->
                    state.copy(pendingCollection = result.pendingByDeposit[id] ?: state.pendingCollection)
                },
                transactionSequence = Math.addExact(main.economy.transactionSequence, 1L),
            ),
        )
        if (commitBoth(nextMain, result.automation, "logistics")) {
            message = "LG-01 : ${result.totalMoved} transférée(s)"
        }
    }

    private data class Timed(
        val id: String,
        val queued: Long,
        val start: Long,
        val finish: Long,
    )

    private fun schedule(
        tasks: List<Timed>,
        lanes: Int,
        time: Long,
    ): Map<String, Pair<Long, Long>> {
        val ends = LongArray(lanes) { time }
        return tasks.sortedBy { it.queued }.associate { task ->
            val lane = ends.indices.minBy { ends[it] }
            val start = maxOf(time, ends[lane])
            val finish = Math.addExact(start, (task.finish - task.start).coerceAtLeast(1L))
            ends[lane] = finish
            task.id to (start to finish)
        }
    }

    private fun rebalancedMain(
        sourceMain: ManufacturingGameState,
        sourceFleet: RobotAutomationState,
    ): ManufacturingGameState {
        val time = now()
        val refiningSchedule = schedule(
            sourceMain.refining.jobs
                .filter { it.status == RefiningJobStatus.QUEUED }
                .map { Timed(it.id, it.queuedAtEpochMillis, it.startsAtEpochMillis, it.finishesAtEpochMillis) },
            engine.queueCount(robot(sourceFleet, RobotFamily.REFINER)),
            time,
        )
        val assemblySchedule = schedule(
            sourceMain.assembly.jobs
                .filter { it.status == AssemblyJobStatus.QUEUED }
                .map { Timed(it.id, it.queuedAtEpochMillis, it.startsAtEpochMillis, it.finishesAtEpochMillis) },
            engine.queueCount(robot(sourceFleet, RobotFamily.ASSEMBLER)),
            time,
        )
        if (refiningSchedule.isEmpty() && assemblySchedule.isEmpty()) return sourceMain
        return sourceMain.copy(
            refining = sourceMain.refining.copy(
                jobs = sourceMain.refining.jobs.map { job ->
                    refiningSchedule[job.id]?.let { timing ->
                        job.copy(startsAtEpochMillis = timing.first, finishesAtEpochMillis = timing.second)
                    } ?: job
                },
            ),
            assembly = sourceMain.assembly.copy(
                jobs = sourceMain.assembly.jobs.map { job ->
                    assemblySchedule[job.id]?.let { timing ->
                        job.copy(startsAtEpochMillis = timing.first, finishesAtEpochMillis = timing.second)
                    } ?: job
                },
            ),
        )
    }

    private fun rebalance() {
        val next = rebalancedMain(main, fleet)
        if (next != main && saveMain(next)) main = next
    }

    private fun tasks(robot: RobotInstance): List<QueueTask> = when (robot.family) {
        RobotFamily.REFINER -> main.refining.jobs.take(6).map {
            QueueTask(it.id, ((it.finishesAtEpochMillis - it.startsAtEpochMillis) / 1_000L).coerceAtLeast(1L))
        }
        RobotFamily.ASSEMBLER -> main.assembly.jobs.take(6).map {
            QueueTask(it.id, ((it.finishesAtEpochMillis - it.startsAtEpochMillis) / 1_000L).coerceAtLeast(1L))
        }
        else -> (1..6).map { QueueTask("${robot.family}_$it", 7L + it * 4L) }
    }

    private fun touch(point: com.badlogic.gdx.math.Vector2) {
        if (persistenceBlocked) {
            message = "Relancez l'application pour terminer la transaction"
            services.haptic.warning()
            return
        }
        val current = layout ?: return
        current.cards.forEachIndexed { index, rectangle ->
            if (rectangle.contains(point)) {
                selected = ordered()[index].id
                services.haptic.impact()
                return
            }
        }
        when {
            current.priority.contains(point) -> priority()
            current.upgrade.contains(point) -> upgrade()
            current.quality.contains(point) -> cycleQuality()
            current.back.contains(point) -> onBack()
        }
    }

    private fun priority() {
        when (val result = engine.cyclePriority(fleet, selected)) {
            is RobotCommandResult.Rejected -> services.haptic.warning()
            is RobotCommandResult.Applied -> {
                val previous = fleet
                if (saveFleet(result.state)) {
                    fleet = result.state
                    message = fleet.robots.getValue(selected).priority.name
                    services.haptic.success()
                } else {
                    fleet = previous
                    message = "Priorité non sauvegardée"
                    services.haptic.warning()
                }
            }
        }
    }

    private fun cycleQuality() {
        val previous = fleet
        val next = engine.cycleQuality(fleet)
        if (saveFleet(next)) {
            fleet = next
            message = "${engine.visibleUnitCount(fleet)} unités"
            services.haptic.impact()
        } else {
            fleet = previous
            message = "Qualité non sauvegardée"
            services.haptic.warning()
        }
    }

    private fun upgrade() {
        when (val result = engine.upgrade(fleet, selected, main.economy.spaceDollars)) {
            is RobotCommandResult.Rejected -> {
                message = if (result.code == "insufficient_space_dollars") {
                    "SpaceDollars insuffisants"
                } else {
                    "Niveau maximal"
                }
                services.haptic.warning()
            }
            is RobotCommandResult.Applied -> {
                val baseMain = main.copy(
                    economy = main.economy.copy(
                        spaceDollars = main.economy.spaceDollars - result.transaction.spaceDollarCost,
                        transactionSequence = Math.addExact(main.economy.transactionSequence, 1L),
                    ),
                )
                val nextMain = rebalancedMain(baseMain, result.state)
                if (commitBoth(nextMain, result.state, "upgrade")) {
                    message = "Niveau ${fleet.robots.getValue(selected).level}"
                    services.haptic.success()
                }
            }
        }
    }

    private fun robot(state: RobotAutomationState, family: RobotFamily): RobotInstance =
        state.robots.values.first { it.family == family }

    private fun ordered(): List<RobotInstance> = fleet.robots.values.sortedBy { it.family.ordinal }

    override fun dispose() {
        hide()
        ui.dispose()
    }

    private companion object {
        const val TAG = "RobotFleetScreen"
        val BACKGROUND = Color(.008f, .014f, .03f, 1f)
    }
}
