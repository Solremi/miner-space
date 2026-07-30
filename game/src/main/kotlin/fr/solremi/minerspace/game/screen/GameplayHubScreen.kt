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

/**
 * Conserve la scène production/exploration de l'étape 6 intacte et ajoute
 * seulement un accès persistant à l'événement météorique.
 */
class GameplayHubScreen(
    services: GameServices,
    private val onMeteorRequested: () -> Unit,
) : KtxScreen {
    private val gameplay = SectorExplorationScreen(services)
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.64f) }
    private var delegatedInput: InputProcessor? = null
    private val meteorInput = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat())
            viewport.unproject(point)
            if (!meteorButton().contains(point)) return false
            onMeteorRequested()
            return true
        }
    }
    private var input = InputMultiplexer()

    override fun show() {
        gameplay.show()
        delegatedInput = Gdx.input.inputProcessor
        installInput()
    }

    override fun hide() {
        gameplay.hide()
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
        delegatedInput = null
    }

    override fun resize(width: Int, height: Int) {
        gameplay.resize(width, height)
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        gameplay.render(delta)
        drawMeteorButton()
    }

    private fun installInput() {
        input = InputMultiplexer()
        input.addProcessor(meteorInput)
        delegatedInput?.let(input::addProcessor)
        Gdx.input.inputProcessor = input
    }

    private fun drawMeteorButton() {
        viewport.apply()
        camera.update()
        val button = meteorButton()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = BUTTON
        shapes.rect(button.x, button.y, button.width, button.height)
        shapes.color = ACCENT
        shapes.rect(button.x, button.y, button.width, 4f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "MÉTÉORES", button.x + 18f, button.y + 30f)
        batch.end()
    }

    private fun meteorButton(): Rectangle {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat()
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val top = max(1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        return Rectangle(left, top - 48f, 118f, 48f)
    }

    override fun dispose() {
        hide()
        gameplay.dispose()
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }

    private companion object {
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val ACCENT = Color(.92f, .48f, 1f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
    }
}
