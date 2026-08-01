package fr.solremi.minerspace.simulation.balance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LongHorizonBalanceSimulatorTest {
    private val simulator = LongHorizonBalanceSimulator()

    @Test
    fun `regular profiles finish Ferrum in the intended 25 to 40 day window`() {
        val results = IndustrialStrategy.entries.map {
            simulator.simulate(PlayerPattern.REGULAR, it)
        }

        results.forEach { result ->
            val day = result.completionDay
            assertNotNull(day)
            assertTrue(day!! in 25..40, "${result.strategy} completed on day $day")
            assertEquals(0, result.stalledDays)
        }
    }

    @Test
    fun `no strategy dominates the regular profile by more than fifteen percent`() {
        val days = IndustrialStrategy.entries.map {
            simulator.simulate(PlayerPattern.REGULAR, it).completionDay!!
        }
        val spread = (days.max() - days.min()).toDouble() / days.min()
        assertTrue(spread <= 0.15, "Strategy completion spread was $spread")
    }

    @Test
    fun `rewarded advertising is optional rather than progression blocking`() {
        val noAds = IndustrialStrategy.entries.map {
            simulator.simulate(PlayerPattern.REGULAR_NO_ADS, it)
        }
        noAds.forEach { result ->
            assertNotNull(result.completionDay, "${result.strategy} did not complete without ads")
            assertTrue(result.completionDay!! <= 45)
        }
    }

    @Test
    fun `simulation is deterministic`() {
        val first = simulator.simulate(PlayerPattern.ACTIVE, IndustrialStrategy.LOGISTICS)
        val second = simulator.simulate(PlayerPattern.ACTIVE, IndustrialStrategy.LOGISTICS)
        assertEquals(first, second)
    }
}
