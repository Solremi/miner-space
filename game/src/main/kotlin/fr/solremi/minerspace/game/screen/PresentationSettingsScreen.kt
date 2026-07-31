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
import fr.solremi.minerspace.domain.presentation.FeedbackKind
import fr.solremi.minerspace.domain.presentation.PresentationSettings
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SoundCue
import fr.solremi.minerspace.game.presentation.FerrumVisualLayer
import fr.solremi.minerspace.game.presentation.PresentationController
import ktx.app.KtxScreen
import kotlin.math.max

class PresentationSettingsScreen(private val services: GameServices, private val onBack: () -> Unit) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.72f) }
    private val small = BitmapFont().apply { data.setScale(.58f) }
    private val effects = FerrumVisualLayer()
    private var settings = PresentationSettings()
    private var message = "Profil appliqué"
    private var current: Layout? = null
    private var cueIndex = 0
    private val input = object : InputAdapter() {
        override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(x.toFloat(), y.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() { settings = PresentationController.loadAndApply(services); Gdx.input.inputProcessor = input }
    override fun hide() { if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null }
    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply(); camera.update()
        effects.draw(camera, viewport.worldWidth, viewport.worldHeight, services.clock.monotonicMillis())
        val l = layout(); current = l
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP; shapes.rect(l.top.x, l.top.y, l.top.width, l.top.height)
        l.rows.forEach { row -> shapes.color = PANEL; shapes.rect(row.x, row.y, row.width, row.height) }
        button(l.test, TEST); button(l.back, BACK)
        shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin(); font.color = TEXT; small.color = MUTED
        font.draw(batch, "FERRUM DELTA · PRÉSENTATION", l.top.x + 12f, l.top.y + l.top.height - 14f)
        small.draw(batch, "Qualité, effets, vibration, son et volume", l.top.x + 12f, l.top.y + 15f)
        val labels = listOf(
            "QUALITÉ · ${settings.quality.name}",
            "EFFETS · ${onOff(settings.effectsEnabled)}",
            "ANIMATIONS · ${if (settings.reducedMotion) "RÉDUITES" else "COMPLÈTES"}",
            "VIBRATION · ${onOff(settings.vibrationEnabled)}",
            "SONS · ${onOff(settings.soundEnabled)}",
            "VOLUME · ${settings.masterVolumePercent}%",
        )
        l.rows.forEachIndexed { index, row -> font.draw(batch, labels[index], row.x + 12f, row.y + 30f) }
        small.draw(batch, message, l.message.x, l.message.y + 16f)
        small.draw(batch, "APERÇU AUDIO", l.test.x + 10f, l.test.y + 29f)
        small.draw(batch, "RETOUR", l.back.x + 18f, l.back.y + 29f)
        batch.end()
    }

    private fun touch(point: Vector2) {
        val l = current ?: return
        l.rows.forEachIndexed { index, row ->
            if (!row.contains(point)) return@forEachIndexed
            settings = when (index) {
                0 -> PresentationController.engine().cycleQuality(settings)
                1 -> settings.copy(effectsEnabled = !settings.effectsEnabled)
                2 -> settings.copy(reducedMotion = !settings.reducedMotion)
                3 -> settings.copy(vibrationEnabled = !settings.vibrationEnabled)
                4 -> settings.copy(soundEnabled = !settings.soundEnabled)
                else -> PresentationController.engine().cycleVolume(settings)
            }
            message = if (PresentationController.update(services, settings)) "Réglage enregistré" else "Réglage appliqué"
            services.haptic.impact(); return
        }
        when {
            l.test.contains(point) -> preview(point)
            l.back.contains(point) -> onBack()
        }
    }

    private fun preview(point: Vector2) {
        val cue = SoundCue.entries[cueIndex % SoundCue.entries.size]
        cueIndex = (cueIndex + 1) % SoundCue.entries.size
        val kind = when (cue) {
            SoundCue.INTERACTION -> FeedbackKind.INTERACTION
            SoundCue.PRODUCTION_COMPLETE -> FeedbackKind.PRODUCTION
            SoundCue.RARITY -> FeedbackKind.RARE
            SoundCue.ERROR -> FeedbackKind.ERROR
            SoundCue.SECTOR_OPEN -> FeedbackKind.SECTOR_OPEN
            SoundCue.LAUNCH -> FeedbackKind.LAUNCH
        }
        PresentationController.play(services, cue, kind, point.x / viewport.worldWidth, point.y / viewport.worldHeight)
        message = "Aperçu : ${cue.name}"
    }

    private fun layout(): Layout {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        val topBar = Rectangle(left, top - 52f, right - left, 52f)
        val back = Rectangle(right - 88f, bottom, 88f, 48f)
        val test = Rectangle(left, bottom, 126f, 48f)
        val message = Rectangle(test.x + test.width + 8f, bottom, back.x - test.x - test.width - 16f, 48f)
        val gap = 6f; val columnGap = 8f
        val rowTop = topBar.y - 8f; val listBottom = bottom + 56f
        val rowHeight = ((rowTop - listBottom - gap * 2f) / 3f).coerceAtLeast(48f)
        val columnWidth = (right - left - columnGap) / 2f
        val rightX = left + columnWidth + columnGap
        val rows = (0 until 6).map { index ->
            val column = index / 3
            val row = index % 3
            Rectangle(if (column == 0) left else rightX, rowTop - (row + 1) * rowHeight - row * gap, columnWidth, rowHeight)
        }
        return Layout(topBar, rows, message, test, back)
    }

    private fun button(rect: Rectangle, accent: Color) {
        shapes.color = BUTTON; shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = accent; shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun onOff(value: Boolean) = if (value) "ACTIVÉ" else "DÉSACTIVÉ"
    override fun dispose() { hide(); effects.dispose(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }

    private data class Layout(val top: Rectangle, val rows: List<Rectangle>, val message: Rectangle, val test: Rectangle, val back: Rectangle)
    private companion object {
        val BACKGROUND = Color(.012f, .018f, .026f, 1f)
        val TOP = Color(.035f, .09f, .13f, .96f)
        val PANEL = Color(.055f, .115f, .15f, .94f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
        val MUTED = Color(.64f, .73f, .78f, 1f)
        val TEST = Color(.92f, .48f, 1f, 1f)
        val BACK = Color(.42f, .58f, .66f, 1f)
    }
}
