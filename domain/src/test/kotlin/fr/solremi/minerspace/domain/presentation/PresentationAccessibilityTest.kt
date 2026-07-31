package fr.solremi.minerspace.domain.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresentationAccessibilityTest {
    private val engine = PresentationSettingsEngine()

    @Test fun `text scale is restricted to tested values`() {
        assertEquals(100, engine.normalize(PresentationSettings(textScalePercent = 101)).textScalePercent)
        assertEquals(115, engine.normalize(PresentationSettings(textScalePercent = 119)).textScalePercent)
        assertEquals(130, engine.normalize(PresentationSettings(textScalePercent = 500)).textScalePercent)
    }

    @Test fun `reduced flashes also reduces spark and shader budgets`() {
        val normal = engine.visualBudget(PresentationSettings(quality = VisualQuality.HIGH))
        val reduced = engine.visualBudget(PresentationSettings(quality = VisualQuality.HIGH, reducedFlashes = true))
        assertTrue(reduced.sparkParticles < normal.sparkParticles)
        assertEquals(0, reduced.shaderPasses)
        assertTrue(engine.feedbackAlpha(PresentationSettings(reducedFlashes = true)) < engine.feedbackAlpha(PresentationSettings()))
    }

    @Test fun `all color vision modes are reachable`() {
        var settings = PresentationSettings()
        val visited = linkedSetOf(settings.colorVisionMode)
        repeat(ColorVisionMode.entries.size - 1) {
            settings = engine.cycleColorVisionMode(settings)
            visited += settings.colorVisionMode
        }
        assertEquals(ColorVisionMode.entries.toSet(), visited)
    }
}
