package fr.solremi.minerspace.domain.assembly

import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AssemblyEngineTest {
    @Test
    fun `builds component then technology and installs deterministic effect`() {
        val fixture = fixture()
        var state = fixture.initialState

        state = fixture.engine.launch(state, POWER_RECIPE, 0L).appliedState()
        state = fixture.engine.reconcile(state, 20_000L)
        state = fixture.engine.collect(state, "as_job_1", 20_000L).appliedState()
        assertEquals(1L, state.economy.inventory[POWER_CELL])

        state = state.copy(
            economy = state.economy.copy(
                inventory = state.economy.inventory.toMutableMap().apply {
                    this[POWER_CELL] = 2L
                    this[SENSOR_ARRAY] = 1L
                },
            ),
        )
        state = fixture.engine.launch(state, TECH_ONE_RECIPE, 21_000L).appliedState()
        state = fixture.engine.reconcile(state, 56_000L)
        state = fixture.engine.collect(state, "as_job_2", 56_000L).appliedState()
        state = fixture.engine.installTechnology(state, TECH_ONE).appliedState()

        assertTrue(TECH_ONE in state.assembly.installedTechnologyIds)
        assertEquals(1_200_000L, fixture.engine.productionMultipliers(state.assembly).technologies)
        assertEquals(0L, state.economy.inventory[TECH_ONE_ITEM])
    }

    @Test
    fun `locks second technology until prerequisite is installed and compares before after`() {
        val fixture = fixture()
        val locked = fixture.engine.launch(fixture.initialState, TECH_TWO_RECIPE, 0L)
        assertTrue(locked is AssemblyCommandResult.Rejected)
        assertEquals("technology_prerequisite_missing", (locked as AssemblyCommandResult.Rejected).code)

        val withFirst = fixture.initialState.copy(
            assembly = fixture.initialState.assembly.copy(installedTechnologyIds = setOf(TECH_ONE)),
        )
        val comparison = fixture.engine.compareExtraction(6L, withFirst.assembly, TECH_TWO)

        assertEquals(360L, comparison.basePerMinute)
        assertEquals(432L, comparison.currentPerMinute)
        assertEquals(486L, comparison.projectedPerMinute)
        assertFalse(TECH_TWO in withFirst.assembly.installedTechnologyIds)
    }

    private fun AssemblyCommandResult.appliedState(): ManufacturingGameState =
        (this as AssemblyCommandResult.Applied).state

    private fun fixture(): Fixture {
        val recipes = listOf(
            AssemblyRecipeDefinition(
                POWER_RECIPE,
                "assembly.power_cell",
                mapOf(REFINED_IRON to 3L, REFINED_COPPER to 2L),
                POWER_CELL,
                1L,
                20L,
                AssemblyOutputKind.COMPONENT,
            ),
            AssemblyRecipeDefinition(
                TECH_ONE_RECIPE,
                "assembly.tech_one",
                mapOf(POWER_CELL to 2L, SENSOR_ARRAY to 1L),
                TECH_ONE_ITEM,
                1L,
                35L,
                AssemblyOutputKind.TECHNOLOGY,
            ),
            AssemblyRecipeDefinition(
                TECH_TWO_RECIPE,
                "assembly.tech_two",
                mapOf(POWER_CELL to 2L, SENSOR_ARRAY to 2L),
                TECH_TWO_ITEM,
                1L,
                50L,
                AssemblyOutputKind.TECHNOLOGY,
                setOf(TECH_ONE),
            ),
        ).associateBy { it.id }
        val technologies = listOf(
            TechnologyDefinition(TECH_ONE, "tech.one", TECH_ONE_ITEM, emptySet(), 200_000L),
            TechnologyDefinition(TECH_TWO, "tech.two", TECH_TWO_ITEM, setOf(TECH_ONE), 150_000L),
        ).associateBy { it.id }
        val definitions = AssemblyDefinitions(
            1,
            "0.4.0-test",
            AssemblerRobotDefinition(GameId.of("robot_as_01"), "robot.as", 4),
            recipes,
            technologies,
        )
        val capacities = setOf(
            REFINED_IRON,
            REFINED_COPPER,
            POWER_CELL,
            SENSOR_ARRAY,
            TECH_ONE_ITEM,
            TECH_TWO_ITEM,
        ).associateWith { 1_000L }
        val inventory = capacities.keys.associateWith { 0L }.toMutableMap().apply {
            this[REFINED_IRON] = 20L
            this[REFINED_COPPER] = 20L
            this[POWER_CELL] = 2L
            this[SENSOR_ARRAY] = 2L
        }
        return Fixture(
            AssemblyEngine(definitions, capacities),
            ManufacturingGameState(
                EconomyState(inventory, emptyMap(), 0L, 0L),
                RefiningState.empty(),
                AssemblyState.empty(),
            ),
        )
    }

    private data class Fixture(
        val engine: AssemblyEngine,
        val initialState: ManufacturingGameState,
    )

    private companion object {
        val REFINED_IRON = GameId.of("refined_iron_ingot")
        val REFINED_COPPER = GameId.of("refined_copper_plate")
        val POWER_CELL = GameId.of("component_power_cell")
        val SENSOR_ARRAY = GameId.of("component_sensor_array")
        val TECH_ONE_ITEM = GameId.of("tech_extraction_protocol_item")
        val TECH_TWO_ITEM = GameId.of("tech_quantum_sorting_item")
        val TECH_ONE = GameId.of("tech_extraction_protocol")
        val TECH_TWO = GameId.of("tech_quantum_sorting")
        val POWER_RECIPE = GameId.of("assembly_power_cell")
        val TECH_ONE_RECIPE = GameId.of("assembly_tech_extraction_protocol")
        val TECH_TWO_RECIPE = GameId.of("assembly_tech_quantum_sorting")
    }
}
