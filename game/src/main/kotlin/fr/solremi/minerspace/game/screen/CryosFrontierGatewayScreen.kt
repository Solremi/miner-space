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
import fr.solremi.minerspace.data.cryos.CryosIxContentFactory
import fr.solremi.minerspace.data.save.CryosIxStateCodec
import fr.solremi.minerspace.domain.cryos.CryosIxEngine
import fr.solremi.minerspace.domain.services.GameServices
import ktx.app.KtxScreen
import kotlin.math.max

class CryosFrontierGatewayScreen(
    private val services: GameServices,
    onTransferRequested: () -> Unit,
    private val onFrontierRequested: () -> Unit,
) : KtxScreen {
    private val cryos = CryosIxScreen(services, onTransferRequested)
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.5f) }
    private var frontierUnlocked = false
    private var delegated: InputProcessor? = null
    private var input = InputMultiplexer()
    private val overlay = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject)
            if (!frontierButton().contains(point)) return false
            frontierUnlocked = loadFrontierUnlocked()
            if (frontierUnlocked) {
                services.haptic.success(); onFrontierRequested()
            } else {
                services.haptic.warning()
            }
            return true
        }
    }

    override fun show() {
        frontierUnlocked = loadFrontierUnlocked()
        cryos.show(); delegated = Gdx.input.inputProcessor
        input = InputMultiplexer().apply { addProcessor(overlay); delegated?.let(::addProcessor) }
        Gdx.input.inputProcessor = input
    }

    override fun hide() {
        cryos.hide()
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
        delegated = null
    }

    override fun resize(width: Int, height: Int) {
        cryos.resize(width, height)
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        cryos.render(delta); viewport.apply(); camera.update()
        val button = frontierButton()
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = if (frontierUnlocked) BUTTON else DISABLED; shapes.rect(button.x, button.y, button.width, button.height)
        shapes.color = if (frontierUnlocked) ACCENT else MUTED; shapes.rect(button.x, button.y, button.width, 4f)
        shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin(); font.color = if (frontierUnlocked) TEXT else MUTED
        font.draw(batch, "FRONTIÈRE", button.x + 8f, button.y + 29f); batch.end()
    }

    private fun loadFrontierUnlocked(): Boolean {
        val definitions = CryosIxContentFactory.create(); val engine = CryosIxEngine(definitions)
        val payload = services.save.loadLatest(CryosIxStateCodec.SLOT_ID) ?: return false
        return runCatching { engine.normalize(CryosIxStateCodec().decode(payload)).frontierUnlocked }.getOrDefault(false)
    }

    private fun frontierButton(): Rectangle {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val right = max(1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val top = max(1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        return Rectangle(right - 104f, top - 48f, 104f, 48f)
    }

    override fun dispose() { hide(); cryos.dispose(); shapes.dispose(); batch.dispose(); font.dispose() }

    private companion object {
        val BUTTON = Color(.065f,.16f,.22f,.96f); val DISABLED = Color(.03f,.055f,.075f,.94f)
        val ACCENT = Color(.72f,.48f,.96f,1f); val TEXT = Color(.94f,.97f,1f,1f); val MUTED = Color(.45f,.55f,.62f,1f)
    }
}
