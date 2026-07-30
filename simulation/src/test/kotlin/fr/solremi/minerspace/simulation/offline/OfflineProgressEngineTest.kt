package fr.solremi.minerspace.simulation.offline

import fr.solremi.minerspace.domain.assembly.AssemblerRobotDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyEngine
import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.domain.assembly.AssemblyRecipeDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.domain.refining.RefinerRobotDefinition
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.refining.RecipeDefinition
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OfflineProgressEngineTest {
    private val raw = GameId.of("raw_iron")
    private val refined = GameId.of("refined_iron")
    private val component = GameId.of("component_test")
    private val techItem = GameId.of("tech_test_item")
    private val depositId = GameId.of("deposit_iron")
    private val refiningRecipe = GameId.of("recipe_refine")
    private val assemblyRecipe = GameId.of("recipe_component")
    private val techRecipe = GameId.of("recipe_tech")
    private val techId = GameId.of("tech_test")

    @Test
    fun `one minute absence advances deterministic extraction`() {
        val fixture = fixture()
        val result = fixture.offline.apply(fixture.state(), 1_000_000L, 1_060_000L)

        assertEquals(60L, result.report.simulatedSeconds)
        assertEquals(600L, result.report.extractedByResource.getValue(raw))
        assertFalse(result.report.capped)
    }

    @Test
    fun `eight hours are simulated and twenty four hours are capped to the same result`() {
        val fixture = fixture()
        val start = 1_000_000L
        val eightHours = fixture.offline.apply(fixture.state(), start, start + 8L * 60L * 60L * 1_000L)
        val twentyFourHours = fixture.offline.apply(fixture.state(), start, start + 24L * 60L * 60L * 1_000L)

        assertEquals(8L * 60L * 60L, eightHours.report.simulatedSeconds)
        assertEquals(eightHours.state, twentyFourHours.state)
        assertTrue(twentyFourHours.report.capped)
        assertEquals(24L * 60L * 60L, twentyFourHours.report.absentSeconds)
    }

    @Test
    fun `depleted deposit and full storage stop offline production exactly`() {
        val depletedFixture = fixture(reserve = 50L)
        val depleted = depletedFixture.offline.apply(depletedFixture.state(), 1_000L, 61_000L)
        assertEquals(0L, depleted.state.economy.deposits.getValue(depositId).remainingReserve)
        assertTrue(depositId in depleted.report.depletedDepositIds)

        val fullFixture = fixture(storageCapacity = 1_000L)
        val fullState = fullFixture.state().copy(
            economy = fullFixture.state().economy.copy(
                inventory = mapOf(raw to 990L, refined to 0L, component to 0L, techItem to 0L),
            ),
        )
        val full = fullFixture.offline.apply(fullState, 1_000L, 61_000L)
        assertEquals(10L, full.state.economy.deposits.getValue(depositId).pendingCollection)
        assertTrue(depositId in full.report.storageBlockedDepositIds)
    }

    @Test
    fun `clock moving backward gives no progress`() {
        val fixture = fixture()
        val result = fixture.offline.apply(fixture.state(), 2_000_000L, 1_000_000L)

        assertEquals(0L, result.report.simulatedSeconds)
        assertTrue(result.report.clockMovedBackward)
        assertEquals(fixture.state(), result.state)
    }

    private fun fixture(
        reserve: Long = 100_000L,
        storageCapacity: Long = 10_000L,
    ): Fixture {
        val economyDefinitions = EconomyDefinitions(
            schemaVersion = 1,
            contentVersion = "test",
            resources = listOf(
                ResourceDefinition(raw, "raw", 1L, storageCapacity, true),
                ResourceDefinition(refined, "refined", 2L, 10_000L, true),
                ResourceDefinition(component, "component", 3L, 10_000L, true),
                ResourceDefinition(techItem, "tech", 0L, 10L, false),
            ).associateBy { it.id },
            deposits = mapOf(
                depositId to DepositDefinition(depositId, raw, reserve, 10L, 1_000L),
            ),
        )
        val refiningDefinitions = RefiningDefinitions(
            1,
            "test",
            RefinerRobotDefinition(GameId.of("rf"), "rf", 4),
            mapOf(
                refiningRecipe to RecipeDefinition(refiningRecipe, "refine", mapOf(raw to 10L), refined, 1L, 10L),
            ),
        )
        val assemblyDefinitions = AssemblyDefinitions(
            1,
            "test",
            AssemblerRobotDefinition(GameId.of("as"), "as", 4),
            recipes = mapOf(
                assemblyRecipe to AssemblyRecipeDefinition(
                    assemblyRecipe,
                    "component",
                    mapOf(refined to 1L),
                    component,
                    1L,
                    10L,
                    AssemblyOutputKind.COMPONENT,
                ),
                techRecipe to AssemblyRecipeDefinition(
                    techRecipe,
                    "tech",
                    mapOf(component to 1L),
                    techItem,
                    1L,
                    10L,
                    AssemblyOutputKind.TECHNOLOGY,
                ),
            ),
            technologies = mapOf(
                techId to TechnologyDefinition(techId, "tech", techItem, emptySet(), 100_000L),
            ),
        )
        val economy = CoreEconomyEngine(economyDefinitions)
        val refiner = RefiningEngine(
            refiningDefinitions,
            economyDefinitions.resources.mapValues { it.value.storageCapacity },
        )
        val assembler = AssemblyEngine(
            assemblyDefinitions,
            economyDefinitions.resources.mapValues { it.value.storageCapacity },
        )
        return Fixture(economy, OfflineProgressEngine(economy, refiner, assembler))
    }

    private inner class Fixture(
        private val economy: CoreEconomyEngine,
        val offline: OfflineProgressEngine,
    ) {
        fun state(): ManufacturingGameState = ManufacturingGameState(
            economy = economy.initialState(),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
    }
}
