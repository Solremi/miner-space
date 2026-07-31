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
import fr.solremi.minerspace.data.narrative.NarrativeContentLoader
import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.NarrativeStateCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.data.save.SectorProgressCodec
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.exploration.ExplorationState
import fr.solremi.minerspace.domain.narrative.*
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.robot.RobotAutomationEngine
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.services.*
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class NarrativeArchiveScreen(
    private val services: GameServices,
    private val onBack: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.76f) }
    private val small = BitmapFont().apply { data.setScale(.58f) }
    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val narrativeDefinitions = NarrativeContentLoader().load(services.content)
    private val narrativeEngine = NarrativeEngine(narrativeDefinitions)
    private val robotDefinitions = RobotContentLoader().load(services.content)
    private val robotEngine = RobotAutomationEngine(robotDefinitions)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val sectorCodec = SectorProgressCodec()
    private val robotCodec = RobotFleetCodec()
    private val narrativeCodec = NarrativeStateCodec()
    private var main = initialMain()
    private var exploration: ExplorationState? = null
    private var robots = robotEngine.initialState(now())
    private var narrative = narrativeEngine.initialState()
    private var mode = Mode.SIGNALS
    private var selected = 0
    private var message = "NOVA reste facultative : la production continue sans ouvrir cet écran."
    private var currentLayout: Layout? = null
    private val lifecycle = LifecycleObserver { if (it == LifecycleState.BACKGROUND) saveNarrative() }
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() {
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
        reload()
        reconcilePending()
    }

    override fun hide() {
        saveNarrative()
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply(); camera.update()
        val layout = layout(); currentLayout = layout
        drawPanels(layout); drawText(layout)
    }

    private fun initialMain() = ManufacturingGameState(economy.initialState(), RefiningState.empty(), AssemblyState.empty())

    private fun reload() {
        main = services.save.loadLatest()?.let { runCatching { mainCodec.decode(it) }.getOrNull() } ?: initialMain()
        exploration = services.save.loadLatest(SectorProgressCodec.SLOT_ID)?.let { runCatching { sectorCodec.decode(it) }.getOrNull() }
        robots = services.save.loadLatest(RobotFleetCodec.SLOT_ID)?.let { runCatching { robotCodec.decode(it) }.getOrNull() }
            ?: robotEngine.initialState(now())
        narrative = services.save.loadLatest(NarrativeStateCodec.SLOT_ID)?.let { payload ->
            runCatching { require(payload.contentVersion == narrativeDefinitions.contentVersion); narrativeEngine.normalize(narrativeCodec.decode(payload)) }.getOrNull()
        } ?: narrativeEngine.initialState()
        selected = 0
    }

    private fun snapshot() = NarrativeSnapshot(
        unlockedSectorCount = exploration?.unlockedSectorIds?.size ?: 1,
        installedTechnologyCount = main.assembly.installedTechnologyIds.size,
        inventory = main.economy.inventory,
        robotMasteryById = robots.robots.mapValues { it.value.masteryPoints },
    )

    private fun rows(): List<Row> = when (mode) {
        Mode.SIGNALS -> narrativeEngine.chapterViews(narrative, snapshot())
            .filter { it.available }
            .take(4)
            .map { view ->
                val status = when {
                    view.resolved -> "RÉSOLU"
                    !view.read -> "NOUVEAU"
                    else -> "ANALYSE ${view.attempts}/${view.definition.pityAttempts}"
                }
                Row(view.definition.id, view.definition.title, status, view.definition.transmission)
            }
        Mode.ARCHIVES -> narrativeEngine.visibleArchives(narrative).take(4).map { chapter ->
            val resolved = chapter.id in narrative.resolvedChapterIds
            Row(chapter.id, chapter.title, if (resolved) "ARCHIVÉ · RÉSOLU" else "ARCHIVÉ", if (resolved) chapter.archiveSummary else chapter.transmission)
        }
    }

    private fun drawPanels(layout: Layout) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP; shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = NOVA; shapes.rect(layout.nova.x, layout.nova.y, layout.nova.width, layout.nova.height)
        layout.rows.forEachIndexed { index, row ->
            shapes.color = if (index == selected) SELECTED else PANEL; shapes.rect(row.x, row.y, row.width, row.height)
            shapes.color = if (index == selected) ACCENT else GRID; shapes.rect(row.x, row.y, 4f, row.height)
        }
        button(layout.mode, true); button(layout.read, canRead()); button(layout.analyze, canAnalyze()); button(layout.back, true)
        shapes.end()
    }

    private fun drawText(layout: Layout) {
        batch.projectionMatrix = camera.combined; batch.begin()
        font.color = TEXT; small.color = MUTED
        font.draw(batch, "NOVA · ${mode.label}", layout.top.x + 12f, layout.top.y + layout.top.height - 14f)
        small.draw(batch, "${narrative.resolvedChapterIds.size}/${narrativeDefinitions.chapters.size} archives résolues · narration ignorable", layout.top.x + 12f, layout.top.y + 15f)
        font.draw(batch, "TRANSMISSION COURTE", layout.nova.x + 12f, layout.nova.y + layout.nova.height - 13f)
        val body = rows().getOrNull(selected)?.body ?: "Aucun signal disponible pour la progression actuelle."
        small.draw(batch, wrap(body, 92), layout.nova.x + 12f, layout.nova.y + 15f)
        rows().forEachIndexed { index, item ->
            val rect = layout.rows[index]
            font.draw(batch, item.title, rect.x + 14f, rect.y + rect.height - 13f)
            small.draw(batch, item.status, rect.x + 14f, rect.y + 14f)
        }
        small.draw(batch, message, layout.message.x, layout.message.y + 15f)
        label(layout.mode, "ONGLET"); label(layout.read, "LIRE"); label(layout.analyze, "ANALYSER"); label(layout.back, "RETOUR")
        batch.end()
    }

    private fun canRead(): Boolean {
        if (mode != Mode.SIGNALS) return false
        val id = rows().getOrNull(selected)?.id ?: return false
        return id !in narrative.readTransmissionIds
    }

    private fun canAnalyze(): Boolean {
        if (mode != Mode.SIGNALS || narrative.pendingGrant != null) return false
        val id = rows().getOrNull(selected)?.id ?: return false
        return id in narrative.readTransmissionIds && id !in narrative.resolvedChapterIds
    }

    private fun readTransmission() {
        val id = rows().getOrNull(selected)?.id ?: return
        when (val result = narrativeEngine.readTransmission(narrative, id, snapshot())) {
            is NarrativeCommandResult.Rejected -> { message = reject(result.code); services.haptic.warning() }
            is NarrativeCommandResult.Applied -> {
                if (saveNarrative(result.state)) {
                    narrative = result.state; message = "Transmission ajoutée aux archives"; services.haptic.success()
                } else message = "Sauvegarde différée"
            }
        }
    }

    private fun investigate() {
        val id = rows().getOrNull(selected)?.id ?: return
        when (val result = narrativeEngine.investigate(narrative, id, snapshot())) {
            is NarrativeCommandResult.Rejected -> { message = reject(result.code); services.haptic.warning() }
            is NarrativeCommandResult.Applied -> {
                if (!saveNarrative(result.state)) { message = "Analyse non enregistrée"; services.haptic.warning(); return }
                narrative = result.state
                if (narrative.pendingGrant == null) {
                    message = "Signal incomplet · nouvelle analyse disponible"
                    services.haptic.impact()
                } else reconcilePending()
            }
        }
    }

    private fun reconcilePending() {
        val grant = narrative.pendingGrant ?: return
        var currentSnapshot = snapshot()
        val missingRare = narrativeEngine.missingRareQuantity(grant, currentSnapshot)
        if (missingRare > 0L) {
            val resourceId = grant.rareResourceId ?: return
            val inventory = main.economy.inventory.toMutableMap()
            inventory[resourceId] = Math.addExact(inventory[resourceId] ?: 0L, missingRare)
            val nextMain = main.copy(economy = main.economy.copy(
                inventory = inventory,
                transactionSequence = Math.addExact(main.economy.transactionSequence, 1L),
            ))
            if (!saveMain(nextMain)) { message = "Découverte en attente de sauvegarde"; return }
            main = nextMain; currentSnapshot = snapshot()
        }

        val veteranId = grant.veteranRobotId
        if (veteranId != null && narrativeEngine.requiredVeteranMastery(grant, currentSnapshot) > 0L) {
            val robot = robots.robots[veteranId] ?: run { message = "Robot vétéran introuvable"; return }
            val nextRobots = robots.copy(
                robots = robots.robots + (veteranId to robot.copy(masteryPoints = maxOf(robot.masteryPoints, grant.expectedVeteranMastery))),
                transactionSequence = Math.addExact(robots.transactionSequence, 1L),
            )
            if (!saveRobots(nextRobots)) { message = "Protocole vétéran en attente"; return }
            robots = nextRobots
        }

        val finalized = narrativeEngine.finalizePending(narrative) as NarrativeCommandResult.Applied
        if (!saveNarrative(finalized.state)) { message = "Clôture d’archive en attente"; return }
        narrative = finalized.state
        message = if (grant.veteranRobotId != null) "Ressource sécurisée · Aster devient vétéran" else "Découverte rare sauvegardée immédiatement"
        services.haptic.success()
    }

    private fun saveMain(value: ManufacturingGameState) = services.save.save(
        mainCodec.encode(value, economyDefinitions.contentVersion, savedAtEpochMillis = now()),
    ) == SaveWriteStatus.WRITTEN

    private fun saveRobots(value: RobotAutomationState) = services.save.save(
        robotCodec.encode(value, robotDefinitions.contentVersion, now()),
    ) == SaveWriteStatus.WRITTEN

    private fun saveNarrative(value: NarrativeState = narrative) = services.save.save(
        narrativeCodec.encode(value, narrativeDefinitions.contentVersion, now()),
    ) == SaveWriteStatus.WRITTEN

    private fun now() = services.clock.nowEpochMillis().coerceAtLeast(0L)

    private fun touch(point: Vector2) {
        val layout = currentLayout ?: return
        layout.rows.forEachIndexed { index, rect -> if (rect.contains(point) && index < rows().size) { selected = index; services.haptic.impact(); return } }
        when {
            layout.mode.contains(point) -> { mode = Mode.entries[(mode.ordinal + 1) % Mode.entries.size]; selected = 0; services.haptic.impact() }
            layout.read.contains(point) && canRead() -> readTransmission()
            layout.analyze.contains(point) && canAnalyze() -> investigate()
            layout.back.contains(point) -> onBack()
        }
    }

    private fun layout(): Layout {
        val width = viewport.worldWidth; val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1); val scaleY = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f; val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f; val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 50f, right - left, 50f)
        val nova = Rectangle(left, topBar.y - 70f, right - left, 64f)
        val back = Rectangle(right - 92f, bottom, 92f, 48f)
        val analyze = Rectangle(back.x - 6f - 102f, bottom, 102f, 48f)
        val read = Rectangle(analyze.x - 6f - 76f, bottom, 76f, 48f)
        val mode = Rectangle(read.x - 6f - 84f, bottom, 84f, 48f)
        val message = Rectangle(left, bottom + 50f, right - left, 22f)
        val listBottom = message.y + message.height + 4f; val listTop = nova.y - 6f
        val gap = 5f; val rowHeight = ((listTop - listBottom - gap * 3f) / 4f).coerceAtLeast(37f)
        val rows = (0 until 4).map { index -> Rectangle(left, listTop - (index + 1) * rowHeight - index * gap, right - left, rowHeight) }
        return Layout(topBar, nova, rows, message, mode, read, analyze, back)
    }

    private fun button(rect: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else DISABLED; shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) ACCENT else GRID; shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun label(rect: Rectangle, value: String) { small.color = TEXT; small.draw(batch, value, rect.x + 7f, rect.y + 29f) }
    private fun reject(code: String) = when (code) {
        "chapter_unavailable" -> "Signal pas encore accessible"
        "transmission_unread" -> "Lisez d’abord la transmission"
        "chapter_already_resolved" -> "Archive déjà résolue"
        "grant_pending" -> "Attribution précédente en cours"
        else -> code
    }
    private fun wrap(value: String, max: Int): String = value.chunked(max).joinToString("\n")

    override fun dispose() { hide(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }

    private data class Row(val id: GameId, val title: String, val status: String, val body: String)
    private data class Layout(val top: Rectangle, val nova: Rectangle, val rows: List<Rectangle>, val message: Rectangle, val mode: Rectangle, val read: Rectangle, val analyze: Rectangle, val back: Rectangle)
    private enum class Mode(val label: String) { SIGNALS("SIGNAUX"), ARCHIVES("ARCHIVES") }

    private companion object {
        val BACKGROUND = Color(.008f, .012f, .028f, 1f)
        val TOP = Color(.025f, .055f, .10f, 1f)
        val NOVA = Color(.10f, .08f, .18f, 1f)
        val PANEL = Color(.035f, .07f, .12f, 1f)
        val SELECTED = Color(.09f, .14f, .22f, 1f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val DISABLED = Color(.04f, .06f, .09f, 1f)
        val ACCENT = Color(.72f, .46f, .96f, 1f)
        val GRID = Color(.14f, .18f, .24f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
        val MUTED = Color(.63f, .72f, .82f, 1f)
    }
}
