package fr.solremi.minerspace.domain.exploration

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExplorationEngineTest {
    private val core = GameId.of("sector_core_delta")
    private val copper = GameId.of("sector_copper_ridge")
    private val deep = GameId.of("sector_xenon_depths")
    private val archive = GameId.of("sector_archive_ruins")
    private val extraction = GameId.of("tech_extraction_protocol")
    private val sorting = GameId.of("tech_quantum_sorting")
    private val sensor = GameId.of("component_sensor_array")
    private val power = GameId.of("component_power_cell")

    @Test
    fun `all sectors remain reachable and costs are applied once`() {
        val definitions = definitions()
        val engine = ExplorationEngine(definitions)
        var state = engine.initialState()
        val access = ExplorationAccess(
            scannerLevel = 3,
            spaceDollars = 5_000L,
            inventory = mapOf(power to 10L, sensor to 10L),
            installedTechnologyIds = setOf(extraction, sorting),
        )

        definitions.sectors.values.filterNot { it.initiallyUnlocked }.forEach { sector ->
            state = (engine.scan(state, sector.id, access) as ExplorationCommandResult.Applied).state
            state = (engine.unlock(state, sector.id, access) as ExplorationCommandResult.Applied).state
        }

        assertEquals(definitions.sectors.keys, state.unlockedSectorIds)
        assertEquals(2_420L, state.spentSpaceDollars)
        assertEquals(2L, state.spentComponents[power])
        assertEquals(5L, state.spentComponents[sensor])
        assertEquals(2, state.discoveredRareDepositIds.size)
        assertTrue(engine.unlock(state, copper, access) is ExplorationCommandResult.Rejected)
    }

    @Test
    fun `scanner technology and path prerequisites are explicit`() {
        val engine = ExplorationEngine(definitions())
        val initial = engine.initialState()
        val weak = ExplorationAccess(1, 5_000L, mapOf(power to 10L, sensor to 10L), emptySet())

        assertFalse(engine.availability(initial, deep, weak).canScan)
        assertTrue(engine.scan(initial, archive, weak) is ExplorationCommandResult.Rejected)

        val strong = weak.copy(scannerLevel = 3, installedTechnologyIds = setOf(extraction, sorting))
        val scannedCopper = (engine.scan(initial, copper, strong) as ExplorationCommandResult.Applied).state
        val openedCopper = (engine.unlock(scannedCopper, copper, strong) as ExplorationCommandResult.Applied).state
        assertTrue(engine.availability(openedCopper, deep, strong).canScan)
    }

    private fun definitions(): ExplorationDefinitions {
        val sectors = listOf(
            SectorDefinition(core, "core", "base", SectorBounds(0, 0, 10, 10), 0, 1, emptySet(), emptySet(), emptyMap(), null, false, true),
            SectorDefinition(copper, "copper", "copper", SectorBounds(10, 0, 10, 10), 120, 1, setOf(core), emptySet(), emptyMap(), null, false, false),
            SectorDefinition(deep, "deep", "xenon", SectorBounds(20, 0, 10, 10), 900, 2, setOf(copper), setOf(extraction), mapOf(sensor to 3L), GameId.of("rare_xenon"), true, false),
            SectorDefinition(archive, "archive", "archive", SectorBounds(30, 0, 10, 10), 1_400, 3, setOf(deep), setOf(sorting), mapOf(power to 2L, sensor to 2L), GameId.of("rare_archive"), true, false),
        ).associateBy { it.id }
        return ExplorationDefinitions(1, "0.6.0", sectors)
    }
}
