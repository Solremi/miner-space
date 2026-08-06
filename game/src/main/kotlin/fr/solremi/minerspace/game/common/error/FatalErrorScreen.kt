package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.app.KtxScreen

class FatalErrorScreen(
    private val title: String,
    private val details: String,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, camera)
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply {
        data.setScale(1.2f)
        color = Color.WHITE
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.12f, 0.018f, 0.028f, 1f)
        viewport.apply()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.draw(batch, title, 32f, viewport.worldHeight - 44f)
        font.data.setScale(0.9f)
        font.draw(
            batch,
            details.take(MAX_DETAILS_LENGTH),
            32f,
            viewport.worldHeight - 84f,
            viewport.worldWidth - 64f,
            1,
            true,
        )
        font.data.setScale(1.2f)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        font.dispose()
    }

    private companion object {
        const val MAX_DETAILS_LENGTH = 260
    }
}
