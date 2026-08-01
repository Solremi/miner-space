package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.exploration.ExplorationActionResult
import fr.solremi.minerspace.data.exploration.ExplorationCoordinator
import fr.solremi.minerspace.domain.exploration.SectorDefinition
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class SectorExplorationScreen(private val services: GameServices) : KtxScreen {
    private val wc = OrthographicCamera()
    private val wv = ExtendViewport(640f, 320f, 960f, 540f, wc)
    private val hc = OrthographicCamera()
    private val hv = ExtendViewport(640f, 320f, 960f, 540f, hc)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.78f) }
    private val small = BitmapFont().apply { data.setScale(.62f) }
    private val coordinator = ExplorationCoordinator.fromServices(services)
    private val sectors = coordinator.definitions

    private var gameplay: ManufacturingPlanetScreen? = ManufacturingPlanetScreen(services)
    private var delegated: InputProcessor? = null
    private var productionVisible = false
    private var exploration = false
    private var session = coordinator.load()
    private val state get() = session.exploration
    private val main get() = session.manufacturing
    private var selected = state.activeMissionSectorId
        ?: sectors.sectors.values.first { it.initiallyUnlocked }.id
    private var message = "Scannez un secteur voisin"
    private var persistenceBlocked = false
    private var lastPinch = 0f
    private var centered = false
    private val opening = mutableMapOf<GameId, Long>()
    private val gesture = GestureDetector(Gestures())
    private var input = InputMultiplexer()
    private val lifecycle = LifecycleObserver {
        if (it == LifecycleState.BACKGROUND && !persistenceBlocked) {
            coordinator.saveExploration(session)
        }
    }

    override fun show() {
        services.lifecycle.addObserver(lifecycle)
        showProduction()
        installInput()
    }

    override fun hide() {
        if (!persistenceBlocked) coordinator.saveExploration(session)
        services.lifecycle.removeObserver(lifecycle)
        if (productionVisible) gameplay?.hide()
        productionVisible = false
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        gameplay?.resize(width, height)
        wv.update(width.coerceAtLeast(1), height.coerceAtLeast(1), false)
        hv.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
        if (!centered) {
            centerTarget(false)
            centered = true
        } else {
            clamp()
        }
    }

    override fun render(delta: Float) {
        if (!exploration) {
            gameplay?.render(delta)
            drawModeButton()
        } else {
            drawMap()
        }
    }

    private fun showProduction() {
        if (productionVisible) return
        val screen = gameplay ?: ManufacturingPlanetScreen(services).also { gameplay = it }
        screen.resize(Gdx.graphics.width, Gdx.graphics.height)
        screen.show()
        productionVisible = true
        delegated = Gdx.input.inputProcessor
    }

    private fun enterMap() {
        if (exploration) return
        if (productionVisible) gameplay?.hide()
        gameplay?.dispose()
        gameplay = null
        productionVisible = false
        delegated = null
        session = coordinator.load()
        persistenceBlocked = false
        exploration = true
        selected = state.activeMissionSectorId
            ?: sectors.sectors.values.first { it.initiallyUnlocked }.id
        centerTarget(false)
        installInput()
        services.haptic.impact()
    }

    private fun leaveMap() {
        if (!exploration) return
        if (persistenceBlocked) {
            message = "Transaction en attente · relancez l'application"
            services.haptic.warning()
            return
        }
        coordinator.saveExploration(session)
        exploration = false
        showProduction()
        installInput()
        services.haptic.impact()
    }

    private fun installInput() {
        input = InputMultiplexer().apply {
            addProcessor(gesture)
            if (!exploration) delegated?.let(::addProcessor)
        }
        Gdx.input.inputProcessor = input
    }

    private fun access() = coordinator.access(session)

    private fun drawModeButton() {
        hv.apply()
        hc.update()
        val rect = modeButton()
        shapes.projectionMatrix = hc.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = BUTTON
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = ACCENT
        shapes.rect(rect.x, rect.y, rect.width, 4f)
        shapes.end()
        batch.projectionMatrix = hc.combined
        batch.begin()
        small.color = TEXT
        small.draw(
            batch,
            "SECTEURS ${state.unlockedSectorIds.size}/${sectors.sectors.size}",
            rect.x + 8f,
            rect.y + 29f,
        )
        batch.end()
    }

    private fun drawMap() {
        ScreenUtils.clear(BG)
        wv.apply()
        wc.update()
        shapes.projectionMatrix = wc.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = GROUND
        shapes.rect(0f, 0f, MW, MH)
        sectors.sectors.values.forEach(::drawSector)
        shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = GRID
        sectors.sectors.values.forEach { sector ->
            with(sector.bounds) {
                shapes.rect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
            }
        }
        sectors.sectors[selected]?.let { sector ->
            shapes.color = SELECT
            with(sector.bounds) {
                shapes.rect(x - 4f, y - 4f, width + 8f, height + 8f)
            }
        }
        shapes.end()
        drawLabels()
        drawHud()
    }

    private fun drawSector(sector: SectorDefinition) {
        val bounds = sector.bounds
        val revealed = sector.id in state.revealedSectorIds
        val unlocked = sector.id in state.unlockedSectorIds
        shapes.color = when {
            unlocked -> sectorColor(sector.id)
            revealed -> SCANNED
            else -> FOG
        }
        shapes.rect(
            bounds.x.toFloat(),
            bounds.y.toFloat(),
            bounds.width.toFloat(),
            bounds.height.toFloat(),
        )
        if (!revealed) {
            shapes.color = FOG_LINE
            var x = bounds.x.toFloat()
            while (x < bounds.x + bounds.width) {
                shapes.rect(x, bounds.y.toFloat(), 12f, bounds.height.toFloat())
                x += 30f
            }
        }
        if (unlocked && sector.rareDepositId != null) {
            shapes.color = RARE
            shapes.circle(bounds.centerX.toFloat(), bounds.centerY.toFloat(), 23f, 20)
        }
        if (sector.missionTarget) {
            shapes.color = if (state.activeMissionSectorId == sector.id) MISSION_ACTIVE else MISSION
            shapes.circle(
                (bounds.x + bounds.width - 25).toFloat(),
                (bounds.y + bounds.height - 25).toFloat(),
                11f,
                16,
            )
        }
        val started = opening[sector.id] ?: return
        val progress = ((services.clock.monotonicMillis() - started) / OPEN_MS)
            .coerceIn(0f, 1f)
        if (progress >= 1f) {
            opening.remove(sector.id)
        } else {
            shapes.color = FOG
            val insetX = bounds.width * progress / 2f
            val insetY = bounds.height * progress / 2f
            shapes.rect(bounds.x.toFloat(), bounds.y.toFloat(), insetX, bounds.height.toFloat())
            shapes.rect(
                (bounds.x + bounds.width).toFloat() - insetX,
                bounds.y.toFloat(),
                insetX,
                bounds.height.toFloat(),
            )
            shapes.rect(
                bounds.x.toFloat() + insetX,
                bounds.y.toFloat(),
                bounds.width - insetX * 2f,
                insetY,
            )
            shapes.rect(
                bounds.x.toFloat() + insetX,
                (bounds.y + bounds.height).toFloat() - insetY,
                bounds.width - insetX * 2f,
                insetY,
            )
        }
    }

    private fun drawLabels() {
        batch.projectionMatrix = wc.combined
        batch.begin()
        sectors.sectors.values
            .filter { it.id in state.revealedSectorIds }
            .forEach { sector ->
                font.color = TEXT
                font.draw(
                    batch,
                    name(sector.id),
                    sector.bounds.x + 12f,
                    sector.bounds.y + sector.bounds.height - 14f,
                )
                small.color = MUTED
                small.draw(
                    batch,
                    if (sector.id in state.unlockedSectorIds) "OUVERT" else "SCANNÉ",
                    sector.bounds.x + 12f,
                    sector.bounds.y + 19f,
                )
            }
        batch.end()
    }

    private fun drawHud() {
        hv.apply()
        hc.update()
        val layout = layout()
        val sector = sectors.sectors[selected]
        shapes.projectionMatrix = hc.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HUD
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL
        shapes.rect(layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height)
        button(layout.scan, canScan())
        button(layout.open, canOpen())
        button(layout.mission, sector?.missionTarget == true)
        button(layout.back, !persistenceBlocked)
        shapes.end()

        batch.projectionMatrix = hc.combined
        batch.begin()
        font.color = TEXT
        small.color = MUTED
        font.draw(
            batch,
            "EXPLORATION · Scanner ${access().scannerLevel}",
            layout.top.x + 12f,
            layout.top.y + layout.top.height - 13f,
        )
        small.draw(
            batch,
            "${main.economy.spaceDollars} SD · ${state.unlockedSectorIds.size}/${sectors.sectors.size} secteurs",
            layout.top.x + 12f,
            layout.top.y + 14f,
        )
        font.draw(
            batch,
            sector?.let { name(it.id) } ?: "Aucun secteur",
            layout.panel.x + 12f,
            layout.panel.y + layout.panel.height - 13f,
        )
        small.draw(
            batch,
            if (persistenceBlocked) message else sector?.let(::details) ?: message,
            layout.panel.x + 12f,
            layout.panel.y + 13f,
        )
        label(layout.scan, "SCANNER")
        label(layout.open, "OUVRIR")
        label(layout.mission, "MISSION")
        label(layout.back, "PRODUCTION")
        batch.end()
    }

    private fun details(sector: SectorDefinition): String {
        if (sector.id !in state.revealedSectorIds) {
            return "Brouillard · scanner ${sector.scannerLevelRequired} requis"
        }
        if (sector.id in state.unlockedSectorIds) return sector.strategicReason
        val components = sector.requiredComponents.entries.joinToString(" · ") { (id, quantity) ->
            "$quantity ${short(id)}"
        }
        return "${sector.unlockCostSpaceDollars} SD · scan ${sector.scannerLevelRequired}" +
            if (components.isBlank()) "" else " · $components"
    }

    private fun button(rect: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) ACCENT else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun label(rect: Rectangle, text: String) {
        small.color = TEXT
        small.draw(batch, text, rect.x + 7f, rect.y + 29f)
    }

    private fun canScan(): Boolean =
        !persistenceBlocked &&
            sectors.sectors[selected]?.let { coordinator.availability(session, it.id).canScan } == true

    private fun canOpen(): Boolean =
        !persistenceBlocked &&
            sectors.sectors[selected]?.let { coordinator.availability(session, it.id).canUnlock } == true

    private fun scan() {
        handle(coordinator.scan(session, selected))
    }

    private fun open() {
        handle(coordinator.unlock(session, selected))
    }

    private fun handle(result: ExplorationActionResult) {
        when (result) {
            is ExplorationActionResult.Applied -> {
                session = result.session
                when (result.transaction.reason) {
                    "scan_sector" -> message = "Secteur révélé"
                    "unlock_sector" -> {
                        opening[result.transaction.sectorId] = services.clock.monotonicMillis()
                        val sector = sectors.sectors.getValue(result.transaction.sectorId)
                        message = "Secteur ouvert · ${sector.strategicReason}"
                    }
                    else -> message = result.transaction.reason
                }
                services.haptic.success()
            }

            is ExplorationActionResult.Rejected -> {
                message = reject(result.code)
                services.haptic.warning()
            }

            is ExplorationActionResult.PersistenceFailed -> {
                message = when (result.code) {
                    "exploration_save_failed" -> "Sauvegarde exploration impossible"
                    "transaction_prepare_failed" -> "Ouverture annulée avant modification"
                    else -> "Sauvegarde impossible"
                }
                services.haptic.warning()
            }

            is ExplorationActionResult.TransactionPending -> {
                persistenceBlocked = true
                val slot = result.transaction.failedSlotId?.let { " · $it" }.orEmpty()
                message = "Transaction en attente$slot · relancez l'application"
                services.haptic.warning()
            }
        }
    }

    private fun reject(code: String): String = when (code) {
        "scanner_level_low" -> "Scanner insuffisant"
        "sector_path_locked" -> "Ouvrez le secteur précédent"
        "technology_prerequisite_missing" -> "Technologie requise"
        "insufficient_space_dollars" -> "SpaceDollars insuffisants"
        "missing_sector_component" -> "Composants insuffisants"
        "sector_not_scanned" -> "Scannez d'abord ce secteur"
        else -> code
    }

    private fun centerTarget(feedback: Boolean = true) {
        val target = state.activeMissionSectorId
            ?: sectors.sectors.values.first { it.initiallyUnlocked }.id
        selected = target
        center(target)
        if (feedback) services.haptic.impact()
    }

    private fun center(id: GameId) {
        val bounds = sectors.sectors.getValue(id).bounds
        wc.zoom = .92f
        wc.position.set(bounds.centerX.toFloat(), bounds.centerY.toFloat(), 0f)
        clamp()
    }

    private fun select(x: Float, y: Float) {
        val point = Vector2(x, y)
        wv.unproject(point)
        sectors.sectors.values.firstOrNull { sector ->
            point.x in sector.bounds.x.toFloat()..(sector.bounds.x + sector.bounds.width).toFloat() &&
                point.y in sector.bounds.y.toFloat()..(sector.bounds.y + sector.bounds.height).toFloat()
        }?.let { sector ->
            if (selected != sector.id) services.haptic.impact()
            selected = sector.id
        }
    }

    private fun clamp() {
        val halfWidth = wc.viewportWidth * wc.zoom / 2f
        val halfHeight = wc.viewportHeight * wc.zoom / 2f
        wc.position.x = axis(wc.position.x, MW, halfWidth)
        wc.position.y = axis(wc.position.y, MH, halfHeight)
        wc.update()
    }

    private fun axis(value: Float, size: Float, half: Float): Float =
        if (half * 2 >= size) size / 2 else value.coerceIn(half, size - half)

    private fun hudPoint(x: Float, y: Float): Vector2 =
        Vector2(x, y).also(hv::unproject)

    private fun modeButton(): Rectangle {
        val (_, right, _, top) = safe()
        return Rectangle(right - 150f, top - 48f, 150f, 48f)
    }

    private fun layout(): Layout {
        val (left, right, bottom, top) = safe()
        val gap = 6f
        val back = Rectangle(right - 112f, bottom, 112f, 48f)
        val mission = Rectangle(back.x - gap - 90f, bottom, 90f, 48f)
        val open = Rectangle(mission.x - gap - 90f, bottom, 90f, 48f)
        val scan = Rectangle(open.x - gap - 90f, bottom, 90f, 48f)
        return Layout(
            top = Rectangle(left, top - 52f, right - left, 52f),
            panel = Rectangle(left, bottom, (scan.x - gap - left).coerceAtLeast(190f), 60f),
            scan = scan,
            open = open,
            mission = mission,
            back = back,
        )
    }

    private fun safe(): List<Float> {
        val width = hv.worldWidth
        val height = hv.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1)
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        return listOf(left, right, bottom, top)
    }

    private fun name(id: GameId): String = when (id) {
        CORE -> "Noyau Delta"
        COPPER -> "Crête cuivrée"
        CRYSTAL -> "Plaines cristallines"
        LOGISTICS -> "Passe logistique"
        XENON -> "Profondeurs xénon"
        else -> "Ruines d'archive"
    }

    private fun short(id: GameId): String =
        if (id == POWER) "pile(s)" else "capteur(s)"

    private fun sectorColor(id: GameId): Color = when (id) {
        CORE -> CORE_C
        COPPER -> COPPER_C
        CRYSTAL -> CRYSTAL_C
        LOGISTICS -> LOG_C
        XENON -> XENON_C
        else -> ARCHIVE_C
    }

    override fun dispose() {
        hide()
        gameplay?.dispose()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        small.dispose()
    }

    private inner class Gestures : GestureDetector.GestureAdapter() {
        private var onHud = false

        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            lastPinch = 0f
            val point = hudPoint(x, y)
            onHud = if (!exploration) {
                modeButton().contains(point)
            } else {
                layout().let {
                    it.top.contains(point) ||
                        it.panel.contains(point) ||
                        it.scan.contains(point) ||
                        it.open.contains(point) ||
                        it.mission.contains(point) ||
                        it.back.contains(point)
                }
            }
            return onHud || exploration
        }

        override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
            val point = hudPoint(x, y)
            if (!exploration) {
                if (modeButton().contains(point)) enterMap()
                return modeButton().contains(point)
            }
            val layout = layout()
            when {
                layout.back.contains(point) && !persistenceBlocked -> leaveMap()
                layout.scan.contains(point) && canScan() -> scan()
                layout.open.contains(point) && canOpen() -> open()
                layout.mission.contains(point) -> centerTarget()
                layout.top.contains(point) || layout.panel.contains(point) -> Unit
                else -> {
                    select(x, y)
                    if (count >= 2) center(selected)
                }
            }
            return true
        }

        override fun pan(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            if (!exploration || onHud) return false
            wc.position.x -= dx * wc.viewportWidth * wc.zoom / wv.screenWidth.coerceAtLeast(1)
            wc.position.y += dy * wc.viewportHeight * wc.zoom / wv.screenHeight.coerceAtLeast(1)
            clamp()
            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (!exploration || onHud || distance <= MathUtils.FLOAT_ROUNDING_ERROR) return false
            val base = if (lastPinch <= 0f) initialDistance else lastPinch
            wc.zoom = (wc.zoom * base / distance).coerceIn(.58f, 1.55f)
            lastPinch = distance
            clamp()
            return true
        }

        override fun pinchStop() {
            lastPinch = 0f
        }
    }

    private data class Layout(
        val top: Rectangle,
        val panel: Rectangle,
        val scan: Rectangle,
        val open: Rectangle,
        val mission: Rectangle,
        val back: Rectangle,
    )

    private companion object {
        const val MW = 1600f
        const val MH = 900f
        const val OPEN_MS = 900f

        val POWER = GameId.of("component_power_cell")
        val CORE = GameId.of("sector_core_delta")
        val COPPER = GameId.of("sector_copper_ridge")
        val CRYSTAL = GameId.of("sector_crystal_flats")
        val LOGISTICS = GameId.of("sector_logistics_pass")
        val XENON = GameId.of("sector_xenon_depths")

        val BG = Color(.008f, .014f, .035f, 1f)
        val GROUND = Color(.055f, .065f, .085f, 1f)
        val GRID = Color(.16f, .20f, .25f, 1f)
        val FOG = Color(.025f, .035f, .055f, 1f)
        val FOG_LINE = Color(.04f, .055f, .08f, 1f)
        val SCANNED = Color(.10f, .13f, .17f, 1f)
        val CORE_C = Color(.18f, .25f, .31f, 1f)
        val COPPER_C = Color(.30f, .17f, .11f, 1f)
        val CRYSTAL_C = Color(.17f, .20f, .36f, 1f)
        val LOG_C = Color(.18f, .29f, .24f, 1f)
        val XENON_C = Color(.24f, .14f, .34f, 1f)
        val ARCHIVE_C = Color(.31f, .24f, .13f, 1f)
        val RARE = Color(.70f, .45f, .95f, 1f)
        val MISSION = Color(.65f, .48f, .20f, 1f)
        val MISSION_ACTIVE = Color(1f, .76f, .22f, 1f)
        val ACCENT = Color(.20f, .82f, .88f, 1f)
        val SELECT = Color(.96f, .78f, .24f, 1f)
        val HUD = Color(.025f, .055f, .10f, 1f)
        val PANEL = Color(.035f, .075f, .13f, 1f)
        val BUTTON = Color(.08f, .18f, .26f, 1f)
        val DISABLED = Color(.05f, .08f, .11f, 1f)
        val TEXT = Color(.90f, .96f, 1f, 1f)
        val MUTED = Color(.61f, .72f, .82f, 1f)
    }
}
