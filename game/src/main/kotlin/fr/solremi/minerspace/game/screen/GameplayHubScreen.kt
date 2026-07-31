package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.domain.services.GameServices
import ktx.app.KtxScreen
import kotlin.math.max

/** Conserve production et exploration, puis ajoute les accès transverses. */
class GameplayHubScreen(
    services: GameServices,
    private val onMeteorRequested: () -> Unit,
    private val onRobotsRequested: () -> Unit,
    private val onStrategyRequested: () -> Unit,
    private val onMissionsRequested: () -> Unit,
    private val onArchivesRequested: () -> Unit,
) : KtxScreen {
    private val gameplay = SectorExplorationScreen(services)
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.50f) }
    private var delegatedInput: InputProcessor? = null
    private val overlayInput = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat()); viewport.unproject(point)
            return when {
                meteorButton().contains(point) -> { onMeteorRequested(); true }
                robotsButton().contains(point) -> { onRobotsRequested(); true }
                strategyButton().contains(point) -> { onStrategyRequested(); true }
                missionsButton().contains(point) -> { onMissionsRequested(); true }
                archivesButton().contains(point) -> { onArchivesRequested(); true }
                else -> false
            }
        }
    }
    private var input = InputMultiplexer()

    override fun show() { gameplay.show(); delegatedInput = Gdx.input.inputProcessor; installInput() }
    override fun hide() { gameplay.hide(); if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null; delegatedInput = null }
    override fun resize(width: Int, height: Int) { gameplay.resize(width, height); viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true) }
    override fun render(delta: Float) { gameplay.render(delta); drawOverlayButtons() }

    private fun installInput() {
        input = InputMultiplexer(); input.addProcessor(overlayInput); delegatedInput?.let(input::addProcessor); Gdx.input.inputProcessor = input
    }

    private fun drawOverlayButtons() {
        viewport.apply(); camera.update()
        val meteor = meteorButton(); val robots = robotsButton(); val strategy = strategyButton(); val missions = missionsButton(); val archives = archivesButton()
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawButton(meteor, METEOR_ACCENT); drawButton(robots, ROBOT_ACCENT); drawButton(strategy, STRATEGY_ACCENT); drawButton(missions, MISSION_ACCENT); drawButton(archives, ARCHIVE_ACCENT)
        shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin(); font.color = TEXT
        font.draw(batch, "MÉTÉORES", meteor.x + 9f, meteor.y + 29f)
        font.draw(batch, "ROBOTS", robots.x + 14f, robots.y + 29f)
        font.draw(batch, "STRATÉGIE", strategy.x + 8f, strategy.y + 29f)
        font.draw(batch, "MISSIONS", missions.x + 10f, missions.y + 29f)
        font.draw(batch, "ARCHIVES", archives.x + 9f, archives.y + 29f)
        batch.end()
    }

    private fun drawButton(rect: Rectangle, accent: Color) {
        shapes.color = BUTTON; shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = accent; shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun meteorButton(): Rectangle { val (left, top) = safeTopLeft(); return Rectangle(left, top - 48f, 92f, 48f) }
    private fun robotsButton(): Rectangle { val p = meteorButton(); return Rectangle(p.x + p.width + 5f, p.y, 78f, 48f) }
    private fun strategyButton(): Rectangle { val p = robotsButton(); return Rectangle(p.x + p.width + 5f, p.y, 94f, 48f) }
    private fun missionsButton(): Rectangle { val p = strategyButton(); return Rectangle(p.x + p.width + 5f, p.y, 86f, 48f) }
    private fun archivesButton(): Rectangle { val p = missionsButton(); return Rectangle(p.x + p.width + 5f, p.y, 88f, 48f) }

    private fun safeTopLeft(): Pair<Float, Float> {
        val width = viewport.worldWidth; val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat(); val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        return Gdx.graphics.safeInsetLeft * scaleX + 8f to max(1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
    }

    override fun dispose() { hide(); gameplay.dispose(); shapes.dispose(); batch.dispose(); font.dispose() }

    private companion object {
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val METEOR_ACCENT = Color(.92f, .48f, 1f, 1f)
        val ROBOT_ACCENT = Color(.20f, .82f, .88f, 1f)
        val STRATEGY_ACCENT = Color(.84f, .62f, .20f, 1f)
        val MISSION_ACCENT = Color(.36f, .78f, .42f, 1f)
        val ARCHIVE_ACCENT = Color(.72f, .46f, .96f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
    }
}
