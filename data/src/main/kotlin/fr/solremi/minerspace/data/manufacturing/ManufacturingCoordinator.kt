package fr.solremi.minerspace.data.manufacturing

import fr.solremi.minerspace.data.assembly.AssemblyContentLoader
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.refining.RefiningContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.domain.assembly.AssemblyCommandResult
import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyEngine
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyCommandResult
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.advanceExtraction
import fr.solremi.minerspace.domain.refining.RefiningCommandResult
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.GameLogger

sealed interface ManufacturingActionResult {
    val state: ManufacturingGameState

    data class Applied(
        override val state: ManufacturingGameState,
        val reason: String,
    ) : ManufacturingActionResult

    data class Rejected(
        override val state: ManufacturingGameState,
        val code: String,
    ) : ManufacturingActionResult

    data class PersistenceFailed(
        override val state: ManufacturingGameState,
        val attemptedReason: String,
    ) : ManufacturingActionResult
}

data class ManufacturingTickResult(
    val state: ManufacturingGameState,
    val changed: Boolean,
    val autosaveFailed: Boolean,
)

class ManufacturingCoordinator(
    private val save: SaveService,
    private val clock: ClockService,
    private val logger: GameLogger,
    val economyDefinitions: EconomyDefinitions,
    val refiningDefinitions: RefiningDefinitions,
    val assemblyDefinitions: AssemblyDefinitions,
) {
    val economy = CoreEconomyEngine(economyDefinitions)
    val refiner = RefiningEngine(
        refiningDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )
    val assembler = AssemblyEngine(
        assemblyDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )

    private val codec = ManufacturingSnapshotCodec()
    private var lastTickMillis = 0L
    private var remainderMillis = 0L
    private var lastAutosaveMillis = 0L

    var state: ManufacturingGameState = loadState()
        private set

    fun start() {
        lastTickMillis = clock.monotonicMillis().coerceAtLeast(0L)
        lastAutosaveMillis = lastTickMillis
        state = reconcile(state)
    }

    fun tick(): ManufacturingTickResult {
        val monotonicNow = clock.monotonicMillis().coerceAtLeast(0L)
        remainderMillis = Math.addExact(
            remainderMillis,
            (monotonicNow - lastTickMillis).coerceAtLeast(0L),
        )
        lastTickMillis = monotonicNow
        var candidate = state
        var changed = false
        val seconds = remainderMillis / 1_000L
        if (seconds > 0L) {
            remainderMillis %= 1_000L
            val extraction = economy.advanceExtraction(
                candidate.economy,
                seconds,
                assembler.productionMultipliers(candidate.assembly),
            )
            if (extraction.state != candidate.economy) {
                candidate = candidate.copy(economy = extraction.state)
                changed = true
            }
        }
        val reconciled = reconcile(candidate)
        if (reconciled != candidate) changed = true
        state = reconciled

        var autosaveFailed = false
        if (changed && monotonicNow - lastAutosaveMillis >= AUTOSAVE_INTERVAL_MILLIS) {
            autosaveFailed = !saveState(state)
            if (!autosaveFailed) lastAutosaveMillis = monotonicNow
        }
        return ManufacturingTickResult(state, changed, autosaveFailed)
    }

    fun save(): Boolean = saveState(state)

    fun sellAll(): ManufacturingActionResult = applyEconomy(
        economy.sellAllSellable(state.economy),
    )

    fun collectDeposit(depositId: GameId): ManufacturingActionResult = applyEconomy(
        economy.collect(state.economy, depositId),
    )

    fun launchRefining(recipeId: GameId): ManufacturingActionResult = applyRefining(
        refiner.launch(state.refiningView(), recipeId, now()),
    )

    fun collectRefining(jobId: String): ManufacturingActionResult = applyRefining(
        refiner.collect(state.refiningView(), jobId, now()),
    )

    fun cancelRefining(jobId: String): ManufacturingActionResult = applyRefining(
        refiner.cancel(state.refiningView(), jobId, now()),
    )

    fun collectRefiningRefunds(): ManufacturingActionResult = applyRefining(
        refiner.collectRefunds(state.refiningView()),
    )

    fun launchAssembly(recipeId: GameId): ManufacturingActionResult = applyAssembly(
        assembler.launch(state, recipeId, now()),
    )

    fun collectAssembly(jobId: String): ManufacturingActionResult = applyAssembly(
        assembler.collect(state, jobId, now()),
    )

    fun installTechnology(technologyId: GameId): ManufacturingActionResult = applyAssembly(
        assembler.installTechnology(state, technologyId),
    )

    fun canLaunchRefining(recipeId: GameId): Boolean {
        if (state.refining.jobs.size >= refiningDefinitions.robot.queueCapacity) return false
        val recipe = refiningDefinitions.recipes[recipeId] ?: return false
        return recipe.inputs.all { (id, quantity) -> stock(id) >= quantity }
    }

    fun canLaunchAssembly(recipeId: GameId): Boolean {
        if (state.assembly.jobs.size >= assemblyDefinitions.robot.queueCapacity) return false
        val recipe = assemblyDefinitions.recipes[recipeId] ?: return false
        if (!state.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)) return false
        return recipe.inputs.all { (id, quantity) -> stock(id) >= quantity }
    }

    fun canInstallTechnology(technologyId: GameId): Boolean {
        val technology = assemblyDefinitions.technologies[technologyId] ?: return false
        if (technology.id in state.assembly.installedTechnologyIds) return false
        if (!state.assembly.installedTechnologyIds.containsAll(technology.requiredTechnologyIds)) return false
        return stock(technology.itemResourceId) > 0L
    }

    fun stock(resourceId: GameId): Long = state.economy.inventory[resourceId] ?: 0L

    private fun applyEconomy(result: EconomyCommandResult): ManufacturingActionResult = when (result) {
        is EconomyCommandResult.Rejected -> ManufacturingActionResult.Rejected(state, result.code)
        is EconomyCommandResult.Applied -> commit(
            candidate = state.copy(economy = result.state),
            reason = result.transaction.reason,
        )
    }

    private fun applyRefining(result: RefiningCommandResult): ManufacturingActionResult = when (result) {
        is RefiningCommandResult.Rejected -> ManufacturingActionResult.Rejected(state, result.code)
        is RefiningCommandResult.Applied -> commit(
            candidate = state.withRefining(result.state),
            reason = result.transaction.reason,
        )
    }

    private fun applyAssembly(result: AssemblyCommandResult): ManufacturingActionResult = when (result) {
        is AssemblyCommandResult.Rejected -> ManufacturingActionResult.Rejected(state, result.code)
        is AssemblyCommandResult.Applied -> commit(
            candidate = result.state,
            reason = result.transaction.reason,
        )
    }

    private fun commit(
        candidate: ManufacturingGameState,
        reason: String,
    ): ManufacturingActionResult {
        if (!saveState(candidate)) {
            logger.warning(TAG, "Manufacturing action was rolled back because persistence failed: $reason")
            return ManufacturingActionResult.PersistenceFailed(state, reason)
        }
        state = candidate
        return ManufacturingActionResult.Applied(state, reason)
    }

    private fun loadState(): ManufacturingGameState {
        val initial = ManufacturingGameState(
            economy = economy.initialState(),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        val payload = runCatching { save.loadLatest() }
            .onFailure { logger.error(TAG, "Unable to read manufacturing save.", it) }
            .getOrNull()
            ?: return initial
        return runCatching {
            require(payload.contentVersion == economyDefinitions.contentVersion)
            require(refiningDefinitions.contentVersion == payload.contentVersion)
            require(assemblyDefinitions.contentVersion == payload.contentVersion)
            val restored = codec.decode(payload)
            economy.requireValid(restored.economy)
            reconcile(restored)
        }.onFailure {
            logger.warning(TAG, "Manufacturing save is invalid; the initial state is used.", it)
        }.getOrElse { initial }
    }

    private fun reconcile(source: ManufacturingGameState): ManufacturingGameState {
        val now = now()
        val withRefining = source.withRefining(refiner.reconcile(source.refiningView(), now))
        return assembler.reconcile(withRefining, now)
    }

    private fun saveState(candidate: ManufacturingGameState): Boolean {
        val status = runCatching {
            save.save(
                codec.encode(
                    state = candidate,
                    contentVersion = economyDefinitions.contentVersion,
                    savedAtEpochMillis = now(),
                ),
            )
        }.onFailure {
            logger.error(TAG, "Unable to persist manufacturing state.", it)
        }.getOrElse { SaveWriteStatus.FAILED }
        return status == SaveWriteStatus.WRITTEN
    }

    private fun now(): Long = clock.nowEpochMillis().coerceAtLeast(0L)

    companion object {
        private const val TAG = "ManufacturingCoordinator"
        private const val AUTOSAVE_INTERVAL_MILLIS = 5_000L

        fun fromServices(services: GameServices): ManufacturingCoordinator =
            ManufacturingCoordinator(
                save = services.save,
                clock = services.clock,
                logger = services.logger,
                economyDefinitions = CoreEconomyContentLoader().load(services.content),
                refiningDefinitions = RefiningContentLoader().load(services.content),
                assemblyDefinitions = AssemblyContentLoader().load(services.content),
            )
    }
}
