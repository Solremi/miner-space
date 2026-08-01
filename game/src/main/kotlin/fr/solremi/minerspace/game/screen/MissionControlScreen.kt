package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.progression.ProgressionContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.ProgressionStateCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.data.save.SectorProgressCodec
import fr.solremi.minerspace.data.save.StrategyStateCodec
import fr.solremi.minerspace.data.transaction.ProgressionStateTransactionCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.exploration.ExplorationState
import fr.solremi.minerspace.domain.progression.ProgressSnapshot
import fr.solremi.minerspace.domain.progression.ProgressionCommandResult
import fr.solremi.minerspace.domain.progression.ProgressionEngine
import fr.solremi.minerspace.domain.progression.ProgressionState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.domain.strategy.StrategyState
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class MissionControlScreen(
    private val services: GameServices,
    private val onBack: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.78f) }
    private val small = BitmapFont().apply { data.setScale(.61f) }

    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val definitions = ProgressionContentLoader().load(services.content)
    private val engine = ProgressionEngine(definitions)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val progressionCodec = ProgressionStateCodec()
    private val sectorCodec = SectorProgressCodec()
    private val robotCodec = RobotFleetCodec()
    private val strategyCodec = StrategyStateCodec()
    private val transactions = ProgressionStateTransactionCoordinator(
        services.save,
        services.clock,
        services.logger,
    )

    private var main = initialMain()
    private var progression = engine.initialState()
    private var exploration: ExplorationState? = null
    private var robots: RobotAutomationState? = null
    private var strategy: StrategyState? = null
    private var snapshot = snapshot()
    private var tab = Tab.OBJECTIVES
    private var selected = 0
    private var message = "Objectifs synchronisés"
    private var currentLayout: Layout? = null
    private var persistenceBlocked = false

    private val lifecycle = LifecycleObserver {
        if (it == LifecycleState.BACKGROUND && !persistenceBlocked) saveProgression()
    }
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject))
            return true
        }
    }

    override fun show() {
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
        persistenceBlocked = false
        reload()
    }

    override fun hide() {
        if (!persistenceBlocked) saveProgression()
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) =
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        val current = layout()
        currentLayout = current
        drawPanels(current)
        drawText(current)
    }

    private fun initialMain() = ManufacturingGameState(
        economy.initialState(),
        RefiningState.empty(),
        AssemblyState.empty(),
    )

    private fun reload() {
        main = services.save.loadLatest()?.let { payload ->
            runCatching { mainCodec.decode(payload) }
                .onFailure { services.logger.warning(TAG, "Unable to load manufacturing state for missions.", it) }
                .getOrNull()
        } ?: initialMain()
        exploration = services.save.loadLatest(SectorProgressCodec.SLOT_ID)?.let { payload ->
            runCatching { sectorCodec.decode(payload) }.getOrNull()
        }
        robots = services.save.loadLatest(RobotFleetCodec.SLOT_ID)?.let { payload ->
            runCatching { robotCodec.decode(payload) }.getOrNull()
        }
        strategy = services.save.loadLatest(StrategyStateCodec.SLOT_ID)?.let { payload ->
            runCatching { strategyCodec.decode(payload) }.getOrNull()
        }
        progression = services.save.loadLatest(ProgressionStateCodec.SLOT_ID)?.let { payload ->
            runCatching {
                require(payload.contentVersion == definitions.contentVersion)
                progressionCodec.decode(payload)
            }.onFailure {
                services.logger.warning(TAG, "Unable to load progression state.", it)
            }.getOrNull()
        } ?: engine.initialState()

        snapshot = snapshot()
        val synchronized = engine.synchronize(progression, snapshot)
        if (synchronized != progression) {
            progression = synchronized
            saveProgression()
        }
        selected = 0
    }

    private fun snapshot(mainState: ManufacturingGameState = main): ProgressSnapshot = ProgressSnapshot(
        inventory = mainState.economy.inventory,
        spaceDollars = mainState.economy.spaceDollars,
        installedTechnologyCount = mainState.assembly.installedTechnologyIds.size,
        unlockedSectorCount = exploration?.unlockedSectorIds?.size ?: 1,
        rareDiscoveryCount = exploration?.discoveredRareDepositIds?.size ?: 0,
        robotLevelSum = robots?.robots?.values?.sumOf { it.level } ?: 4,
        robotMasteryPoints = robots?.robots?.values?.sumOf { it.masteryPoints } ?: 0L,
        ownedModuleCount = strategy?.modules?.size ?: 0,
        specializationChosen = strategy?.activeSpecialization != null,
    )

    private fun saveProgression(value: ProgressionState = progression): Boolean = services.save.save(
        progressionCodec.encode(value, definitions.contentVersion, now()),
    ) == SaveWriteStatus.WRITTEN

    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)

    private fun drawPanels(layout: Layout) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = TUTORIAL
        shapes.rect(layout.tutorial.x, layout.tutorial.y, layout.tutorial.width, layout.tutorial.height)
        rows().forEachIndexed { index, _ ->
            val row = layout.rows[index]
            shapes.color = if (index == selected) SELECTED else PANEL
            shapes.rect(row.x, row.y, row.width, row.height)
            shapes.color = if (index == selected) ACCENT else GRID
            shapes.rect(row.x, row.y, 4f, row.height)
        }
        button(layout.tab, !persistenceBlocked)
        button(layout.action, !persistenceBlocked && actionAvailable())
        button(layout.pin, !persistenceBlocked && tab == Tab.OBJECTIVES && rows().isNotEmpty())
        button(layout.back, !persistenceBlocked)
        shapes.end()
    }

    private fun drawText(layout: Layout) {
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        small.color = MUTED
        font.draw(batch, "MISSIONS · ${tab.label}", layout.top.x + 12f, layout.top.y + layout.top.height - 14f)
        small.draw(
            batch,
            "${main.economy.spaceDollars} SD · ${engine.objectiveViews(progression, snapshot).size} objectifs actifs",
            layout.top.x + 12f,
            layout.top.y + 15f,
        )
        val tutorial = engine.tutorialProgress(progression, snapshot)
        font.draw(
            batch,
            tutorial.step?.let { "${it.phaseLabel} · ${it.actionKey}" }
                ?: "Tutoriel de la première semaine terminé",
            layout.tutorial.x + 12f,
            layout.tutorial.y + layout.tutorial.height - 14f,
        )
        small.draw(
            batch,
            tutorial.step?.let { "${tutorial.current}/${it.target} · reprise automatique après fermeture" }
                ?: "${tutorial.completed}/${tutorial.total} étapes",
            layout.tutorial.x + 12f,
            layout.tutorial.y + 15f,
        )
        rows().forEachIndexed { index, item ->
            val row = layout.rows[index]
            font.draw(batch, item.title, row.x + 14f, row.y + row.height - 13f)
            small.draw(batch, item.detail, row.x + 14f, row.y + 14f)
        }
        small.draw(batch, message, layout.message.x, layout.message.y + 15f)
        label(layout.tab, "ONGLET")
        label(layout.action, "ACTION")
        label(layout.pin, "SUIVRE")
        label(layout.back, "RETOUR")
        batch.end()
    }

    private fun rows(): List<RowItem> = when (tab) {
        Tab.OBJECTIVES -> engine.objectiveViews(progression, snapshot).take(4).map { view ->
            RowItem(
                view.definition.id.value,
                view.definition.titleKey,
                "${view.definition.kind.name} · ${view.current}/${view.definition.target} · " +
                    "${view.definition.rewardSpaceDollars} SD${if (view.completed) " · À RÉCUPÉRER" else ""}",
            )
        }
        Tab.CONTRACTS -> engine.activeContracts(progression, snapshot).map { view ->
            RowItem(
                view.occurrenceId,
                view.definition.titleKey,
                "${view.definition.tier.name} · ${view.currentInventory}/${view.definition.quantity} · " +
                    "${view.definition.rewardSpaceDollars} SD${if (!view.unlocked) " · VERROUILLÉ" else if (view.deliverable) " · LIVRABLE" else ""}",
            )
        }
        Tab.CODEX -> codexRows()
    }

    private fun codexRows(): List<RowItem> {
        val entries = engine.visibleCodexEntries(progression, snapshot).take(3).map { view ->
            RowItem(
                "entry:${view.definition.id.value}",
                view.definition.titleKey,
                "${view.definition.category.name} · ${if (view.discovered) "DÉCOUVERT" else "${view.current}/${view.definition.target}"}",
            )
        }
        val collections = engine.collectionViews(progression).take(2).map { view ->
            RowItem(
                "collection:${view.definition.id.value}",
                view.definition.titleKey,
                "COLLECTION · ${view.discoveredEntries}/${view.definition.entryIds.size}" +
                    if (view.claimable) " · RÉCOMPENSE" else "",
            )
        }
        return (collections + entries).take(4)
    }

    private fun actionAvailable(): Boolean {
        val item = rows().getOrNull(selected) ?: return false
        return when (tab) {
            Tab.OBJECTIVES -> engine.objectiveViews(progression, snapshot)
                .find { it.definition.id.value == item.id }
                ?.claimable == true
            Tab.CONTRACTS -> engine.activeContracts(progression, snapshot)
                .find { it.occurrenceId == item.id }
                ?.deliverable == true
            Tab.CODEX -> item.id.startsWith("collection:") && engine.collectionViews(progression)
                .find { it.definition.id.value == item.id.removePrefix("collection:") }
                ?.claimable == true
        }
    }

    private fun action() {
        val item = rows().getOrNull(selected) ?: return
        val result = when (tab) {
            Tab.OBJECTIVES -> engine.claimMission(progression, GameId.of(item.id), snapshot)
            Tab.CONTRACTS -> engine.deliverContract(progression, item.id, snapshot)
            Tab.CODEX -> if (item.id.startsWith("collection:")) {
                engine.claimCollection(progression, GameId.of(item.id.removePrefix("collection:")))
            } else {
                return
            }
        }
        applyTransaction(result)
    }

    private fun applyTransaction(result: ProgressionCommandResult) {
        when (result) {
            is ProgressionCommandResult.Rejected -> {
                message = when (result.code) {
                    "mission_incomplete" -> "Objectif incomplet"
                    "contract_locked" -> "Contrat pas encore disponible"
                    "contract_inventory_missing" -> "Stock insuffisant"
                    "collection_incomplete" -> "Collection incomplète"
                    else -> result.code
                }
                services.haptic.warning()
            }
            is ProgressionCommandResult.Applied -> {
                val inventory = main.economy.inventory.toMutableMap()
                for ((id, amount) in result.transaction.delta.inventoryDelta) {
                    val next = Math.addExact(inventory[id] ?: 0L, amount)
                    if (next < 0L) {
                        message = "Stock insuffisant"
                        services.haptic.warning()
                        return
                    }
                    inventory[id] = next
                }
                val dollars = Math.addExact(
                    main.economy.spaceDollars,
                    result.transaction.delta.spaceDollarsDelta,
                )
                if (dollars < 0L) {
                    message = "SpaceDollars insuffisants"
                    services.haptic.warning()
                    return
                }

                val nextMain = main.copy(
                    economy = main.economy.copy(
                        inventory = inventory,
                        spaceDollars = dollars,
                        transactionSequence = Math.addExact(main.economy.transactionSequence, 1L),
                    ),
                )
                val nextSnapshot = snapshot(nextMain)
                val nextProgression = engine.synchronize(result.state, nextSnapshot)
                val committed = transactions.commit(
                    main = nextMain,
                    progression = nextProgression,
                    mainContentVersion = economyDefinitions.contentVersion,
                    progressionContentVersion = definitions.contentVersion,
                    reason = result.transaction.reason,
                    savedAtEpochMillis = now(),
                )
                if (!committed.committed) {
                    persistenceBlocked = true
                    message = "Transaction en attente · relancez l'application"
                    services.haptic.warning()
                    return
                }

                main = nextMain
                progression = nextProgression
                snapshot = nextSnapshot
                selected = selected.coerceAtMost((rows().size - 1).coerceAtLeast(0))
                message = when (result.transaction.reason) {
                    "deliver_contract" -> "Contrat livré"
                    "claim_collection" -> "Collection complétée"
                    "claim_achievement" -> "Exploit validé"
                    else -> "Mission validée"
                }
                services.haptic.success()
            }
        }
    }

    private fun pin() {
        val item = rows().getOrNull(selected) ?: return
        if (tab != Tab.OBJECTIVES) return
        val previous = progression
        val next = engine.selectObjective(progression, GameId.of(item.id))
        if (saveProgression(next)) {
            progression = next
            message = "Objectif suivi"
            services.haptic.impact()
        } else {
            progression = previous
            message = "Objectif non sauvegardé"
            services.haptic.warning()
        }
    }

    private fun touch(point: Vector2) {
        if (persistenceBlocked) {
            message = "Relancez l'application pour terminer la transaction"
            services.haptic.warning()
            return
        }
        val current = currentLayout ?: return
        current.rows.forEachIndexed { index, row ->
            if (row.contains(point) && index < rows().size) {
                selected = index
                services.haptic.impact()
                return
            }
        }
        when {
            current.tab.contains(point) -> {
                tab = Tab.entries[(tab.ordinal + 1) % Tab.entries.size]
                selected = 0
                services.haptic.impact()
            }
            current.action.contains(point) && actionAvailable() -> action()
            current.pin.contains(point) -> pin()
            current.back.contains(point) -> onBack()
        }
    }

    private fun layout(): Layout {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1)
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 50f, right - left, 50f)
        val tutorial = Rectangle(left, topBar.y - 62f, right - left, 56f)
        val back = Rectangle(right - 92f, bottom, 92f, 48f)
        val pin = Rectangle(back.x - 92f, bottom, 86f, 48f)
        val action = Rectangle(pin.x - 98f, bottom, 92f, 48f)
        val tab = Rectangle(action.x - 94f, bottom, 88f, 48f)
        val message = Rectangle(left, bottom + 50f, right - left, 22f)
        val listBottom = message.y + message.height + 4f
        val listTop = tutorial.y - 6f
        val gap = 5f
        val rowHeight = ((listTop - listBottom - gap * 3f) / 4f).coerceAtLeast(38f)
        val rows = (0 until 4).map { index ->
            Rectangle(
                left,
                listTop - (index + 1) * rowHeight - index * gap,
                right - left,
                rowHeight,
            )
        }
        return Layout(topBar, tutorial, rows, message, tab, action, pin, back)
    }

    private fun button(rect: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) ACCENT else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun label(rect: Rectangle, text: String) {
        small.color = TEXT
        small.draw(batch, text, rect.x + 8f, rect.y + 29f)
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        small.dispose()
    }

    private enum class Tab(val label: String) {
        OBJECTIVES("OBJECTIFS"),
        CONTRACTS("CONTRATS"),
        CODEX("CODEX"),
    }

    private data class RowItem(
        val id: String,
        val title: String,
        val detail: String,
    )

    private data class Layout(
        val top: Rectangle,
        val tutorial: Rectangle,
        val rows: List<Rectangle>,
        val message: Rectangle,
        val tab: Rectangle,
        val action: Rectangle,
        val pin: Rectangle,
        val back: Rectangle,
    )

    private companion object {
        const val TAG = "MissionControlScreen"
        val BACKGROUND = Color(.008f, .014f, .03f, 1f)
        val TOP = Color(.025f, .055f, .10f, 1f)
        val TUTORIAL = Color(.08f, .12f, .18f, 1f)
        val PANEL = Color(.035f, .075f, .13f, 1f)
        val SELECTED = Color(.08f, .15f, .21f, 1f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val DISABLED = Color(.04f, .06f, .08f, 1f)
        val GRID = Color(.16f, .20f, .25f, 1f)
        val ACCENT = Color(.36f, .78f, .42f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
        val MUTED = Color(.63f, .72f, .82f, 1f)
    }
}
