package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.game.assets.AssetGroup
import fr.solremi.minerspace.game.assets.GameAssetRuntime
import ktx.app.KtxScreen

class AssetGroupLoadingScreen(
    private val runtime: GameAssetRuntime,
    private val group: AssetGroup,
    private val onReady: () -> Unit,
    private val onFailure: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.75f) }
    private var accepted = false
    private var completed = false

    override fun show() {
        accepted = runtime.acquire(group)
        if (!accepted) onFailure()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        if (!accepted || completed) return
        val ready = runtime.update()
        draw(runtime.progress())
        if (ready) {
            completed = true
            Gdx.app.postRunnable(onReady)
        }
    }

    private fun draw(progress: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        val width = viewport.worldWidth.coerceAtMost(620f)
        val x = (viewport.worldWidth - width) / 2f
        val y = viewport.worldHeight / 2f - 10f
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TRACK
        shapes.rect(x, y, width, 20f)
        shapes.color = ACCENT
        shapes.rect(x, y, width * progress.coerceIn(0f, 1f), 20f)
        shapes.end()
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "CHARGEMENT ${group.name} · ${(progress * 100f).toInt()} %", x, y + 45f)
        batch.end()
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }

    private companion object {
        val BACKGROUND = Color(.006f, .01f, .025f, 1f)
        val TRACK = Color(.04f, .08f, .12f, 1f)
        val ACCENT = Color(.24f, .78f, .92f, 1f)
        val TEXT = Color(.94f, .97f, 1f, 1f)
    }
}
