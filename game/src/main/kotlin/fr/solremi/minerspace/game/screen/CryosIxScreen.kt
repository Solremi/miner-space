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
import fr.solremi.minerspace.data.cryos.CryosIxContentFactory
import fr.solremi.minerspace.data.save.CryosIxStateCodec
import fr.solremi.minerspace.data.save.PrestigeStateCodec
import fr.solremi.minerspace.domain.cryos.*
import fr.solremi.minerspace.domain.prestige.PlanetPrestigeEngine
import fr.solremi.minerspace.domain.prestige.PrestigeState
import fr.solremi.minerspace.domain.services.*
import ktx.app.KtxScreen
import kotlin.math.max

class CryosIxScreen(
    private val services: GameServices,
    private val onTransferRequested: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.69f) }
    private val small = BitmapFont().apply { data.setScale(.53f) }
    private val definitions = CryosIxContentFactory.create()
    private val engine = CryosIxEngine(definitions)
    private val codec = CryosIxStateCodec()
    private val prestigeCodec = PrestigeStateCodec()
    private var prestige = loadPrestige()
    private var state = loadState()
    private var mode = if (state.baseInstalled) Mode.EXTRACT else Mode.BASE
    private var resourceCursor = 0
    private var recipeCursor = 0
    private var message = "Installez la base, produisez énergie et chaleur, puis étendez le réseau."
    private var current: Layout? = null
    private val lifecycle = LifecycleObserver { if (it == LifecycleState.BACKGROUND) save() }
    private val input = object : InputAdapter() {
        override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(x.toFloat(), y.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() {
        prestige = loadPrestige(); state = loadState()
        services.lifecycle.addObserver(lifecycle); Gdx.input.inputProcessor = input
    }
    override fun hide() {
        save(); services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }
    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BG); viewport.apply(); camera.update()
        val l = layout(); current = l
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = SKY; shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.color = AURORA; shapes.rect(0f, l.panel.y - 22f, viewport.worldWidth, 26f)
        shapes.color = TOP; shapes.rect(l.top.x, l.top.y, l.top.width, l.top.height)
        shapes.color = PANEL; shapes.rect(l.panel.x, l.panel.y, l.panel.width, l.panel.height)
        l.buttons.forEachIndexed { i, r -> button(r, i < 4 || state.baseInstalled, accent(i)) }
        shapes.end()

        batch.projectionMatrix = camera.combined; batch.begin(); font.color = TEXT; small.color = MUTED
        font.draw(batch, "CRYOS IX · RÉSEAU THERMIQUE", l.top.x + 12f, l.top.y + 36f)
        small.draw(batch, "Énergie ${state.energy}/500 · Chaleur ${state.heat}/500 · Froid ${state.coldExposure}", l.top.x + 12f, l.top.y + 14f)
        font.draw(batch, "PROGRESSION", l.panel.x + 12f, l.panel.y + l.panel.height - 14f)
        small.draw(batch, "Base ${yes(state.baseInstalled)} · nœuds ${state.thermalNodes}/5 · secteurs ${state.unlockedSectorIds.size}/6", l.panel.x + 12f, l.panel.y + l.panel.height - 40f)
        small.draw(batch, "Tech ${state.installedTechnologyIds.size}/5 · modules ${state.craftedModuleIds.size}/8 · missions ${state.completedMainMissionIds.size}/12", l.panel.x + 12f, l.panel.y + l.panel.height - 61f)
        small.draw(batch, "Vétéran ${prestige.veteranRobot?.displayName ?: "absent"} · Noyaux ${prestige.stellarCores} · Frontière ${yes(state.frontierUnlocked)}", l.panel.x + 12f, l.panel.y + l.panel.height - 82f)
        definitions.sectors.values.forEachIndexed { index, s ->
            val open = s.id in state.unlockedSectorIds
            small.color = if (open) ICE else MUTED
            small.draw(batch, "${if (open) "●" else "○"} ${s.name} · nœud ${s.requiredThermalNodes} · chaleur ${s.minimumHeat}", l.panel.x + 12f, l.panel.y + l.panel.height - 109f - index * 18f)
        }
        small.color = MUTED; small.draw(batch, message, l.message.x, l.message.y + 17f)
        val labels = listOf("ÉNERGIE", "CHALEUR", mode.label, "ACTION", "STELLAIRE")
        l.buttons.forEachIndexed { i, r -> small.draw(batch, labels[i], r.x + 7f, r.y + 29f) }
        batch.end()
    }

    private fun touch(point: Vector2) {
        val l = current ?: return
        when {
            l.buttons[0].contains(point) -> apply(engine.generateEnergy(state))
            l.buttons[1].contains(point) -> apply(engine.heatBase(state))
            l.buttons[2].contains(point) -> cycleMode()
            l.buttons[3].contains(point) -> execute()
            l.buttons[4].contains(point) -> onTransferRequested()
        }
    }

    private fun cycleMode() {
        val values = Mode.entries; mode = values[(mode.ordinal + 1) % values.size]
        if (!state.baseInstalled) mode = Mode.BASE
        message = mode.description; services.haptic.impact()
    }

    private fun execute() {
        val result = when (mode) {
            Mode.BASE -> engine.installBase(state)
            Mode.EXTRACT -> definitions.resources.keys.toList().let { ids ->
                val id = ids[resourceCursor++ % ids.size]; engine.extract(state, id)
            }
            Mode.REFINE -> definitions.recipes.keys.toList().let { ids ->
                val id = ids[recipeCursor++ % ids.size]; engine.refine(state, id)
            }
            Mode.NETWORK -> engine.buildThermalNode(state)
            Mode.SECTOR -> engine.unlockNextSector(state)
            Mode.TECH -> engine.installNextTechnology(state)
            Mode.MODULE -> engine.craftNextModule(state)
            Mode.EVENT -> engine.resolveNextEvent(state)
            Mode.OBJECTIVE -> engine.completePlanetaryObjective(state)
        }
        apply(result)
    }

    private fun apply(result: CryosCommandResult) {
        when (result) {
            is CryosCommandResult.Rejected -> { message = reject(result.code); services.haptic.warning() }
            is CryosCommandResult.Applied -> {
                state = result.state; message = success(result.transaction.reason)
                if (save()) services.haptic.success() else { message = "Action appliquée · sauvegarde en attente"; services.haptic.warning() }
                if (mode == Mode.BASE && state.baseInstalled) mode = Mode.EXTRACT
            }
        }
    }

    private fun loadState(): CryosIxState {
        val initial = engine.initialState(prestige.veteranRobot?.id)
        val payload = services.save.loadLatest(CryosIxStateCodec.SLOT_ID) ?: return initial
        return runCatching { require(payload.contentVersion == definitions.contentVersion); engine.normalize(codec.decode(payload)) }.getOrElse { initial }
    }
    private fun loadPrestige(): PrestigeState {
        val initial = PlanetPrestigeEngine().initialState()
        val payload = services.save.loadLatest(PrestigeStateCodec.SLOT_ID) ?: return initial
        return runCatching { prestigeCodec.decode(payload) }.getOrElse { initial }
    }
    private fun save() = services.save.save(codec.encode(state, definitions.contentVersion, now())) == SaveWriteStatus.WRITTEN
    private fun now() = services.clock.nowEpochMillis().coerceAtLeast(0L)

    private fun layout(): Layout {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        val topBar = Rectangle(left, top - 52f, right - left, 52f)
        val gap = 6f; val bw = (right - left - gap * 4f) / 5f
        val buttons = (0..4).map { Rectangle(left + it * (bw + gap), bottom, bw, 48f) }
        val message = Rectangle(left, bottom + 54f, right - left, 30f)
        return Layout(topBar, Rectangle(left, message.y + 34f, right - left, topBar.y - message.y - 42f), message, buttons)
    }
    private fun button(r: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED; shapes.rect(r.x, r.y, r.width, r.height)
        shapes.color = if (enabled) accent else GRID; shapes.rect(r.x, r.y, r.width, 4f)
    }
    private fun accent(index: Int) = when (index) { 0 -> ENERGY; 1 -> HEAT; 2 -> ICE; 3 -> ACTION; else -> STAR }
    private fun yes(value: Boolean) = if (value) "oui" else "non"

    private fun reject(code: String) = when (code) {
        "base_required" -> "Installez d’abord la base"
        "energy_insufficient" -> "Énergie insuffisante"
        "heat_insufficient" -> "Chaleur insuffisante : renforcez le réseau"
        "sector_locked" -> "Source située dans un secteur verrouillé"
        "materials_insufficient", "thermal_materials_insufficient", "technology_materials_insufficient", "module_materials_insufficient" -> "Matériaux insuffisants"
        "thermal_network_incomplete" -> "Construisez les cinq nœuds thermiques"
        "all_sectors_required" -> "Ouvrez les six secteurs"
        "technologies_required" -> "Trois technologies sont requises"
        "cryogenic_module_required" -> "Fabriquez un module cryogénique"
        else -> code
    }
    private fun success(reason: String) = when (reason) {
        "install_cryos_base" -> "Base installée · énergie et chaleur disponibles"
        "generate_energy" -> "Générateur actif"
        "heat_base" -> "Réserve thermique renforcée"
        "extract_cryos_resource" -> "Extraction réussie · le froid consomme la chaleur"
        "refine_cryos_material" -> "Matériau cryogénique raffiné"
        "build_thermal_node" -> "Nœud thermique construit"
        "unlock_cryos_sector" -> "Secteur relié au réseau"
        "install_cryos_technology" -> "Technologie Cryos installée"
        "craft_cryogenic_module" -> "Module thermique fabriqué"
        "resolve_cryos_event" -> "Événement froid résolu sans pénalité permanente"
        "complete_cryos_major_objective" -> "Cryos IX stabilisée · frontière ouverte"
        else -> reason
    }

    override fun dispose() { hide(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }
    private enum class Mode(val label: String, val description: String) {
        BASE("BASE", "Installer la base"), EXTRACT("EXTRAIRE", "Extraire malgré le froid"), REFINE("RAFFINER", "Transformer les ressources locales"),
        NETWORK("RÉSEAU", "Étendre le réseau thermique"), SECTOR("SECTEUR", "Ouvrir le secteur relié suivant"), TECH("TECH", "Installer une technologie propre"),
        MODULE("MODULE", "Fabriquer un module cryogénique"), EVENT("ÉVÉNEMENT", "Résoudre un événement froid"), OBJECTIVE("OBJECTIF", "Stabiliser la planète")
    }
    private data class Layout(val top: Rectangle, val panel: Rectangle, val message: Rectangle, val buttons: List<Rectangle>)
    private companion object {
        val BG = Color(.005f,.012f,.025f,1f); val SKY = Color(.018f,.055f,.095f,1f); val AURORA = Color(.12f,.55f,.72f,.22f)
        val TOP = Color(.035f,.09f,.14f,.98f); val PANEL = Color(.045f,.105f,.15f,.96f); val BUTTON = Color(.065f,.16f,.22f,1f)
        val DISABLED = Color(.03f,.055f,.075f,1f); val GRID = Color(.12f,.20f,.26f,1f); val ENERGY = Color(.95f,.76f,.20f,1f)
        val HEAT = Color(.95f,.34f,.16f,1f); val ICE = Color(.28f,.84f,.95f,1f); val ACTION = Color(.42f,.80f,.58f,1f)
        val STAR = Color(.74f,.48f,.96f,1f); val TEXT = Color(.94f,.97f,1f,1f); val MUTED = Color(.65f,.76f,.84f,1f)
    }
}
