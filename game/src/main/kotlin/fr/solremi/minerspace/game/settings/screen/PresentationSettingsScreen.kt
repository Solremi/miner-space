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

class PresentationSettingsScreen(
    private val services: GameServices,
    private val onLegalRequested: () -> Unit,
    private val onBack: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val small = BitmapFont()
    private val effects = FerrumVisualLayer()
    private var settings = PresentationSettings()
    private var page = Page.PERFORMANCE
    private var message = "Profil appliqué"
    private var current: Layout? = null
    private var cueIndex = 0
    private val input = object : InputAdapter() {
        override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(x.toFloat(), y.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() { settings = PresentationController.loadAndApply(services); applyFontScale(); Gdx.input.inputProcessor = input }
    override fun hide() { if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null }
    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(background())
        viewport.apply(); camera.update()
        effects.draw(camera, viewport.worldWidth, viewport.worldHeight, services.clock.monotonicMillis())
        val l = layout(); current = l
        shapes.projectionMatrix = camera.combined; shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = topColor(); shapes.rect(l.top.x, l.top.y, l.top.width, l.top.height)
        l.rows.forEach { row -> shapes.color = panelColor(); shapes.rect(row.x, row.y, row.width, row.height) }
        button(l.preview, PREVIEW); button(l.page, PAGE); button(l.legal, LEGAL); button(l.back, BACK)
        shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin(); font.color = textColor(); small.color = mutedColor()
        font.draw(batch, "MINER SPACE · ACCESSIBILITÉ ET PRÉSENTATION", l.top.x + 12f, l.top.y + l.top.height - 14f)
        small.draw(batch, page.subtitle, l.top.x + 12f, l.top.y + 15f)
        labels().forEachIndexed { index, label -> font.draw(batch, label, l.rows[index].x + 12f, l.rows[index].y + 30f) }
        small.draw(batch, message, l.message.x, l.message.y + 17f)
        listOf("APERÇU", page.buttonLabel, "LÉGAL", "RETOUR").forEachIndexed { index, label ->
            val rect = listOf(l.preview, l.page, l.legal, l.back)[index]
            small.color = textColor(); small.draw(batch, label, rect.x + 8f, rect.y + 29f)
        }
        batch.end()
    }

    private fun labels(): List<String> = when (page) {
        Page.PERFORMANCE -> listOf(
            "QUALITÉ · ${settings.quality.name}",
            "EFFETS · ${onOff(settings.effectsEnabled)}",
            "ANIMATIONS · ${if (settings.reducedMotion) "RÉDUITES" else "COMPLÈTES"}",
            "VIBRATION · ${onOff(settings.vibrationEnabled)}",
            "SONS · ${onOff(settings.soundEnabled)}",
            "VOLUME · ${settings.masterVolumePercent}%",
        )
        Page.ACCESSIBILITY -> listOf(
            "TEXTE · ${settings.textScalePercent}%",
            "CONTRASTE · ${if (settings.highContrast) "ÉLEVÉ" else "STANDARD"}",
            "COULEURS · ${settings.colorVisionMode.name}",
            "FLASHES · ${if (settings.reducedFlashes) "RÉDUITS" else "COMPLETS"}",
            "ANIMATIONS · ${if (settings.reducedMotion) "RÉDUITES" else "COMPLÈTES"}",
            "VIBRATION · ${onOff(settings.vibrationEnabled)}",
        )
    }

    private fun touch(point: Vector2) {
        val l = current ?: return
        l.rows.forEachIndexed { index, row ->
            if (!row.contains(point)) return@forEachIndexed
            settings = when (page) {
                Page.PERFORMANCE -> when (index) {
                    0 -> PresentationController.engine().cycleQuality(settings)
                    1 -> settings.copy(effectsEnabled = !settings.effectsEnabled)
                    2 -> settings.copy(reducedMotion = !settings.reducedMotion)
                    3 -> settings.copy(vibrationEnabled = !settings.vibrationEnabled)
                    4 -> settings.copy(soundEnabled = !settings.soundEnabled)
                    else -> PresentationController.engine().cycleVolume(settings)
                }
                Page.ACCESSIBILITY -> when (index) {
                    0 -> PresentationController.engine().cycleTextScale(settings)
                    1 -> settings.copy(highContrast = !settings.highContrast)
                    2 -> PresentationController.engine().cycleColorVisionMode(settings)
                    3 -> settings.copy(reducedFlashes = !settings.reducedFlashes)
                    4 -> settings.copy(reducedMotion = !settings.reducedMotion)
                    else -> settings.copy(vibrationEnabled = !settings.vibrationEnabled)
                }
            }
            message = if (PresentationController.update(services, settings)) "Réglage enregistré" else "Réglage appliqué"
            applyFontScale(); services.haptic.impact(); return
        }
        when {
            l.preview.contains(point) -> preview(point)
            l.page.contains(point) -> { page = page.next(); message = page.subtitle; services.haptic.impact() }
            l.legal.contains(point) -> onLegalRequested()
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

    private fun applyFontScale() {
        val scale = PresentationController.engine().fontScale(settings)
        font.data.setScale(.72f * scale)
        small.data.setScale(.58f * scale)
    }

    private fun layout(): Layout {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        val topBar = Rectangle(left, top - 52f, right - left, 52f)
        val footerGap = 6f; val footerWidth = (right - left - footerGap * 3f) / 4f
        val footer = (0..3).map { Rectangle(left + it * (footerWidth + footerGap), bottom, footerWidth, 48f) }
        val message = Rectangle(left, bottom + 52f, right - left, 24f)
        val gap = 6f; val columnGap = 8f
        val rowTop = topBar.y - 8f; val listBottom = message.y + message.height + 4f
        val rowHeight = ((rowTop - listBottom - gap * 2f) / 3f).coerceAtLeast(48f)
        val columnWidth = (right - left - columnGap) / 2f
        val rightX = left + columnWidth + columnGap
        val rows = (0 until 6).map { index ->
            val column = index / 3; val row = index % 3
            Rectangle(if (column == 0) left else rightX, rowTop - (row + 1) * rowHeight - row * gap, columnWidth, rowHeight)
        }
        return Layout(topBar, rows, message, footer[0], footer[1], footer[2], footer[3])
    }

    private fun button(rect: Rectangle, accent: Color) {
        shapes.color = buttonColor(); shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = accent; shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun background() = if (settings.highContrast) Color.BLACK else BACKGROUND
    private fun topColor() = if (settings.highContrast) Color(.02f, .02f, .02f, 1f) else TOP
    private fun panelColor() = if (settings.highContrast) Color(.10f, .10f, .10f, 1f) else PANEL
    private fun buttonColor() = if (settings.highContrast) Color(.16f, .16f, .16f, 1f) else BUTTON
    private fun textColor() = if (settings.highContrast) Color.WHITE else TEXT
    private fun mutedColor() = if (settings.highContrast) Color(.82f, .82f, .82f, 1f) else MUTED
    private fun onOff(value: Boolean) = if (value) "ACTIVÉ" else "DÉSACTIVÉ"
    override fun dispose() { hide(); effects.dispose(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }

    private enum class Page(val subtitle: String, val buttonLabel: String) {
        PERFORMANCE("Qualité, effets, vibration, son et volume", "ACCESSIBILITÉ"),
        ACCESSIBILITY("Texte 100–130 %, contraste, couleurs, mouvements et flashes", "PRÉSENTATION");
        fun next() = entries[(ordinal + 1) % entries.size]
    }
    private data class Layout(
        val top: Rectangle,
        val rows: List<Rectangle>,
        val message: Rectangle,
        val preview: Rectangle,
        val page: Rectangle,
        val legal: Rectangle,
        val back: Rectangle,
    )
    private companion object {
        val BACKGROUND = Color(.012f, .018f, .026f, 1f)
        val TOP = Color(.035f, .09f, .13f, .96f)
        val PANEL = Color(.055f, .115f, .15f, .94f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
        val MUTED = Color(.64f, .73f, .78f, 1f)
        val PREVIEW = Color(.92f, .48f, 1f, 1f)
        val PAGE = Color(.22f, .78f, .92f, 1f)
        val LEGAL = Color(.34f, .84f, .58f, 1f)
        val BACK = Color(.42f, .58f, .66f, 1f)
    }
}
