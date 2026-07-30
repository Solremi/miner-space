package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.domain.services.GameServices
import ktx.app.KtxScreen

class EmptyPlanetScreen(
    private val services: GameServices,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply {
        data.setScale(1.12f)
        color = Color(0.88f, 0.94f, 1f, 1f)
    }

    override fun show() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)

        viewport.apply()
        val worldWidth = viewport.worldWidth
        val worldHeight = viewport.worldHeight
        val safePadding = (worldHeight * 0.045f).coerceAtLeast(14f)

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = SKY
        shapes.rect(0f, 0f, worldWidth, worldHeight)

        shapes.color = STAR
        STAR_POSITIONS.forEach { (xRatio, yRatio) ->
            shapes.circle(worldWidth * xRatio, worldHeight * yRatio, 1.4f, 8)
        }

        val baseWidth = (worldWidth * 0.26f).coerceIn(170f, 260f)
        val baseHeight = (worldHeight * 0.18f).coerceIn(54f, 86f)
        val baseX = (worldWidth - baseWidth) / 2f
        val baseY = worldHeight * 0.24f

        shapes.color = GROUND
        shapes.rect(0f, 0f, worldWidth, worldHeight * 0.28f)
        shapes.color = BASE_SHADOW
        shapes.rect(baseX + 8f, baseY - 7f, baseWidth, baseHeight)
        shapes.color = BASE
        shapes.rect(baseX, baseY, baseWidth, baseHeight)
        shapes.color = ACCENT
        shapes.rect(baseX + baseWidth * 0.12f, baseY + baseHeight * 0.72f, baseWidth * 0.76f, 5f)

        shapes.color = HUD
        shapes.rect(safePadding, worldHeight - safePadding - 50f, worldWidth - safePadding * 2f, 50f)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.draw(batch, "MINER SPACE", safePadding + 16f, worldHeight - safePadding - 18f)
        font.draw(
            batch,
            "Fondation prête · étape 0",
            safePadding + 16f,
            worldHeight - safePadding - 39f,
        )
        font.draw(
            batch,
            "Base temporaire",
            baseX + 18f,
            baseY + baseHeight * 0.52f,
        )
        font.draw(
            batch,
            "UTC ${services.clock.nowEpochMillis()}",
            worldWidth - safePadding - 155f,
            worldHeight - safePadding - 29f,
        )
        batch.end()
    }

    override fun dispose() {
        shapes.dispose()
        batch.dispose()
        font.dispose()
    }

    private companion object {
        const val MIN_WORLD_WIDTH = 640f
        const val MIN_WORLD_HEIGHT = 320f

        val BACKGROUND = Color(0.01f, 0.02f, 0.06f, 1f)
        val SKY = Color(0.025f, 0.045f, 0.105f, 1f)
        val GROUND = Color(0.08f, 0.10f, 0.16f, 1f)
        val BASE_SHADOW = Color(0.01f, 0.01f, 0.02f, 0.50f)
        val BASE = Color(0.18f, 0.24f, 0.34f, 1f)
        val ACCENT = Color(0.20f, 0.82f, 0.88f, 1f)
        val HUD = Color(0.035f, 0.07f, 0.13f, 0.96f)
        val STAR = Color(0.75f, 0.86f, 1f, 0.75f)

        val STAR_POSITIONS = listOf(
            0.08f to 0.78f,
            0.14f to 0.56f,
            0.22f to 0.88f,
            0.31f to 0.68f,
            0.39f to 0.83f,
            0.47f to 0.61f,
            0.56f to 0.91f,
            0.66f to 0.73f,
            0.74f to 0.86f,
            0.83f to 0.59f,
            0.92f to 0.80f,
        )
    }
}
