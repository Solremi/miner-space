package fr.solremi.minerspace.domain.presentation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresentationSettingsEngineTest {
    private val engine = PresentationSettingsEngine()

    @Test
    fun `low quality keeps the smallest deterministic budget`() {
        val low = engine.visualBudget(PresentationSettings(quality = VisualQuality.LOW))
        val medium = engine.visualBudget(PresentationSettings(quality = VisualQuality.MEDIUM))
        val high = engine.visualBudget(PresentationSettings(quality = VisualQuality.HIGH))
        assertTrue(low.dustParticles < medium.dustParticles)
        assertTrue(medium.dustParticles < high.dustParticles)
        assertEquals(0, low.shaderPasses)
    }

    @Test
    fun `reduced motion caps animated elements without disabling feedback`() {
        val budget = engine.visualBudget(PresentationSettings(quality = VisualQuality.HIGH, reducedMotion = true))
        assertTrue(budget.dustParticles <= 6)
        assertTrue(budget.robotUnits <= 10)
        assertTrue(budget.sparkParticles > 0)
    }

    @Test
    fun `volume and effects are normalized`() {
        val normalized = engine.normalize(PresentationSettings(masterVolumePercent = 180))
        assertEquals(100, normalized.masterVolumePercent)
        assertEquals(0f, engine.normalizedVolume(PresentationSettings(masterVolumePercent = -20)))
        assertEquals(VisualBudget(0, 8, 0, 0, 0), engine.visualBudget(PresentationSettings(effectsEnabled = false)))
    }
}
