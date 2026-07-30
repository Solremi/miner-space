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
import fr.solremi.minerspace.data.assembly.AssemblyContentLoader
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.exploration.SectorContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.SectorProgressCodec
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.exploration.ExplorationAccess
import fr.solremi.minerspace.domain.exploration.ExplorationCommandResult
import fr.solremi.minerspace.domain.exploration.ExplorationEngine
import fr.solremi.minerspace.domain.exploration.ExplorationState
import fr.solremi.minerspace.domain.exploration.SectorDefinition
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
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
    private val economyDef = CoreEconomyContentLoader().load(services.content)
    private val assemblyDef = AssemblyContentLoader().load(services.content)
    private val sectors = SectorContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDef)
    private val engine = ExplorationEngine(sectors)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val sectorCodec = SectorProgressCodec()
    private var gameplay: ManufacturingPlanetScreen? = ManufacturingPlanetScreen(services)
    private var delegated: InputProcessor? = null
    private var productionVisible = false
    private var exploration = false
    private var main = loadMain()
    private var state = loadSectors()
    private var selected = state.activeMissionSectorId ?: sectors.sectors.values.first { it.initiallyUnlocked }.id
    private var message = "Scannez un secteur voisin"
    private var lastPinch = 0f
    private var centered = false
    private val opening = mutableMapOf<GameId, Long>()
    private val gesture = GestureDetector(Gestures())
    private var input = InputMultiplexer()
    private val lifecycle = LifecycleObserver { if (it == LifecycleState.BACKGROUND) saveSectors() }

    override fun show() {
        services.lifecycle.addObserver(lifecycle)
        showProduction()
        installInput()
    }

    override fun hide() {
        saveSectors()
        services.lifecycle.removeObserver(lifecycle)
        if (productionVisible) gameplay?.hide()
        productionVisible = false
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        gameplay?.resize(width, height)
        wv.update(width.coerceAtLeast(1), height.coerceAtLeast(1), false)
        hv.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
        if (!centered) { centerTarget(false); centered = true } else clamp()
    }

    override fun render(delta: Float) {
        if (!exploration) {
            gameplay?.render(delta)
            drawModeButton()
        } else drawMap()
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
        gameplay?.dispose(); gameplay = null; productionVisible = false; delegated = null
        main = loadMain(); state = engine.normalize(state); exploration = true
        centerTarget(false); installInput(); services.haptic.impact()
    }

    private fun leaveMap() {
        if (!exploration) return
        saveSectors(); exploration = false; showProduction(); installInput(); services.haptic.impact()
    }

    private fun installInput() {
        input = InputMultiplexer().apply {
            addProcessor(gesture)
            if (!exploration) delegated?.let(::addProcessor)
        }
        Gdx.input.inputProcessor = input
    }

    private fun initialMain() = ManufacturingGameState(economy.initialState(), RefiningState.empty(), AssemblyState.empty())
    private fun loadMain(): ManufacturingGameState {
        val payload = services.save.loadLatest() ?: return initialMain()
        return runCatching { require(payload.contentVersion == economyDef.contentVersion); mainCodec.decode(payload) }
            .getOrElse { initialMain() }
    }
    private fun loadSectors(): ExplorationState {
        val payload = services.save.loadLatest(SectorProgressCodec.SLOT_ID) ?: return engine.initialState()
        return runCatching { require(payload.contentVersion == sectors.contentVersion); engine.normalize(sectorCodec.decode(payload)) }
            .getOrElse { engine.initialState() }
    }
    private fun saveSectors() = services.save.save(
        sectorCodec.encode(state, sectors.contentVersion, services.clock.nowEpochMillis().coerceAtLeast(0L)),
    ) == SaveWriteStatus.WRITTEN
    private fun saveMain(value: ManufacturingGameState) = services.save.save(
        mainCodec.encode(value, economyDef.contentVersion, savedAtEpochMillis = services.clock.nowEpochMillis().coerceAtLeast(0L)),
    ) == SaveWriteStatus.WRITTEN

    private fun access(): ExplorationAccess {
        val tech = main.assembly.installedTechnologyIds
        val scanner = 1 + (if (TECH1 in tech) 1 else 0) + (if (TECH2 in tech) 1 else 0)
        return ExplorationAccess(scanner, main.economy.spaceDollars, main.economy.inventory, tech)
    }

    private fun drawModeButton() {
        hv.apply(); hc.update(); val r = modeButton()
        shapes.projectionMatrix = hc.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = BUTTON; shapes.rect(r.x, r.y, r.width, r.height)
        shapes.color = ACCENT; shapes.rect(r.x, r.y, r.width, 4f); shapes.end()
        batch.projectionMatrix = hc.combined; batch.begin(); small.color = TEXT
        small.draw(batch, "SECTEURS ${state.unlockedSectorIds.size}/${sectors.sectors.size}", r.x + 8f, r.y + 29f); batch.end()
    }

    private fun drawMap() {
        ScreenUtils.clear(BG); wv.apply(); wc.update(); shapes.projectionMatrix = wc.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled); shapes.color = GROUND; shapes.rect(0f, 0f, MW, MH)
        sectors.sectors.values.forEach(::drawSector); shapes.end()
        shapes.begin(ShapeRenderer.ShapeType.Line); shapes.color = GRID
        sectors.sectors.values.forEach { s -> with(s.bounds) { shapes.rect(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()) } }
        sectors.sectors[selected]?.let { s -> shapes.color = SELECT; with(s.bounds) { shapes.rect(x - 4f, y - 4f, width + 8f, height + 8f) } }
        shapes.end(); drawLabels(); drawHud()
    }

    private fun drawSector(s: SectorDefinition) {
        val b = s.bounds; val revealed = s.id in state.revealedSectorIds; val unlocked = s.id in state.unlockedSectorIds
        shapes.color = when { unlocked -> sectorColor(s.id); revealed -> SCANNED; else -> FOG }
        shapes.rect(b.x.toFloat(), b.y.toFloat(), b.width.toFloat(), b.height.toFloat())
        if (!revealed) {
            shapes.color = FOG_LINE
            var x = b.x.toFloat(); while (x < b.x + b.width) { shapes.rect(x, b.y.toFloat(), 12f, b.height.toFloat()); x += 30f }
        }
        if (unlocked && s.rareDepositId != null) {
            shapes.color = RARE; shapes.circle(b.centerX.toFloat(), b.centerY.toFloat(), 23f, 20)
        }
        if (s.missionTarget) {
            shapes.color = if (state.activeMissionSectorId == s.id) MISSION_ACTIVE else MISSION
            shapes.circle((b.x + b.width - 25).toFloat(), (b.y + b.height - 25).toFloat(), 11f, 16)
        }
        val started = opening[s.id] ?: return
        val progress = ((services.clock.monotonicMillis() - started) / OPEN_MS).coerceIn(0f, 1f)
        if (progress >= 1f) opening.remove(s.id) else {
            shapes.color = FOG
            val insetX = b.width * progress / 2f; val insetY = b.height * progress / 2f
            shapes.rect(b.x.toFloat(), b.y.toFloat(), insetX, b.height.toFloat())
            shapes.rect((b.x + b.width).toFloat() - insetX, b.y.toFloat(), insetX, b.height.toFloat())
            shapes.rect(b.x.toFloat() + insetX, b.y.toFloat(), (b.width - insetX * 2f), insetY)
            shapes.rect(b.x.toFloat() + insetX, (b.y + b.height).toFloat() - insetY, (b.width - insetX * 2f), insetY)
        }
    }

    private fun drawLabels() {
        batch.projectionMatrix = wc.combined; batch.begin()
        sectors.sectors.values.filter { it.id in state.revealedSectorIds }.forEach { s ->
            font.color = TEXT; font.draw(batch, name(s.id), s.bounds.x + 12f, s.bounds.y + s.bounds.height - 14f)
            small.color = MUTED; small.draw(batch, if (s.id in state.unlockedSectorIds) "OUVERT" else "SCANNÉ", s.bounds.x + 12f, s.bounds.y + 19f)
        }
        batch.end()
    }

    private fun drawHud() {
        hv.apply(); hc.update(); val l = layout(); val sector = sectors.sectors[selected]
        shapes.projectionMatrix = hc.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HUD; shapes.rect(l.top.x, l.top.y, l.top.width, l.top.height)
        shapes.color = PANEL; shapes.rect(l.panel.x, l.panel.y, l.panel.width, l.panel.height)
        button(l.scan, canScan()); button(l.open, canOpen()); button(l.mission, sector?.missionTarget == true); button(l.back, true); shapes.end()
        batch.projectionMatrix = hc.combined; batch.begin(); font.color = TEXT; small.color = MUTED
        font.draw(batch, "EXPLORATION · Scanner ${access().scannerLevel}", l.top.x + 12f, l.top.y + l.top.height - 13f)
        small.draw(batch, "${main.economy.spaceDollars} SD · ${state.unlockedSectorIds.size}/${sectors.sectors.size} secteurs", l.top.x + 12f, l.top.y + 14f)
        font.draw(batch, sector?.let { name(it.id) } ?: "Aucun secteur", l.panel.x + 12f, l.panel.y + l.panel.height - 13f)
        small.draw(batch, sector?.let(::details) ?: message, l.panel.x + 12f, l.panel.y + 13f)
        label(l.scan, "SCANNER"); label(l.open, "OUVRIR"); label(l.mission, "MISSION"); label(l.back, "PRODUCTION"); batch.end()
    }

    private fun details(s: SectorDefinition): String {
        if (s.id !in state.revealedSectorIds) return "Brouillard · scanner ${s.scannerLevelRequired} requis"
        if (s.id in state.unlockedSectorIds) return s.strategicReason
        val components = s.requiredComponents.entries.joinToString(" · ") { "${it.value} ${short(it.key)}" }
        return "${s.unlockCostSpaceDollars} SD · scan ${s.scannerLevelRequired}" + if (components.isBlank()) "" else " · $components"
    }

    private fun button(r: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else DISABLED; shapes.rect(r.x, r.y, r.width, r.height)
        shapes.color = if (enabled) ACCENT else GRID; shapes.rect(r.x, r.y, r.width, 4f)
    }
    private fun label(r: Rectangle, text: String) { small.color = TEXT; small.draw(batch, text, r.x + 7f, r.y + 29f) }
    private fun canScan() = sectors.sectors[selected]?.let { engine.availability(state, it.id, access()).canScan } == true
    private fun canOpen() = sectors.sectors[selected]?.let { engine.availability(state, it.id, access()).canUnlock } == true

    private fun scan() = apply(engine.scan(state, selected, access()))
    private fun open() {
        val result = engine.unlock(state, selected, access())
        if (result !is ExplorationCommandResult.Applied) { apply(result); return }
        val sector = sectors.sectors.getValue(selected); val oldMain = main
        val inv = oldMain.economy.inventory.toMutableMap()
        sector.requiredComponents.forEach { (id, q) -> inv[id] = (inv[id] ?: 0L) - q }
        result.transaction.rareDepositId?.let { rareResource(it) }?.let { id -> inv[id] = Math.addExact(inv[id] ?: 0L, 1L) }
        val nextMain = oldMain.copy(economy = oldMain.economy.copy(
            inventory = inv,
            spaceDollars = oldMain.economy.spaceDollars - sector.unlockCostSpaceDollars,
            transactionSequence = Math.addExact(oldMain.economy.transactionSequence, 1L),
        ))
        if (!saveMain(nextMain)) { message = "Sauvegarde du coût impossible"; services.haptic.warning(); return }
        val oldState = state; main = nextMain; state = result.state
        if (!saveSectors()) { saveMain(oldMain); main = oldMain; state = oldState; message = "Ouverture annulée"; services.haptic.warning(); return }
        opening[selected] = services.clock.monotonicMillis(); message = "Secteur ouvert · ${sector.strategicReason}"
        services.haptic.success()
    }

    private fun apply(result: ExplorationCommandResult) {
        when (result) {
            is ExplorationCommandResult.Applied -> { state = result.state; saveSectors(); message = if (result.transaction.reason == "scan_sector") "Secteur révélé" else "Mission ciblée"; services.haptic.success() }
            is ExplorationCommandResult.Rejected -> { message = reject(result.code); services.haptic.warning() }
        }
    }
    private fun reject(code: String) = when (code) {
        "scanner_level_low" -> "Scanner insuffisant"
        "sector_path_locked" -> "Ouvrez le secteur précédent"
        "technology_prerequisite_missing" -> "Technologie requise"
        "insufficient_space_dollars" -> "SpaceDollars insuffisants"
        "missing_sector_component" -> "Composants insuffisants"
        "sector_not_scanned" -> "Scannez d'abord ce secteur"
        else -> code
    }

    private fun centerTarget(feedback: Boolean = true) {
        val target = state.activeMissionSectorId ?: sectors.sectors.values.first { it.initiallyUnlocked }.id
        selected = target; center(target); if (feedback) services.haptic.impact()
    }
    private fun center(id: GameId) { val b = sectors.sectors.getValue(id).bounds; wc.zoom = .92f; wc.position.set(b.centerX.toFloat(), b.centerY.toFloat(), 0f); clamp() }
    private fun select(x: Float, y: Float) {
        val p = Vector2(x, y); wv.unproject(p)
        sectors.sectors.values.firstOrNull { p.x in it.bounds.x.toFloat()..(it.bounds.x + it.bounds.width).toFloat() && p.y in it.bounds.y.toFloat()..(it.bounds.y + it.bounds.height).toFloat() }?.let {
            if (selected != it.id) services.haptic.impact(); selected = it.id
        }
    }
    private fun clamp() {
        val hw = wc.viewportWidth * wc.zoom / 2f; val hh = wc.viewportHeight * wc.zoom / 2f
        wc.position.x = axis(wc.position.x, MW, hw); wc.position.y = axis(wc.position.y, MH, hh); wc.update()
    }
    private fun axis(v: Float, size: Float, half: Float) = if (half * 2 >= size) size / 2 else v.coerceIn(half, size - half)
    private fun hudPoint(x: Float, y: Float) = Vector2(x, y).also(hv::unproject)

    private fun modeButton(): Rectangle { val (_, r, _, t) = safe(); return Rectangle(r - 150f, t - 48f, 150f, 48f) }
    private fun layout(): Layout {
        val (left, right, bottom, top) = safe(); val gap = 6f
        val back = Rectangle(right - 112f, bottom, 112f, 48f)
        val mission = Rectangle(back.x - gap - 90f, bottom, 90f, 48f)
        val open = Rectangle(mission.x - gap - 90f, bottom, 90f, 48f)
        val scan = Rectangle(open.x - gap - 90f, bottom, 90f, 48f)
        return Layout(Rectangle(left, top - 52f, right - left, 52f), Rectangle(left, bottom, (scan.x - gap - left).coerceAtLeast(190f), 60f), scan, open, mission, back)
    }
    private fun safe(): List<Float> {
        val w = hv.worldWidth; val h = hv.worldHeight; val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val l = Gdx.graphics.safeInsetLeft * sx + 8f; val r = max(l + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val b = Gdx.graphics.safeInsetBottom * sy + 8f; val t = max(b + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f); return listOf(l, r, b, t)
    }

    private fun name(id: GameId) = when (id) { CORE -> "Noyau Delta"; COPPER -> "Crête cuivrée"; CRYSTAL -> "Plaines cristallines"; LOGISTICS -> "Passe logistique"; XENON -> "Profondeurs xénon"; else -> "Ruines d'archive" }
    private fun short(id: GameId) = if (id == POWER) "pile(s)" else "capteur(s)"
    private fun rareResource(id: GameId) = when (id) { RARE_P -> PRISMATIC; RARE_X -> XENON_R; RARE_A -> ARCHIVE_R; else -> null }
    private fun sectorColor(id: GameId) = when (id) { CORE -> CORE_C; COPPER -> COPPER_C; CRYSTAL -> CRYSTAL_C; LOGISTICS -> LOG_C; XENON -> XENON_C; else -> ARCHIVE_C }

    override fun dispose() { hide(); gameplay?.dispose(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }

    private inner class Gestures : GestureDetector.GestureAdapter() {
        private var onHud = false
        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            lastPinch = 0f; val p = hudPoint(x, y)
            onHud = if (!exploration) modeButton().contains(p) else layout().let { it.top.contains(p) || it.panel.contains(p) || it.scan.contains(p) || it.open.contains(p) || it.mission.contains(p) || it.back.contains(p) }
            return onHud || exploration
        }
        override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
            val p = hudPoint(x, y)
            if (!exploration) { if (modeButton().contains(p)) enterMap(); return modeButton().contains(p) }
            val l = layout()
            when { l.back.contains(p) -> leaveMap(); l.scan.contains(p) && canScan() -> scan(); l.open.contains(p) && canOpen() -> open(); l.mission.contains(p) -> centerTarget(); l.top.contains(p) || l.panel.contains(p) -> Unit; else -> { select(x, y); if (count >= 2) center(selected) } }
            return true
        }
        override fun pan(x: Float, y: Float, dx: Float, dy: Float): Boolean {
            if (!exploration || onHud) return false
            wc.position.x -= dx * wc.viewportWidth * wc.zoom / wv.screenWidth.coerceAtLeast(1); wc.position.y += dy * wc.viewportHeight * wc.zoom / wv.screenHeight.coerceAtLeast(1); clamp(); return true
        }
        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (!exploration || onHud || distance <= MathUtils.FLOAT_ROUNDING_ERROR) return false
            val base = if (lastPinch <= 0f) initialDistance else lastPinch; wc.zoom = (wc.zoom * base / distance).coerceIn(.58f, 1.55f); lastPinch = distance; clamp(); return true
        }
        override fun pinchStop() { lastPinch = 0f }
    }

    private data class Layout(val top: Rectangle, val panel: Rectangle, val scan: Rectangle, val open: Rectangle, val mission: Rectangle, val back: Rectangle)
    private companion object {
        const val MW = 1600f; const val MH = 900f; const val OPEN_MS = 900f
        val TECH1 = GameId.of("tech_extraction_protocol"); val TECH2 = GameId.of("tech_quantum_sorting")
        val POWER = GameId.of("component_power_cell"); val CORE = GameId.of("sector_core_delta"); val COPPER = GameId.of("sector_copper_ridge")
        val CRYSTAL = GameId.of("sector_crystal_flats"); val LOGISTICS = GameId.of("sector_logistics_pass"); val XENON = GameId.of("sector_xenon_depths")
        val RARE_P = GameId.of("rare_deposit_prismatic_ferrite"); val RARE_X = GameId.of("rare_deposit_xenon"); val RARE_A = GameId.of("rare_deposit_archive_fragment")
        val PRISMATIC = GameId.of("rare_prismatic_ferrite"); val XENON_R = GameId.of("rare_xenon_crystal"); val ARCHIVE_R = GameId.of("rare_archive_fragment")
        val BG = Color(.008f,.014f,.035f,1f); val GROUND = Color(.055f,.065f,.085f,1f); val GRID = Color(.16f,.20f,.25f,1f); val FOG = Color(.025f,.035f,.055f,1f)
        val FOG_LINE = Color(.04f,.055f,.08f,1f); val SCANNED = Color(.10f,.13f,.17f,1f); val CORE_C = Color(.18f,.25f,.31f,1f); val COPPER_C = Color(.30f,.17f,.11f,1f)
        val CRYSTAL_C = Color(.17f,.20f,.36f,1f); val LOG_C = Color(.18f,.29f,.24f,1f); val XENON_C = Color(.24f,.14f,.34f,1f); val ARCHIVE_C = Color(.31f,.24f,.13f,1f)
        val RARE = Color(.70f,.45f,.95f,1f); val MISSION = Color(.65f,.48f,.20f,1f); val MISSION_ACTIVE = Color(1f,.76f,.22f,1f); val ACCENT = Color(.20f,.82f,.88f,1f)
        val SELECT = Color(.96f,.78f,.24f,1f); val HUD = Color(.025f,.055f,.10f,1f); val PANEL = Color(.035f,.075f,.13f,1f); val BUTTON = Color(.08f,.18f,.26f,1f)
        val DISABLED = Color(.05f,.08f,.11f,1f); val TEXT = Color(.90f,.96f,1f,1f); val MUTED = Color(.61f,.72f,.82f,1f)
    }
}
