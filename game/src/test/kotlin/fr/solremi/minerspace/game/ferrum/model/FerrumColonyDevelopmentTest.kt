package fr.solremi.minerspace.game.ferrum.model

import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FerrumColonyDevelopmentTest {
    @Test
    fun `empty colony remains an outpost`() {
        assertEquals(FerrumColonyStage.OUTPOST, development().stage)
    }

    @Test
    fun `refined materials reveal industrial infrastructure`() {
        assertEquals(FerrumColonyStage.INDUSTRIAL, development(refinedIron = 5L).stage)
    }

    @Test
    fun `one installed technology reveals the automated network`() {
        assertEquals(
            FerrumColonyStage.NETWORKED,
            development(technologies = setOf(FerrumIds.TECH_EXTRACTION)).stage,
        )
    }

    @Test
    fun `two installed technologies reveal the orbital chantier`() {
        assertEquals(
            FerrumColonyStage.ORBITAL,
            development(
                technologies = setOf(
                    FerrumIds.TECH_EXTRACTION,
                    GameId.of("tech_quantum_sorting"),
                ),
            ).stage,
        )
    }

    private fun development(
        refinedIron: Long = 0L,
        refinedCopper: Long = 0L,
        powerCells: Long = 0L,
        sensors: Long = 0L,
        technologies: Set<GameId> = emptySet(),
    ): FerrumColonyDevelopment {
        val state = ManufacturingGameState(
            economy = EconomyState(
                inventory = mapOf(
                    FerrumIds.REFINED_IRON to refinedIron,
                    FerrumIds.REFINED_COPPER to refinedCopper,
                    FerrumIds.POWER_CELL to powerCells,
                    FerrumIds.SENSOR_ARRAY to sensors,
                ),
                deposits = emptyMap(),
                spaceDollars = 0L,
                transactionSequence = 0L,
            ),
            refining = RefiningState.empty(),
            assembly = AssemblyState(emptyList(), technologies, 1L),
        )
        return FerrumColonyDevelopment.from(state)
    }
}
