package fr.solremi.minerspace.simulation.frontier

import fr.solremi.minerspace.data.frontier.FrontierContentFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FrontierGenerationValidatorTest {
    @Test
    fun `ten thousand worlds remain possible and non repetitive`() {
        val report = FrontierGenerationValidator(FrontierContentFactory.create()).validate(98_765_432L)
        assertEquals(10_000, report.generatedWorldCount)
        assertEquals(0, report.invalidWorldCount)
        assertEquals(0, report.immediateRepeatCount)
        assertEquals(2, report.minEstimatedDays)
        assertEquals(7, report.maxEstimatedDays)
    }
}
