package fr.solremi.minerspace.game.ferrum.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import fr.solremi.minerspace.game.ui.FerrumPlayerHudLayout
import fr.solremi.minerspace.game.ui.FerrumPrimaryDestination
import fr.solremi.minerspace.game.ui.UiText

class FerrumHudRenderer {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val titleFont = BitmapFont().apply { data.setScale(0.76f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.57f) }

    fun render(
        camera: OrthographicCamera,
        layout: FerrumPlayerHudLayout,
        model: FerrumHudModel,
    ) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL
        shapes.rect(layout.status.x, layout.status.y, layout.status.width, layout.status.height)
        layout.primaryNavigation.forEachIndexed { index, rect ->
            val active = FerrumPrimaryDestination.entries[index] == FerrumPrimaryDestination.MENU && model.menuOpen
            button(rect, true, if (active) ACTION_ACCENT else NAV_ACCENT)
        }
        button(layout.recipe, model.recipeEnabled, RECIPE_ACCENT)
        button(layout.action, model.actionEnabled, ACTION_ACCENT)
        button(layout.task, model.taskEnabled, TASK_ACCENT)
        button(layout.utility, true, NAV_ACCENT)
        layout.secondaryMenu.forEach { button(it, true, MENU_ACCENT) }
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = TEXT
        smallFont.color = MUTED
        titleFont.draw(batch, model.title, layout.top.x + 10f, layout.top.y + 40f)
        smallFont.draw(batch, UiText.ellipsis(model.subtitle, 52), layout.top.x + 10f, layout.top.y + 16f)
        layout.primaryNavigation.forEachIndexed { index, rect -> label(rect, model.primaryLabels[index]) }
        titleFont.draw(batch, UiText.ellipsis(model.adviceHeading, 48), layout.status.x + 8f, layout.status.y + 34f)
        smallFont.draw(batch, UiText.ellipsis(model.adviceDetail, 68), layout.status.x + 8f, layout.status.y + 13f)
        label(layout.recipe, model.recipeLabel)
        label(layout.action, model.actionLabel)
        label(layout.task, model.taskLabel)
        label(layout.utility, model.utilityLabel)
        layout.secondaryMenu.forEachIndexed { index, rect -> label(rect, model.secondaryLabels[index]) }
        smallFont.color = MUTED
        smallFont.draw(
            batch,
            UiText.ellipsis(model.footer, 100),
            layout.safeArea.left,
            layout.status.y + layout.status.height + 13f,
        )
        batch.end()
    }

    fun dispose() {
        shapes.dispose()
        batch.dispose()
        titleFont.dispose()
        smallFont.dispose()
    }

    private fun button(rect: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) accent else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun label(rect: Rectangle, value: String) {
        smallFont.color = TEXT
        smallFont.draw(batch, value, rect.x + 7f, rect.y + 29f)
    }

    private companion object {
        val TOP = Color(0.020f, 0.047f, 0.085f, 0.97f)
        val PANEL = Color(0.030f, 0.070f, 0.120f, 0.97f)
        val BUTTON = Color(0.065f, 0.145f, 0.215f, 0.98f)
        val DISABLED = Color(0.030f, 0.050f, 0.072f, 0.98f)
        val GRID = Color(0.14f, 0.19f, 0.24f, 1f)
        val NAV_ACCENT = Color(0.30f, 0.72f, 0.96f, 1f)
        val MENU_ACCENT = Color(0.42f, 0.58f, 0.94f, 1f)
        val RECIPE_ACCENT = Color(0.72f, 0.46f, 0.96f, 1f)
        val ACTION_ACCENT = Color(0.28f, 0.88f, 0.66f, 1f)
        val TASK_ACCENT = Color(0.96f, 0.62f, 0.22f, 1f)
        val TEXT = Color(0.94f, 0.97f, 1f, 1f)
        val MUTED = Color(0.62f, 0.72f, 0.82f, 1f)
    }
}
