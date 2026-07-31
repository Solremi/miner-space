package fr.solremi.minerspace.game.presentation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import fr.solremi.minerspace.domain.presentation.ColorVisionMode
import fr.solremi.minerspace.domain.presentation.FeedbackKind
import fr.solremi.minerspace.domain.presentation.PresentationSettings
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
        drawLighting(width, height, time, settings, budget.shaderPasses)
        drawDust(width, height, time, budget.dustParticles, settings.highContrast)
        drawRobots(width, height, time, budget.robotUnits, settings.reducedMotion, settings.highContrast)
        drawMeteors(width, height, time, budget.meteorTrails, settings)
        drawFeedback(width, height, nowMillis, budget.sparkParticles, settings)
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawLighting(width: Float, height: Float, time: Float, settings: PresentationSettings, lightingPasses: Int) {
        val pulse = if (settings.reducedFlashes) .5f else .5f + .5f * MathUtils.sin(time * .35f)
        val baseAlpha = if (settings.highContrast) .018f else .035f
        shapes.color = Color(.55f, .16f, .06f, baseAlpha + pulse * if (settings.reducedFlashes) .008f else .025f)
        shapes.circle(width * .78f, height * .16f, width * .34f, if (settings.quality == VisualQuality.LOW) 24 else 40)
        shapes.color = Color(.10f, .55f, .62f, if (settings.highContrast) .055f else .025f)
        shapes.rect(0f, height * .46f, width, if (settings.highContrast) 4f else 3f)
        repeat(lightingPasses) { pass ->
            shapes.color = Color(.92f, .42f, .12f, if (settings.reducedFlashes) .008f else .018f)
            val y = height * (.18f + pass * .11f) + MathUtils.sin(time * .2f + pass) * 5f
            shapes.rect(0f, y, width, 2f)
        }
    }

    private fun drawDust(width: Float, height: Float, time: Float, count: Int, highContrast: Boolean) {
        repeat(count) { index ->
            val x = ((index * 97) % 997) / 997f * width + MathUtils.sin(time * .12f + index) * 7f
            val y = ((index * 53 + 17) % 541) / 541f * height
            shapes.color = if (highContrast) DUST_HIGH_CONTRAST else if (index % 3 == 0) DUST_LIGHT else DUST_DARK
            shapes.circle(x, y, if (index % 4 == 0) 1.8f else 1.1f, 8)
        }
    }

    private fun drawRobots(width: Float, height: Float, time: Float, count: Int, reduced: Boolean, highContrast: Boolean) {
        repeat(count) { index ->
            val column = index % 7
            val row = index / 7
            val x = width * .57f + column * 18f
            val baseY = height * .12f + row * 14f
            val y = baseY + if (reduced) 0f else MathUtils.sin(time * 1.4f + index * .7f) * 3f
            shapes.color = ROBOT_SHADOW; shapes.rect(x + 2f, baseY - 2f, 9f, 5f)
            shapes.color = if (highContrast) ROBOT_HIGH_CONTRAST else if (index % 4 == 0) ROBOT_ACCENT else ROBOT_BODY
            shapes.rect(x, y, 9f, 5f)
            shapes.color = if (highContrast) Color.WHITE else ROBOT_EYE; shapes.rect(x + 6f, y + 2f, 2f, 1f)
        }
    }

    private fun drawMeteors(width: Float, height: Float, time: Float, count: Int, settings: PresentationSettings) {
        repeat(count) { index ->
            val phase = (time * (.07f + index * .012f) + index * .19f) % 1f
            val x = width * (1.05f - phase)
            val y = height * (.72f + (index % 4) * .055f)
            shapes.color = if (settings.highContrast) Color(.72f,.88f,1f,.30f) else METEOR_TRAIL
            shapes.rectLine(x, y, x + 25f, y + 12f, if (settings.highContrast) 3f else 2f)
            shapes.color = if (settings.colorVisionMode == ColorVisionMode.MONOCHROME) Color.WHITE else METEOR_CORE
            shapes.circle(x, y, 3f, 10)
        }
    }

    private fun drawFeedback(width: Float, height: Float, nowMillis: Long, sparkBudget: Int, settings: PresentationSettings) {
        GameFeedbackBus.active(nowMillis).forEach { pulse ->
            val age = (nowMillis - pulse.startedAtMillis).coerceAtLeast(0L).toFloat() / pulse.durationMillis
            val remaining = (1f - age).coerceIn(0f, 1f)
            val x = pulse.normalizedX * width
            val y = pulse.normalizedY * height
            val radius = if (settings.reducedFlashes) 6f + age * 18f else 8f + age * 52f
            val alpha = remaining * PresentationController.engine().feedbackAlpha(settings)
            shapes.color = colorFor(pulse.kind, alpha, settings)
            shapes.circle(x, y, radius, if (settings.reducedFlashes) 14 else 24)
            val sparks = minOf(sparkBudget, if (pulse.kind in SPECIAL_FEEDBACK) 16 else 6)
            repeat(sparks) { index ->
                val angle = index * MathUtils.PI2 / sparks.coerceAtLeast(1)
                val distance = radius + 5f + index % 3 * 3f
                shapes.circle(x + MathUtils.cos(angle) * distance, y + MathUtils.sin(angle) * distance, if (settings.highContrast) 2.4f else 1.8f, 7)
            }
        }
    }

    private fun colorFor(kind: FeedbackKind, alpha: Float, settings: PresentationSettings): Color {
        val rgb = when (settings.colorVisionMode) {
            ColorVisionMode.DEFAULT -> when (kind) {
                FeedbackKind.INTERACTION -> floatArrayOf(.25f, .82f, .9f)
                FeedbackKind.SUCCESS, FeedbackKind.PRODUCTION -> floatArrayOf(.32f, .9f, .48f)
                FeedbackKind.ERROR -> floatArrayOf(1f, .23f, .18f)
                FeedbackKind.RARE -> floatArrayOf(.92f, .48f, 1f)
                FeedbackKind.SECTOR_OPEN -> floatArrayOf(1f, .64f, .18f)
                FeedbackKind.LAUNCH -> floatArrayOf(.45f, .72f, 1f)
            }
            ColorVisionMode.DEUTERANOPIA, ColorVisionMode.PROTANOPIA -> when (kind) {
                FeedbackKind.INTERACTION -> floatArrayOf(.34f, .70f, .95f)
                FeedbackKind.SUCCESS, FeedbackKind.PRODUCTION -> floatArrayOf(.95f, .78f, .18f)
                FeedbackKind.ERROR -> floatArrayOf(.80f, .35f, .10f)
                FeedbackKind.RARE -> floatArrayOf(.62f, .46f, .90f)
                FeedbackKind.SECTOR_OPEN -> floatArrayOf(.95f, .56f, .10f)
                FeedbackKind.LAUNCH -> floatArrayOf(.20f, .55f, .95f)
            }
            ColorVisionMode.TRITANOPIA -> when (kind) {
                FeedbackKind.INTERACTION -> floatArrayOf(.18f, .75f, .62f)
                FeedbackKind.SUCCESS, FeedbackKind.PRODUCTION -> floatArrayOf(.35f, .86f, .60f)
                FeedbackKind.ERROR -> floatArrayOf(.96f, .28f, .34f)
                FeedbackKind.RARE -> floatArrayOf(.88f, .42f, .58f)
                FeedbackKind.SECTOR_OPEN -> floatArrayOf(.92f, .68f, .22f)
                FeedbackKind.LAUNCH -> floatArrayOf(.42f, .78f, .70f)
            }
            ColorVisionMode.MONOCHROME -> when (kind) {
                FeedbackKind.INTERACTION -> floatArrayOf(.72f, .72f, .72f)
                FeedbackKind.SUCCESS, FeedbackKind.PRODUCTION -> floatArrayOf(.90f, .90f, .90f)
                FeedbackKind.ERROR -> floatArrayOf(.38f, .38f, .38f)
                FeedbackKind.RARE -> floatArrayOf(.82f, .82f, .82f)
                FeedbackKind.SECTOR_OPEN -> floatArrayOf(.64f, .64f, .64f)
                FeedbackKind.LAUNCH -> floatArrayOf(.76f, .76f, .76f)
            }
        }
        val adjustedAlpha = if (settings.highContrast) minOf(1f, alpha * 1.45f) else alpha
        return Color(rgb[0], rgb[1], rgb[2], adjustedAlpha)
    }

    fun dispose() = shapes.dispose()

    private companion object {
        val SPECIAL_FEEDBACK = setOf(FeedbackKind.RARE, FeedbackKind.SECTOR_OPEN, FeedbackKind.LAUNCH)
        val DUST_LIGHT = Color(.92f, .58f, .31f, .14f)
        val DUST_DARK = Color(.33f, .18f, .12f, .12f)
        val DUST_HIGH_CONTRAST = Color(.90f,.90f,.90f,.22f)
        val ROBOT_SHADOW = Color(.01f, .02f, .03f, .35f)
        val ROBOT_BODY = Color(.12f, .28f, .34f, .52f)
        val ROBOT_ACCENT = Color(.82f, .42f, .13f, .62f)
        val ROBOT_HIGH_CONTRAST = Color(.84f,.84f,.84f,.78f)
        val ROBOT_EYE = Color(.45f, .95f, 1f, .8f)
        val METEOR_TRAIL = Color(.82f, .36f, 1f, .16f)
        val METEOR_CORE = Color(1f, .74f, .28f, .5f)
    }
}
