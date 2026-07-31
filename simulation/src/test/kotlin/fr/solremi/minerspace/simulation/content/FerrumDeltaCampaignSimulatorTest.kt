package fr.solremi.minerspace.simulation.content

import fr.solremi.minerspace.data.content.FerrumDeltaContentFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FerrumDeltaCampaignSimulatorTest {
    private val content = FerrumDeltaContentFactory.create()
    private val simulator = FerrumDeltaCampaignSimulator(content)

    @Test
    fun `published profiles finish in their target windows`() {
        val active = simulator.simulate("VERY_ACTIVE")
        val regular = simulator.simulate("REGULAR")
        val casual = simulator.simulate("CASUAL")

        assertEquals(22, active.completionDays)
        assertEquals(32, regular.completionDays)
        assertEquals(50, casual.completionDays)
        assertTrue(simulator.isWithinPublishedRange(active))
        assertTrue(simulator.isWithinPublishedRange(regular))
        assertTrue(simulator.isWithinPublishedRange(casual))
    }

    @Test
    fun `very active profile cannot finish in a few days without ads`() {
        assertTrue(simulator.simulate("VERY_ACTIVE").completionDays >= 18)
        assertEquals(18, simulator.simulate("VERY_ACTIVE", advertisingEnabled = true).completionDays)
    }

    @Test
    fun `every profile reaches all sectors and guaranteed sources`() {
        content.playerProfiles.forEach { profile ->
            val result = simulator.simulate(profile.id)
            assertEquals(content.sectors.mapTo(linkedSetOf()) { it.id }, result.unlockedSectorIds)
            assertTrue(result.blockedSectorIds.isEmpty())
            assertEquals(
                content.resources.filter { it.mandatory }.mapTo(linkedSetOf()) { it.guaranteedSourceId },
                result.mandatoryResourceSourceIds,
            )
        }
    }
}
