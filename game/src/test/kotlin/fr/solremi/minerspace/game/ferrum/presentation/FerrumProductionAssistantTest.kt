package fr.solremi.minerspace.game.ferrum.presentation

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
import fr.solremi.minerspace.game.ferrum.model.FerrumIds
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
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
        val job = RefiningJob(
            id = "rf_job_1",
            recipeId = FerrumIds.RECIPE_IRON,
            queuedAtEpochMillis = 0L,
            startsAtEpochMillis = 0L,
            finishesAtEpochMillis = 1_000L,
            reservedInputs = mapOf(FerrumIds.RAW_IRON to 2L),
            outputResourceId = FerrumIds.REFINED_IRON,
            outputQuantity = 1L,
            status = RefiningJobStatus.READY_TO_COLLECT,
        )
        val state = initialState().copy(refining = RefiningState(listOf(job), emptyMap(), 2L))

        val advice = FerrumProductionAssistant.evaluate(state, refiningDefinitions(), assemblyDefinitions())

        assertEquals(FerrumNodeId.REFINER, advice.target)
        assertEquals("Production terminée", advice.title)
    }

    private fun initialState(): ManufacturingGameState = ManufacturingGameState(
        economy = EconomyState(
            inventory = listOf(
                FerrumIds.RAW_IRON,
                FerrumIds.RAW_COPPER,
                FerrumIds.RAW_CRYSTAL,
                FerrumIds.REFINED_IRON,
                FerrumIds.REFINED_COPPER,
                FerrumIds.POWER_CELL,
                FerrumIds.SENSOR_ARRAY,
                FerrumIds.TECH_EXTRACTION_ITEM,
            ).associateWith { 0L },
            deposits = mapOf(
                FerrumIds.DEPOSIT_IRON to DepositState(1_000L, 0L),
                FerrumIds.DEPOSIT_COPPER to DepositState(1_000L, 0L),
                FerrumIds.DEPOSIT_CRYSTAL to DepositState(1_000L, 0L),
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
            FerrumIds.RECIPE_IRON to RecipeDefinition(
                FerrumIds.RECIPE_IRON,
                "recipe.iron",
                mapOf(FerrumIds.RAW_IRON to 2L),
                FerrumIds.REFINED_IRON,
                1L,
                10L,
            ),
            FerrumIds.RECIPE_COPPER to RecipeDefinition(
                FerrumIds.RECIPE_COPPER,
                "recipe.copper",
                mapOf(FerrumIds.RAW_COPPER to 2L),
                FerrumIds.REFINED_COPPER,
                1L,
                10L,
            ),
        ),
    )

    private fun assemblyDefinitions(): AssemblyDefinitions {
        val powerCell = AssemblyRecipeDefinition(
            FerrumIds.ASSEMBLY_POWER_CELL,
            "assembly.power",
            mapOf(FerrumIds.REFINED_IRON to 3L, FerrumIds.REFINED_COPPER to 2L),
            FerrumIds.POWER_CELL,
            1L,
            20L,
            AssemblyOutputKind.COMPONENT,
        )
        val sensor = AssemblyRecipeDefinition(
            FerrumIds.ASSEMBLY_SENSOR_ARRAY,
            "assembly.sensor",
            mapOf(FerrumIds.REFINED_COPPER to 2L, FerrumIds.RAW_CRYSTAL to 4L),
            FerrumIds.SENSOR_ARRAY,
            1L,
            20L,
            AssemblyOutputKind.COMPONENT,
        )
        val technologyRecipe = AssemblyRecipeDefinition(
            FerrumIds.ASSEMBLY_TECH_EXTRACTION,
            "assembly.tech",
            mapOf(FerrumIds.POWER_CELL to 2L, FerrumIds.SENSOR_ARRAY to 1L),
            FerrumIds.TECH_EXTRACTION_ITEM,
            1L,
            30L,
            AssemblyOutputKind.TECHNOLOGY,
        )
        val technology = TechnologyDefinition(
            FerrumIds.TECH_EXTRACTION,
            "technology.extraction",
            FerrumIds.TECH_EXTRACTION_ITEM,
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
}
