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
import fr.solremi.minerspace.data.frontier.FrontierContentFactory
import fr.solremi.minerspace.data.save.FrontierStateCodec
import fr.solremi.minerspace.domain.frontier.*
import fr.solremi.minerspace.domain.services.*
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class InterplanetaryFrontierScreen(
    private val services: GameServices,
    private val onCryosRequested: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.67f) }
    private val small = BitmapFont().apply { data.setScale(.51f) }
    private val definitions = FrontierContentFactory.create()
    private val engine = FrontierEngine(definitions)
    private val codec = FrontierStateCodec()
    private var state = loadOrBootstrap()
    private var message = "La frontière reste ouverte : stabilisez un monde ou choisissez une autre route."
    private var current: Layout? = null
    private val lifecycle = LifecycleObserver { if (it == LifecycleState.BACKGROUND) save() }
    private val input = object : InputAdapter() {
        override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(x.toFloat(), y.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() {
        state = loadOrBootstrap()
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
    }

    override fun hide() {
        save(); services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BG); viewport.apply(); camera.update()
        val layout = layout(); current = layout
        val worlds = orderedWorlds()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = SPACE; shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        drawStars(layout.map)
        shapes.color = TOP; shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL; shapes.rect(layout.info.x, layout.info.y, layout.info.width, layout.info.height)
        drawWorldNodes(layout.map, worlds)
        layout.buttons.forEachIndexed { index, rectangle -> button(rectangle, index != 0 || activeIncompleteCount() < 3, accent(index)) }
        shapes.end()

        batch.projectionMatrix = camera.combined; batch.begin(); font.color = TEXT; small.color = MUTED
        font.draw(batch, "FRONTIÈRE INTERPLANÉTAIRE", layout.top.x + 12f, layout.top.y + 36f)
        small.draw(batch, "Graine ${state.seed} · mondes ${state.worlds.size} · stabilisés ${state.completedWorldCount}", layout.top.x + 12f, layout.top.y + 14f)
        val selected = selectedWorld()
        if (selected == null) {
            font.draw(batch, "AUCUN MONDE", layout.info.x + 12f, layout.info.y + layout.info.height - 18f)
            small.draw(batch, "Générez une première route contrôlée.", layout.info.x + 12f, layout.info.y + layout.info.height - 45f)
        } else {
            val definition = selected.definition
            font.draw(batch, familyName(definition.family), layout.info.x + 12f, layout.info.y + layout.info.height - 18f)
            small.draw(batch, "${difficultyName(definition.difficulty)} · ${definition.estimatedDays} jours ciblés", layout.info.x + 12f, layout.info.y + layout.info.height - 43f)
            small.draw(batch, objectiveName(definition.objectiveId), layout.info.x + 12f, layout.info.y + layout.info.height - 65f)
            small.draw(batch, "Progression ${selected.progress}/${definition.targetProgress} · actions ${selected.actionCount}", layout.info.x + 12f, layout.info.y + layout.info.height - 87f)
            small.draw(batch, "Secteurs ${definition.sectors.size} · chaîne vérifiée", layout.info.x + 12f, layout.info.y + layout.info.height - 109f)
            small.draw(batch, "Modificateurs :", layout.info.x + 12f, layout.info.y + layout.info.height - 134f)
            definition.modifierIds.sortedBy { it.value }.take(3).forEachIndexed { index, id ->
                small.draw(batch, "• ${definitions.modifiers.getValue(id).name}", layout.info.x + 18f, layout.info.y + layout.info.height - 155f - index * 19f)
            }
            val reward = when (definition.rewardKind) {
                FrontierRewardKind.PERMANENT_BONUS -> "bonus permanent"
                FrontierRewardKind.COSMETIC -> "élément cosmétique"
                FrontierRewardKind.COLLECTION -> "entrée de collection"
            }
            small.draw(batch, "Récompense : $reward ×${definition.rewardAmount}", layout.info.x + 12f, layout.info.y + 22f)
        }
        small.color = MUTED; small.draw(batch, message, layout.message.x, layout.message.y + 17f)
        val labels = listOf("NOUVEAU", "MONDE", "ACTION", "ARCHIVES", "CRYOS")
        layout.buttons.forEachIndexed { index, rectangle -> small.draw(batch, labels[index], rectangle.x + 7f, rectangle.y + 29f) }
        batch.end()
    }

    private fun touch(point: Vector2) {
        val layout = current ?: return
        when {
            layout.buttons[0].contains(point) -> discover()
            layout.buttons[1].contains(point) -> cycleWorld()
            layout.buttons[2].contains(point) -> apply(engine.performAction(state, now()))
            layout.buttons[3].contains(point) -> showArchive()
            layout.buttons[4].contains(point) -> onCryosRequested()
        }
    }

    private fun discover() {
        val difficulty = FrontierDifficulty.entries[state.nextGenerationIndex % FrontierDifficulty.entries.size]
        apply(engine.discoverWorld(state, difficulty, now()))
    }

    private fun cycleWorld() {
        val worlds = orderedWorlds()
        if (worlds.isEmpty()) { message = "Aucun monde enregistré"; services.haptic.warning(); return }
        val currentIndex = worlds.indexOfFirst { it.definition.id == state.activeWorldId }.coerceAtLeast(0)
        val next = worlds[(currentIndex + 1) % worlds.size]
        apply(engine.selectWorld(state, next.definition.id))
    }

    private fun showArchive() {
        val completed = orderedWorlds().filter { it.status == FrontierWorldStatus.COMPLETED }
        if (completed.isEmpty()) {
            message = "Archives vides · aucun monde stabilisé"
            services.haptic.impact(); return
        }
        val currentIndex = completed.indexOfFirst { it.definition.id == state.activeWorldId }
        val selected = completed[(currentIndex + 1).mod(completed.size)]
        apply(engine.selectWorld(state, selected.definition.id))
        message = "Archive ${selected.definition.generationIndex + 1} · récompense conservée"
    }

    private fun apply(result: FrontierCommandResult) {
        when (result) {
            is FrontierCommandResult.Rejected -> {
                message = when (result.code) {
                    "active_world_limit" -> "Trois mondes actifs maximum · stabilisez une route"
                    "no_active_world" -> "Sélectionnez ou générez un monde"
                    "world_already_completed" -> "Monde déjà stabilisé · une nouvelle route reste disponible"
                    else -> result.code
                }
                services.haptic.warning()
            }
            is FrontierCommandResult.Applied -> {
                state = result.state
                message = when (result.transaction.reason) {
                    "discover_frontier_world" -> "Nouvelle route générée et validée"
                    "select_frontier_world" -> "Monde sélectionné"
                    "advance_frontier_world" -> "Progression enregistrée sur le monde actif"
                    "complete_frontier_world" -> "Monde stabilisé · nouvelle route disponible"
                    else -> result.transaction.reason
                }
                if (save()) services.haptic.success() else services.haptic.warning()
            }
        }
    }

    private fun loadOrBootstrap(): FrontierState {
        val payload = services.save.loadLatest(FrontierStateCodec.SLOT_ID)
        if (payload != null) {
            runCatching { return engine.normalize(codec.decode(payload)) }
        }
        var initial = engine.initialState(now() xor SEED_SALT)
        FrontierDifficulty.entries.forEachIndexed { index, difficulty ->
            initial = (engine.discoverWorld(initial, difficulty, now() + index) as FrontierCommandResult.Applied).state
        }
        initial = (engine.selectWorld(initial, initial.worlds.values.minBy { it.definition.generationIndex }.definition.id) as FrontierCommandResult.Applied).state
        services.save.save(codec.encode(initial, now()))
        return initial
    }

    private fun save() = services.save.save(codec.encode(state, now())) == SaveWriteStatus.WRITTEN
    private fun selectedWorld() = state.activeWorldId?.let(state.worlds::get)
    private fun orderedWorlds() = state.worlds.values.sortedBy { it.definition.generationIndex }
    private fun activeIncompleteCount() = state.worlds.values.count { it.status == FrontierWorldStatus.ACTIVE }
    private fun now() = services.clock.nowEpochMillis().coerceAtLeast(0L)

    private fun drawWorldNodes(map: Rectangle, worlds: List<FrontierWorldProgress>) {
        worlds.takeLast(8).forEachIndexed { index, world ->
            val column = index % 4; val row = index / 4
            val x = map.x + map.width * (.14f + column * .24f)
            val y = map.y + map.height * (.68f - row * .42f)
            val selected = world.definition.id == state.activeWorldId
            shapes.color = if (selected) SELECT else familyColor(world.definition.family)
            shapes.circle(x, y, if (selected) 13f else 9f, 24)
            if (world.status == FrontierWorldStatus.COMPLETED) {
                shapes.color = COMPLETE; shapes.circle(x, y, 4f, 16)
            }
        }
    }

    private fun drawStars(map: Rectangle) {
        repeat(28) { index ->
            val x = map.x + ((index * 73) % max(1, map.width.toInt())).toFloat()
            val y = map.y + ((index * 47) % max(1, map.height.toInt())).toFloat()
            shapes.color = if (index % 3 == 0) STAR else STAR_DIM
            shapes.circle(x, y, if (index % 5 == 0) 1.6f else 1f, 8)
        }
    }

    private fun layout(): Layout {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        val topBar = Rectangle(left, top - 52f, right - left, 52f)
        val gap = 6f; val buttonWidth = (right - left - gap * 4f) / 5f
        val buttons = (0..4).map { Rectangle(left + it * (buttonWidth + gap), bottom, buttonWidth, 48f) }
        val message = Rectangle(left, bottom + 54f, right - left, 30f)
        val middleBottom = message.y + 34f; val middleHeight = topBar.y - middleBottom - 8f
        val infoWidth = (right - left) * .42f
        val info = Rectangle(right - infoWidth, middleBottom, infoWidth, middleHeight)
        val map = Rectangle(left, middleBottom, info.x - left - 8f, middleHeight)
        return Layout(topBar, map, info, message, buttons)
    }

    private fun button(rectangle: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED; shapes.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height)
        shapes.color = if (enabled) accent else GRID; shapes.rect(rectangle.x, rectangle.y, rectangle.width, 4f)
    }

    private fun accent(index: Int) = when (index) { 0 -> NEW; 1 -> SELECT; 2 -> ACTION; 3 -> COMPLETE; else -> ICE }
    private fun familyColor(family: FrontierVisualFamily) = when (family) {
        FrontierVisualFamily.VOLCANIC -> VOLCANIC
        FrontierVisualFamily.CRYSTALLINE -> CRYSTAL
        FrontierVisualFamily.DERELICT -> DERELICT
    }
    private fun familyName(family: FrontierVisualFamily) = when (family) {
        FrontierVisualFamily.VOLCANIC -> "MONDE VOLCANIQUE"
        FrontierVisualFamily.CRYSTALLINE -> "MONDE CRISTALLIN"
        FrontierVisualFamily.DERELICT -> "MONDE ÉPAVE"
    }
    private fun difficultyName(difficulty: FrontierDifficulty) = when (difficulty) {
        FrontierDifficulty.SCOUT -> "Éclaireur"
        FrontierDifficulty.EXPEDITION -> "Expédition"
        FrontierDifficulty.DEEP -> "Profonde"
    }
    private fun objectiveName(id: GameId) = definitions.objectives[id]?.name ?: id.value

    override fun dispose() { hide(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }

    private data class Layout(val top: Rectangle, val map: Rectangle, val info: Rectangle, val message: Rectangle, val buttons: List<Rectangle>)
    private companion object {
        const val SEED_SALT = 0x5A17C0DEL
        val BG = Color(.004f,.008f,.018f,1f); val SPACE = Color(.008f,.018f,.04f,1f); val TOP = Color(.025f,.055f,.10f,.98f)
        val PANEL = Color(.035f,.075f,.12f,.96f); val BUTTON = Color(.06f,.14f,.21f,1f); val DISABLED = Color(.025f,.045f,.065f,1f)
        val GRID = Color(.12f,.19f,.25f,1f); val TEXT = Color(.94f,.97f,1f,1f); val MUTED = Color(.64f,.75f,.84f,1f)
        val STAR = Color(.78f,.88f,1f,.82f); val STAR_DIM = Color(.35f,.48f,.64f,.45f); val VOLCANIC = Color(.95f,.34f,.14f,1f)
        val CRYSTAL = Color(.30f,.86f,.96f,1f); val DERELICT = Color(.68f,.62f,.52f,1f); val SELECT = Color(.96f,.76f,.24f,1f)
        val COMPLETE = Color(.45f,.95f,.64f,1f); val NEW = Color(.72f,.48f,.95f,1f); val ACTION = Color(.38f,.82f,.62f,1f); val ICE = Color(.28f,.80f,.95f,1f)
    }
}
