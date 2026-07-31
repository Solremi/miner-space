package fr.solremi.minerspace.game.presentation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import fr.solremi.minerspace.domain.presentation.FeedbackKind
import fr.solremi.minerspace.domain.presentation.VisualQuality

class FerrumVisualLayer {
    private val shapes = ShapeRenderer()

    fun draw(camera: OrthographicCamera, width: Float, height: Float, nowMillis: Long) {
        val settings = PresentationController.current
        val budget = PresentationController.engine().visualBudget(settings)
        val time = if (settings.reducedMotion) 0f else nowMillis / 1_000f
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        drawLighting(width, height, time, settings.quality, budget.shaderPasses)
        drawDust(width, height, time, budget.dustParticles)
        drawRobots(width, height, time, budget.robotUnits, settings.reducedMotion)
        drawMeteors(width, height, time, budget.meteorTrails)
        drawFeedback(width, height, nowMillis, budget.sparkParticles)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawLighting(width: Float, height: Float, time: Float, quality: VisualQuality, lightingPasses: Int) {
        val pulse = .5f + .5f * MathUtils.sin(time * .35f)
        shapes.color = Color(.55f, .16f, .06f, .035f + pulse * .025f)
        shapes.circle(width * .78f, height * .16f, width * .34f, if (quality == VisualQuality.LOW) 24 else 40)
        shapes.color = Color(.10f, .55f, .62f, .025f)
        shapes.rect(0f, height * .46f, width, 3f)
        repeat(lightingPasses) { pass ->
            shapes.color = Color(.92f, .42f, .12f, .018f)
            val y = height * (.18f + pass * .11f) + MathUtils.sin(time * .2f + pass) * 5f
            shapes.rect(0f, y, width, 2f)
        }
    }

    private fun drawDust(width: Float, height: Float, time: Float, count: Int) {
        repeat(count) { index ->
            val x = ((index * 97) % 997) / 997f * width + MathUtils.sin(time * .12f + index) * 7f
            val y = ((index * 53 + 17) % 541) / 541f * height
            shapes.color = if (index % 3 == 0) DUST_LIGHT else DUST_DARK
            shapes.circle(x, y, if (index % 4 == 0) 1.8f else 1.1f, 8)
        }
    }

    private fun drawRobots(width: Float, height: Float, time: Float, count: Int, reduced: Boolean) {
        repeat(count) { index ->
            val column = index % 7
            val row = index / 7
            val x = width * .57f + column * 18f
            val baseY = height * .12f + row * 14f
            val y = baseY + if (reduced) 0f else MathUtils.sin(time * 1.4f + index * .7f) * 3f
            shapes.color = ROBOT_SHADOW; shapes.rect(x + 2f, baseY - 2f, 9f, 5f)
            shapes.color = if (index % 4 == 0) ROBOT_ACCENT else ROBOT_BODY; shapes.rect(x, y, 9f, 5f)
            shapes.color = ROBOT_EYE; shapes.rect(x + 6f, y + 2f, 2f, 1f)
        }
    }

    private fun drawMeteors(width: Float, height: Float, time: Float, count: Int) {
        repeat(count) { index ->
            val phase = (time * (.07f + index * .012f) + index * .19f) % 1f
            val x = width * (1.05f - phase)
            val y = height * (.72f + (index % 4) * .055f)
            shapes.color = METEOR_TRAIL; shapes.rectLine(x, y, x + 25f, y + 12f, 2f)
            shapes.color = METEOR_CORE; shapes.circle(x, y, 3f, 10)
        }
    }

    private fun drawFeedback(width: Float, height: Float, nowMillis: Long, sparkBudget: Int) {
        GameFeedbackBus.active(nowMillis).forEach { pulse ->
            val age = (nowMillis - pulse.startedAtMillis).coerceAtLeast(0L).toFloat() / pulse.durationMillis
            val remaining = (1f - age).coerceIn(0f, 1f)
            val x = pulse.normalizedX * width
            val y = pulse.normalizedY * height
            val radius = 8f + age * 52f
            shapes.color = colorFor(pulse.kind, remaining * .55f)
            shapes.circle(x, y, radius, 24)
            val sparks = minOf(sparkBudget, if (pulse.kind in SPECIAL_FEEDBACK) 16 else 6)
            repeat(sparks) { index ->
                val angle = index * MathUtils.PI2 / sparks.coerceAtLeast(1)
                val distance = radius + 5f + index % 3 * 3f
                shapes.circle(x + MathUtils.cos(angle) * distance, y + MathUtils.sin(angle) * distance, 1.8f, 7)
            }
        }
    }

    private fun colorFor(kind: FeedbackKind, alpha: Float): Color = when (kind) {
        FeedbackKind.INTERACTION -> Color(.25f, .82f, .9f, alpha)
        FeedbackKind.SUCCESS, FeedbackKind.PRODUCTION -> Color(.32f, .9f, .48f, alpha)
        FeedbackKind.ERROR -> Color(1f, .23f, .18f, alpha)
        FeedbackKind.RARE -> Color(.92f, .48f, 1f, alpha)
        FeedbackKind.SECTOR_OPEN -> Color(1f, .64f, .18f, alpha)
        FeedbackKind.LAUNCH -> Color(.45f, .72f, 1f, alpha)
    }

    fun dispose() = shapes.dispose()

    private companion object {
        val SPECIAL_FEEDBACK = setOf(FeedbackKind.RARE, FeedbackKind.SECTOR_OPEN, FeedbackKind.LAUNCH)
        val DUST_LIGHT = Color(.92f, .58f, .31f, .14f)
        val DUST_DARK = Color(.33f, .18f, .12f, .12f)
        val ROBOT_SHADOW = Color(.01f, .02f, .03f, .35f)
        val ROBOT_BODY = Color(.12f, .28f, .34f, .52f)
        val ROBOT_ACCENT = Color(.82f, .42f, .13f, .62f)
        val ROBOT_EYE = Color(.45f, .95f, 1f, .8f)
        val METEOR_TRAIL = Color(.82f, .36f, 1f, .16f)
        val METEOR_CORE = Color(1f, .74f, .28f, .5f)
    }
}
