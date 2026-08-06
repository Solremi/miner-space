package fr.solremi.minerspace.game.scene

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
        assertEquals(
            FerrumColonyStage.INDUSTRIAL,
            development(refinedIron = 5L).stage,
        )
    }

    @Test
    fun `one installed technology reveals the automated network`() {
        assertEquals(
            FerrumColonyStage.NETWORKED,
            development(technologies = setOf(GameId.of("tech_extraction_protocol"))).stage,
        )
    }

    @Test
    fun `two installed technologies reveal the orbital chantier`() {
        assertEquals(
            FerrumColonyStage.ORBITAL,
            development(
                technologies = setOf(
                    GameId.of("tech_extraction_protocol"),
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
                    REFINED_IRON to refinedIron,
                    REFINED_COPPER to refinedCopper,
                    POWER_CELL to powerCells,
                    SENSOR_ARRAY to sensors,
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

    private companion object {
        val REFINED_IRON = GameId.of("refined_iron_ingot")
        val REFINED_COPPER = GameId.of("refined_copper_plate")
        val POWER_CELL = GameId.of("component_power_cell")
        val SENSOR_ARRAY = GameId.of("component_sensor_array")
    }
}
