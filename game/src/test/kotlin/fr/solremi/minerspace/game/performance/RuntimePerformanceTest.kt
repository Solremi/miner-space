package fr.solremi.minerspace.game.performance

import fr.solremi.minerspace.domain.presentation.VisualQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuntimePerformanceTest {
    @Test
    fun `low memory profile caps expensive budgets`() {
        val high = RuntimePerformanceBudgets.forQuality(VisualQuality.HIGH)
        val constrained = RuntimePerformanceBudgets.forQuality(
            VisualQuality.HIGH,
            lowMemoryDevice = true,
        )

        assertTrue(constrained.maxVisibleRobots < high.maxVisibleRobots)
        assertTrue(constrained.assetMemoryBudgetBytes < high.assetMemoryBudgetBytes)
        assertTrue(constrained.maxTextureEdge <= 1024)
        assertTrue(constrained.shaderPasses <= 1)
    }

    @Test
    fun `frame monitor reports bounded statistics`() {
        val monitor = FrameTimeMonitor(capacity = 30, slowFrameThresholdNanos = 20_000_000L)
        repeat(20) { monitor.record(16_000_000L) }
        repeat(10) { monitor.record(30_000_000L) }

        val snapshot = monitor.snapshot()
        assertEquals(30, snapshot.sampleCount)
        assertEquals(10, snapshot.slowFrameCount)
        assertTrue(snapshot.averageMillis in 20.0..21.0)
        assertEquals(30.0, snapshot.maximumMillis)
    }
}
