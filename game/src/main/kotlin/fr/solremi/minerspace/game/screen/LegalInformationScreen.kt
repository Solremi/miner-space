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
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.game.presentation.PresentationController
import ktx.app.KtxScreen
import kotlin.math.max

class LegalInformationScreen(private val services: GameServices, private val onBack: () -> Unit) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val title = BitmapFont()
    private val font = BitmapFont()
    private var document = Document.PRIVACY
    private var page = 0
    private var current: Layout? = null
    private var lines = load(document)
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() { applyFontScale(); Gdx.input.inputProcessor = input }
    override fun hide() { if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null }
    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        val settings = PresentationController.current
        ScreenUtils.clear(if (settings.highContrast) Color.BLACK else BACKGROUND)
        viewport.apply(); camera.update()
        val layout = layout(); current = layout
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = if (settings.highContrast) Color(.08f,.08f,.08f,1f) else PANEL
        shapes.rect(layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height)
        layout.buttons.forEachIndexed { index, rect ->
            shapes.color = if (settings.highContrast) Color(.18f,.18f,.18f,1f) else BUTTON
            shapes.rect(rect.x, rect.y, rect.width, rect.height)
            shapes.color = when (index) { 0 -> BACK; 1 -> DOCUMENT; else -> PAGE }
            shapes.rect(rect.x, rect.y, rect.width, 4f)
        }
        shapes.end()
        batch.projectionMatrix = camera.combined; batch.begin()
        title.color = if (settings.highContrast) Color.WHITE else TEXT
        font.color = if (settings.highContrast) Color.WHITE else MUTED
        title.draw(batch, document.title, layout.panel.x + 14f, layout.panel.y + layout.panel.height - 18f)
        font.draw(batch, "Document local consultable hors connexion · page ${page + 1}/${pageCount()}", layout.panel.x + 14f, layout.panel.y + layout.panel.height - 42f)
        var y = layout.panel.y + layout.panel.height - 68f
        pageLines().forEach { line ->
            font.draw(batch, line, layout.panel.x + 14f, y)
            y -= 20f * PresentationController.engine().fontScale(settings)
        }
        listOf("RETOUR", "DOCUMENT", "PAGE").forEachIndexed { index, label ->
            val rect = layout.buttons[index]; font.draw(batch, label, rect.x + 12f, rect.y + 29f)
        }
        batch.end()
    }

    private fun touch(point: Vector2) {
        val layout = current ?: return
        when {
            layout.buttons[0].contains(point) -> onBack()
            layout.buttons[1].contains(point) -> { document = document.next(); page = 0; lines = load(document); services.haptic.impact() }
            layout.buttons[2].contains(point) -> { page = (page + 1) % pageCount(); services.haptic.impact() }
        }
    }

    private fun load(document: Document): List<String> {
        val text = services.content.readText(document.path) ?: "Document indisponible dans cette version."
        return text.lineSequence()
            .map { it.trim().removePrefix("#").removePrefix("-").trim() }
            .filter(String::isNotBlank)
            .flatMap(::wrap)
            .toList()
            .ifEmpty { listOf("Document vide") }
    }

    private fun wrap(line: String): Sequence<String> = sequence {
        val words = line.split(Regex("\\s+")); var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (candidate.length > 84 && current.isNotEmpty()) { yield(current); current = word } else current = candidate
        }
        if (current.isNotEmpty()) yield(current)
    }

    private fun pageLines(): List<String> { val count = linesPerPage(); return lines.drop(page * count).take(count) }
    private fun pageCount(): Int = max(1, (lines.size + linesPerPage() - 1) / linesPerPage())
    private fun linesPerPage(): Int = if (PresentationController.current.textScalePercent >= 130) 7 else 9
    private fun applyFontScale() {
        val scale = PresentationController.engine().fontScale(PresentationController.current)
        title.data.setScale(.78f * scale); font.data.setScale(.55f * scale)
    }

    private fun layout(): Layout {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        val gap = 6f; val buttonWidth = (right - left - gap * 2f) / 3f
        val buttons = (0..2).map { Rectangle(left + it * (buttonWidth + gap), bottom, buttonWidth, 48f) }
        return Layout(Rectangle(left, bottom + 56f, right - left, top - bottom - 56f), buttons)
    }

    override fun dispose() { hide(); shapes.dispose(); batch.dispose(); title.dispose(); font.dispose() }

    private enum class Document(val title: String, val path: String) {
        PRIVACY("POLITIQUE DE CONFIDENTIALITÉ", "legal/privacy-policy-fr.md"),
        LICENSES("LICENCES ET CRÉDITS", "legal/third-party-notices.md");
        fun next() = entries[(ordinal + 1) % entries.size]
    }
    private data class Layout(val panel: Rectangle, val buttons: List<Rectangle>)
    private companion object {
        val BACKGROUND = Color(.006f,.012f,.024f,1f); val PANEL = Color(.032f,.072f,.112f,1f)
        val BUTTON = Color(.06f,.14f,.20f,1f); val TEXT = Color(.94f,.97f,1f,1f); val MUTED = Color(.72f,.80f,.86f,1f)
        val BACK = Color(.46f,.60f,.70f,1f); val DOCUMENT = Color(.34f,.86f,.62f,1f); val PAGE = Color(.35f,.72f,.96f,1f)
    }
}
