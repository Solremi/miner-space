package fr.solremi.minerspace.game.presentation

import fr.solremi.minerspace.domain.assembly.AssemblerRobotDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.domain.assembly.AssemblyRecipeDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.economy.DepositState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RecipeDefinition
import fr.solremi.minerspace.domain.refining.RefinerRobotDefinition
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.refining.RefiningJob
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.game.scene.FerrumNodeId
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FerrumProductionAssistantTest {
    @Test
    fun `initial advice sends the player to Aster and the iron deposit`() {
        val advice = FerrumProductionAssistant.evaluate(initialState(), refiningDefinitions(), assemblyDefinitions())

        assertEquals(1, advice.phase)
        assertEquals(FerrumNodeId.IRON_DEPOSIT, advice.target)
        assertTrue(advice.title.contains("Aster"))
    }

    @Test
    fun `ready refining output takes priority over onboarding`() {
        val initial = initialState()
        val job = RefiningJob(
            id = "rf_job_1",
            recipeId = RECIPE_IRON,
            queuedAtEpochMillis = 0L,
            startsAtEpochMillis = 0L,
            finishesAtEpochMillis = 1_000L,
            reservedInputs = mapOf(RAW_IRON to 2L),
            outputResourceId = REFINED_IRON,
            outputQuantity = 1L,
            status = RefiningJobStatus.READY_TO_COLLECT,
        )
        val state = initial.copy(
            refining = RefiningState(listOf(job), emptyMap(), 2L),
        )

        val advice = FerrumProductionAssistant.evaluate(state, refiningDefinitions(), assemblyDefinitions())

        assertEquals(FerrumNodeId.REFINER, advice.target)
        assertEquals("Production terminée", advice.title)
    }

    private fun initialState(): ManufacturingGameState = ManufacturingGameState(
        economy = EconomyState(
            inventory = listOf(
                RAW_IRON,
                RAW_COPPER,
                RAW_CRYSTAL,
                REFINED_IRON,
                REFINED_COPPER,
                POWER_CELL,
                SENSOR_ARRAY,
                TECH_EXTRACTION_ITEM,
            ).associateWith { 0L },
            deposits = mapOf(
                DEPOSIT_IRON to DepositState(1_000L, 0L),
                DEPOSIT_COPPER to DepositState(1_000L, 0L),
                DEPOSIT_CRYSTAL to DepositState(1_000L, 0L),
            ),
            spaceDollars = 0L,
            transactionSequence = 0L,
        ),
        refining = RefiningState.empty(),
        assembly = AssemblyState.empty(),
    )

    private fun refiningDefinitions(): RefiningDefinitions = RefiningDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        robot = RefinerRobotDefinition(GameId.of("robot_refiner"), "robot.refiner", 4),
        recipes = mapOf(
            RECIPE_IRON to RecipeDefinition(
                RECIPE_IRON,
                "recipe.iron",
                mapOf(RAW_IRON to 2L),
                REFINED_IRON,
                1L,
                10L,
            ),
            RECIPE_COPPER to RecipeDefinition(
                RECIPE_COPPER,
                "recipe.copper",
                mapOf(RAW_COPPER to 2L),
                REFINED_COPPER,
                1L,
                10L,
            ),
        ),
    )

    private fun assemblyDefinitions(): AssemblyDefinitions {
        val powerCell = AssemblyRecipeDefinition(
            ASSEMBLY_POWER_CELL,
            "assembly.power",
            mapOf(REFINED_IRON to 3L, REFINED_COPPER to 2L),
            POWER_CELL,
            1L,
            20L,
            AssemblyOutputKind.COMPONENT,
        )
        val sensor = AssemblyRecipeDefinition(
            ASSEMBLY_SENSOR_ARRAY,
            "assembly.sensor",
            mapOf(REFINED_COPPER to 2L, RAW_CRYSTAL to 4L),
            SENSOR_ARRAY,
            1L,
            20L,
            AssemblyOutputKind.COMPONENT,
        )
        val technologyRecipe = AssemblyRecipeDefinition(
            ASSEMBLY_TECH_EXTRACTION,
            "assembly.tech",
            mapOf(POWER_CELL to 2L, SENSOR_ARRAY to 1L),
            TECH_EXTRACTION_ITEM,
            1L,
            30L,
            AssemblyOutputKind.TECHNOLOGY,
        )
        val technology = TechnologyDefinition(
            TECH_EXTRACTION,
            "technology.extraction",
            TECH_EXTRACTION_ITEM,
            emptySet(),
            100_000L,
        )
        return AssemblyDefinitions(
            schemaVersion = 1,
            contentVersion = "test",
            robot = AssemblerRobotDefinition(GameId.of("robot_assembler"), "robot.assembler", 4),
            recipes = listOf(powerCell, sensor, technologyRecipe).associateBy { it.id },
            technologies = mapOf(technology.id to technology),
        )
    }

    private companion object {
        val RAW_IRON = GameId.of("raw_iron")
        val RAW_COPPER = GameId.of("raw_copper")
        val RAW_CRYSTAL = GameId.of("raw_crystal")
        val REFINED_IRON = GameId.of("refined_iron_ingot")
        val REFINED_COPPER = GameId.of("refined_copper_plate")
        val POWER_CELL = GameId.of("component_power_cell")
        val SENSOR_ARRAY = GameId.of("component_sensor_array")
        val TECH_EXTRACTION_ITEM = GameId.of("tech_extraction_protocol_item")
        val TECH_EXTRACTION = GameId.of("tech_extraction_protocol")
        val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
        val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
        val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")
        val RECIPE_IRON = GameId.of("recipe_iron_ingot")
        val RECIPE_COPPER = GameId.of("recipe_copper_plate")
        val ASSEMBLY_POWER_CELL = GameId.of("assembly_power_cell")
        val ASSEMBLY_SENSOR_ARRAY = GameId.of("assembly_sensor_array")
        val ASSEMBLY_TECH_EXTRACTION = GameId.of("assembly_tech_extraction_protocol")
    }
}
