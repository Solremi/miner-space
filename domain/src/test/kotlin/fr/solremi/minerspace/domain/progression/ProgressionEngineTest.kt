package fr.solremi.minerspace.domain.progression

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ProgressionEngineTest {
    private val definitions = fixtures()
    private val engine = ProgressionEngine(definitions)
    private val rich = ProgressSnapshot(
        inventory = mapOf(
            id("raw_iron") to 100L, id("raw_copper") to 80L, id("raw_crystal") to 40L,
            id("refined_iron_ingot") to 15L, id("refined_copper_plate") to 10L,
            id("component_power_cell") to 3L, id("component_sensor_array") to 2L,
        ),
        spaceDollars = 2_000L, installedTechnologyCount = 2, unlockedSectorCount = 6,
        rareDiscoveryCount = 1, robotLevelSum = 8, robotMasteryPoints = 1_600L,
        ownedModuleCount = 2, specializationChosen = true,
    )

    @Test fun `tutorial resumes and completes deterministically`() {
        val partial = rich.copy(inventory = mapOf(id("raw_iron") to 25L))
        val first = engine.synchronize(engine.initialState(), partial)
        assertEquals(1, first.tutorialStepIndex)
        val resumed = engine.synchronize(first, rich)
        assertEquals(7, resumed.tutorialStepIndex)
        assertEquals(7, resumed.completedTutorialIds.size)
    }

    @Test fun `at least three objectives run in parallel after introduction`() {
        val objectives = engine.objectiveViews(engine.initialState(), rich)
        assertTrue(objectives.size >= 3)
        assertTrue(objectives.count { it.definition.kind == MissionKind.MAIN } >= 3)
    }

    @Test fun `contract delivery consumes stock and advances occurrence once`() {
        var state = engine.initialState().copy(claimedMissionIds = setOf(id("main_refining"), id("main_automation")))
        val contract = engine.activeContracts(state, rich).first { it.definition.tier == ContractTier.SIMPLE }
        val applied = engine.deliverContract(state, contract.occurrenceId, rich) as ProgressionCommandResult.Applied
        assertEquals(-contract.definition.quantity, applied.transaction.delta.inventoryDelta.getValue(contract.definition.resourceId))
        state = applied.state
        val duplicate = engine.deliverContract(state, contract.occurrenceId, rich)
        assertTrue(duplicate is ProgressionCommandResult.Rejected)
    }

    @Test fun `impossible codex entries stay hidden until prerequisites are claimed`() {
        val state = engine.synchronize(engine.initialState(), rich)
        assertFalse(engine.visibleCodexEntries(state, rich).any { it.definition.id == id("codex_technology") })
        val unlocked = state.copy(claimedMissionIds = setOf(id("main_technology")))
        assertTrue(engine.visibleCodexEntries(engine.synchronize(unlocked, rich), rich).any { it.definition.id == id("codex_technology") })
    }

    @Test fun `collection reward cannot be claimed twice`() {
        val collection = definitions.collections.getValue(id("collection_foundation"))
        val ready = engine.initialState().copy(discoveredCodexEntryIds = collection.entryIds)
        val first = engine.claimCollection(ready, collection.id) as ProgressionCommandResult.Applied
        assertEquals(collection.rewardSpaceDollars, first.transaction.delta.spaceDollarsDelta)
        assertTrue(engine.claimCollection(first.state, collection.id) is ProgressionCommandResult.Rejected)
    }

    private fun fixtures(): ProgressionDefinitions {
        val tutorials = (1..7).map { TutorialStepDefinition(id("tutorial_$it"), "Jour $it", "T$it", "A$it", ProgressMetric.RAW_IRON, it.toLong()) }
        val missions = listOf(
            MissionDefinition(id("main_foundation"), MissionKind.MAIN, "Fondation", ProgressMetric.RAW_TOTAL, 10, 10, emptySet()),
            MissionDefinition(id("main_refining"), MissionKind.MAIN, "Raffinage", ProgressMetric.REFINED_TOTAL, 1, 10, emptySet()),
            MissionDefinition(id("main_automation"), MissionKind.MAIN, "Automation", ProgressMetric.COMPONENT_TOTAL, 1, 10, emptySet()),
            MissionDefinition(id("main_technology"), MissionKind.MAIN, "Technologie", ProgressMetric.TECHNOLOGIES, 1, 10, setOf(id("main_automation"))),
        ).associateBy { it.id }
        val contracts = ContractTier.entries.map { tier -> ContractDefinition(id("contract_${tier.name.lowercase()}"), tier, tier.name, id("raw_iron"), 5, 10, emptySet()) }
        val entries = listOf(
            CodexEntryDefinition(id("codex_iron"), CodexCategory.RESOURCE, "Fer", ProgressMetric.RAW_IRON, 1, emptySet(), id("collection_foundation")),
            CodexEntryDefinition(id("codex_technology"), CodexCategory.INDUSTRY, "Tech", ProgressMetric.TECHNOLOGIES, 1, setOf(id("main_technology")), null),
        ).associateBy { it.id }
        val collections = mapOf(id("collection_foundation") to CollectionDefinition(id("collection_foundation"), "Fondation", setOf(id("codex_iron")), 25))
        return ProgressionDefinitions(1, "test", tutorials, missions, contracts, entries, collections)
    }

    private fun id(value: String) = GameId.of(value)
}
