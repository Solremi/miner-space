package fr.solremi.minerspace.domain.economy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProductionFormulaTest {
    @Test
    fun `applies official multiplier categories with a single final floor`() {
        val result = ProductionFormula.floor(
            base = 100L,
            multipliers = ProductionMultipliers(
                robot = 1_100_000L,
                modules = 1_200_000L,
                synergies = 1_050_000L,
                specialization = 900_000L,
                technologies = 1_350_000L,
                planet = 1_100_000L,
                event = 800_000L,
                prestige = 1_250_000L,
            ),
        )

        assertEquals(185L, result)
    }
}
