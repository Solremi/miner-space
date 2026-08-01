package fr.solremi.minerspace.game.performance

import fr.solremi.minerspace.domain.presentation.VisualQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AdaptiveRuntimeBudgetTest {
    @BeforeEach
    fun reset() {
        AdaptiveRuntimeGovernor.resetForTests()
    }

    @Test
    fun `low memory profile caps high quality and memory use`() {
        val budget = RuntimePerformanceBudgets.forQuality(
            quality = VisualQuality.HIGH,
            lowMemoryDevice = true,
            adaptive = false,
        )

        assertTrue(budget.maxVisibleRobots <= 16)
        assertTrue(budget.assetMemoryBudgetBytes <= 128L * 1024L * 1024L)
    }

    @Test
    fun `sustained slow frames lower the selected tier`() {
        repeat(120) { AdaptiveRuntimeGovernor.record(32_000_000L) }

        val budget = RuntimePerformanceBudgets.forQuality(
            quality = VisualQuality.HIGH,
            lowMemoryDevice = false,
        )

        assertEquals(28, budget.maxVisibleRobots)
        assertEquals(1, budget.shaderPasses)
    }

    @Test
    fun `governor never raises low quality`() {
        repeat(720) { AdaptiveRuntimeGovernor.record(10_000_000L) }

        val budget = RuntimePerformanceBudgets.forQuality(
            quality = VisualQuality.LOW,
            lowMemoryDevice = false,
        )

        assertEquals(12, budget.maxVisibleRobots)
    }
}
