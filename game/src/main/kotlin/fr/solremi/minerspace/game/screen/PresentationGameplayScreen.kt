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
import fr.solremi.minerspace.domain.presentation.FeedbackKind
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SoundCue
import fr.solremi.minerspace.game.presentation.FerrumVisualLayer
import fr.solremi.minerspace.game.presentation.GameFeedbackBus
import fr.solremi.minerspace.game.presentation.PresentationController
import ktx.app.KtxScreen
import kotlin.math.max

class PresentationGameplayScreen(
    private val services: GameServices,
    onMeteorRequested: () -> Unit,
    onRobotsRequested: () -> Unit,
    onStrategyRequested: () -> Unit,
    onMissionsRequested: () -> Unit,
    onArchivesRequested: () -> Unit,
    private val onPresentationRequested: () -> Unit,
    private val onTransferRequested: () -> Unit,
) : KtxScreen {
    private val gameplay = GameplayHubScreen(
        services,
        onMeteorRequested,
        onRobotsRequested,
        onStrategyRequested,
        onMissionsRequested,
        onArchivesRequested,
    )
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.5f) }
    private val effects = FerrumVisualLayer()
    private var delegated: InputProcessor? = null
    private var input = InputMultiplexer()
    private val overlay = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat()); viewport.unproject(point)
            val x = point.x / viewport.worldWidth; val y = point.y / viewport.worldHeight
            return when {
                transferButton().contains(point) -> {
                    PresentationController.play(services, SoundCue.LAUNCH, FeedbackKind.LAUNCH, x, y)
                    onTransferRequested(); true
                }
                settingsButton().contains(point) -> {
                    PresentationController.play(services, SoundCue.INTERACTION, FeedbackKind.INTERACTION, x, y)
                    onPresentationRequested(); true
                }
                meteorButton().contains(point) -> cue(SoundCue.LAUNCH, FeedbackKind.LAUNCH, x, y)
                archivesButton().contains(point) -> cue(SoundCue.RARITY, FeedbackKind.RARE, x, y)
                robotsButton().contains(point) || strategyButton().contains(point) || missionsButton().contains(point) ->
                    cue(SoundCue.INTERACTION, FeedbackKind.INTERACTION, x, y)
                else -> {
                    GameFeedbackBus.emit(FeedbackKind.INTERACTION, services.clock.monotonicMillis(), x, y)
                    false
                }
            }
        }
    }

    override fun show() {
        PresentationController.loadAndApply(services)
        gameplay.show(); delegated = Gdx.input.inputProcessor
        input = InputMultiplexer().apply { addProcessor(overlay); delegated?.let(::addProcessor) }
        Gdx.input.inputProcessor = input
    }

    override fun hide() {
        gameplay.hide()
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
        delegated = null
    }

    override fun resize(width: Int, height: Int) {
        gameplay.resize(width, height)
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        gameplay.render(delta)
        viewport.apply(); camera.update()
        effects.draw(camera, viewport.worldWidth, viewport.worldHeight, services.clock.monotonicMillis())
        val transfer = transferButton(); val settings = settingsButton()
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawButton(transfer, TRANSFER_ACCENT)
        drawButton(settings, ACCENT)
        shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin(); font.color = TEXT
        font.draw(batch, "DÉPART", transfer.x + 10f, transfer.y + 29f)
        font.draw(batch, "FX", settings.x + 15f, settings.y + 29f)
        batch.end()
    }

    private fun drawButton(rect: Rectangle, accent: Color) {
        shapes.color = BUTTON; shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = accent; shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun cue(sound: SoundCue, kind: FeedbackKind, x: Float, y: Float): Boolean {
        PresentationController.play(services, sound, kind, x, y)
        return false
    }

    private fun meteorButton(): Rectangle { val p = safeTopLeft(); return Rectangle(p.first, p.second - 48f, 92f, 48f) }
    private fun robotsButton(): Rectangle { val p = meteorButton(); return Rectangle(p.x + p.width + 5f, p.y, 78f, 48f) }
    private fun strategyButton(): Rectangle { val p = robotsButton(); return Rectangle(p.x + p.width + 5f, p.y, 94f, 48f) }
    private fun missionsButton(): Rectangle { val p = strategyButton(); return Rectangle(p.x + p.width + 5f, p.y, 86f, 48f) }
    private fun archivesButton(): Rectangle { val p = missionsButton(); return Rectangle(p.x + p.width + 5f, p.y, 88f, 48f) }
    private fun settingsButton(): Rectangle { val p = safeTopRight(); return Rectangle(p.first - 48f, p.second - 48f, 48f, 48f) }
    private fun transferButton(): Rectangle { val p = settingsButton(); return Rectangle(p.x - 5f - 78f, p.y, 78f, 48f) }

    private fun safeTopLeft(): Pair<Float, Float> {
        val width = viewport.worldWidth; val height = viewport.worldHeight
        val sx = width / Gdx.graphics.width.coerceAtLeast(1); val sy = height / Gdx.graphics.height.coerceAtLeast(1)
        return Gdx.graphics.safeInsetLeft * sx + 8f to max(1f, height - Gdx.graphics.safeInsetTop * sy - 8f)
    }
    private fun safeTopRight(): Pair<Float, Float> {
        val width = viewport.worldWidth; val height = viewport.worldHeight
        val sx = width / Gdx.graphics.width.coerceAtLeast(1); val sy = height / Gdx.graphics.height.coerceAtLeast(1)
        return max(1f, width - Gdx.graphics.safeInsetRight * sx - 8f) to max(1f, height - Gdx.graphics.safeInsetTop * sy - 8f)
    }

    override fun dispose() { hide(); gameplay.dispose(); effects.dispose(); shapes.dispose(); batch.dispose(); font.dispose() }

    private companion object {
        val BUTTON = Color(.075f, .17f, .25f, .94f)
        val ACCENT = Color(.94f, .48f, .16f, 1f)
        val TRANSFER_ACCENT = Color(.42f, .80f, .96f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
    }
}
