package fr.solremi.minerspace.domain.refining

import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RefiningEngineTest {
    private val rawIron = GameId.of("raw_iron")
    private val refinedIron = GameId.of("refined_iron_ingot")
    private val recipeId = GameId.of("recipe_iron_ingot")
    private val economyDefinitions = EconomyDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        resources = listOf(
            ResourceDefinition(rawIron, "raw", 1L, 100L, true),
            ResourceDefinition(refinedIron, "refined", 5L, 4L, true),
        ).associateBy { it.id },
        deposits = listOf(
            DepositDefinition(GameId.of("deposit_iron"), rawIron, 100L, 1L, 10L),
        ).associateBy { it.id },
    )
    private val definitions = RefiningDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        robot = RefinerRobotDefinition(GameId.of("robot_rf_01"), "robot", 4),
        recipes = listOf(
            RecipeDefinition(recipeId, "recipe", mapOf(rawIron to 10L), refinedIron, 4L, 10L),
        ).associateBy { it.id },
    )
    private val engine = RefiningEngine(
        definitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )

    @Test
    fun `launch reserves ingredients and cancellation follows refund bands`() {
        val base = state(rawIronStock = 30L)
        val launched = engine.launch(base, recipeId, 1_000L) as RefiningCommandResult.Applied
        assertEquals(20L, launched.state.economy.inventory.getValue(rawIron))

        val early = engine.cancel(launched.state, "rf_job_1", 1_500L) as RefiningCommandResult.Applied
        assertEquals(30L, early.state.economy.inventory.getValue(rawIron))
        assertTrue(early.transaction.reason.endsWith(":10000"))

        val midLaunch = engine.launch(base, recipeId, 1_000L) as RefiningCommandResult.Applied
        val middle = engine.cancel(midLaunch.state, "rf_job_1", 6_000L) as RefiningCommandResult.Applied
        assertEquals(28L, middle.state.economy.inventory.getValue(rawIron))
        assertTrue(middle.transaction.reason.endsWith(":8000"))

        val lateLaunch = engine.launch(base, recipeId, 1_000L) as RefiningCommandResult.Applied
        val late = engine.cancel(lateLaunch.state, "rf_job_1", 10_500L) as RefiningCommandResult.Applied
        assertEquals(20L, late.state.economy.inventory.getValue(rawIron))
        assertTrue(late.transaction.reason.endsWith(":0"))
    }

    @Test
    fun `completed output remains collectable while storage is full`() {
        val launched = engine.launch(state(rawIronStock = 20L, refinedStock = 4L), recipeId, 1_000L)
            as RefiningCommandResult.Applied
        val completed = engine.reconcile(launched.state, 12_000L)
        val blocked = engine.collect(completed, "rf_job_1", 12_000L)
        assertTrue(blocked is RefiningCommandResult.Rejected)
        assertEquals("output_storage_full", (blocked as RefiningCommandResult.Rejected).code)
        assertEquals(1, blocked.state.refining.jobs.size)
        assertEquals(RefiningJobStatus.READY_TO_COLLECT, blocked.state.refining.jobs.single().status)

        val freed = blocked.state.copy(
            economy = blocked.state.economy.copy(
                inventory = blocked.state.economy.inventory + (refinedIron to 0L),
            ),
        )
        val collected = engine.collect(freed, "rf_job_1", 12_000L) as RefiningCommandResult.Applied
        assertEquals(4L, collected.state.economy.inventory.getValue(refinedIron))
        assertTrue(collected.state.refining.jobs.isEmpty())
    }

    @Test
    fun `collecting the same completed job twice cannot duplicate output`() {
        val launched = engine.launch(state(rawIronStock = 20L), recipeId, 1_000L)
            as RefiningCommandResult.Applied
        val first = engine.collect(launched.state, "rf_job_1", 12_000L) as RefiningCommandResult.Applied
        val second = engine.collect(first.state, "rf_job_1", 12_000L)
        assertTrue(second is RefiningCommandResult.Rejected)
        assertEquals(4L, second.state.economy.inventory.getValue(refinedIron))
    }

    private fun state(rawIronStock: Long, refinedStock: Long = 0L): RefiningGameState {
        val economy = CoreEconomyEngine(economyDefinitions).initialState().copy(
            inventory = mapOf(rawIron to rawIronStock, refinedIron to refinedStock),
        )
        return RefiningGameState(economy, RefiningState.empty())
    }
}
