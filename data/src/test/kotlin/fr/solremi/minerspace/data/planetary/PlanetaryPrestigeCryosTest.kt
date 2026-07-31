package fr.solremi.minerspace.data.planetary

import fr.solremi.minerspace.data.cryos.CryosIxContentFactory
import fr.solremi.minerspace.data.save.CryosIxStateCodec
import fr.solremi.minerspace.data.save.PrestigeStateCodec
import fr.solremi.minerspace.domain.cryos.*
import fr.solremi.minerspace.domain.cryos.CryosCommandResult
import fr.solremi.minerspace.domain.cryos.CryosIxEngine
import fr.solremi.minerspace.domain.prestige.*
import fr.solremi.minerspace.domain.robot.*
import fr.solremi.minerspace.domain.robot.RobotStatistics
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlanetPrestigeEngineTest {
    private val veteran = RobotInstance(
        id = GameId.of("robot_extractor_01"),
        family = RobotFamily.EXTRACTOR,
        displayName = "Aster",
        serialNumber = "EX-0001",
        level = 5,
        trait = RobotTrait.PRECISE,
        masteryPoints = 7_500L,
        priority = AutomationPriority.BALANCED,
        statistics = RobotStatistics(extracted = 50_000L, activeSeconds = 12_000L),
    )

    @Test
    fun `prepared transfer can be reconciled repeatedly without duplicate stellar cores`() {
        val engine = PlanetPrestigeEngine()
        val snapshot = PrestigeSnapshot(
            launchShipyardUnlocked = true,
            discoveredCodexEntryIds = setOf(GameId.of("codex_raw_iron")),
            archiveIds = setOf(GameId.of("nova_first_contact")),
            permanentBonusIds = setOf(GameId.of("bonus_specialization_industrial")),
            robots = listOf(veteran),
        )
        var state = engine.initialState()
        state = (engine.prepareTransfer(state, snapshot, 1_000L) as PrestigeCommandResult.Applied).state
        val first = (engine.reconcilePrepared(state) as PrestigeCommandResult.Applied).state
        val second = (engine.reconcilePrepared(first) as PrestigeCommandResult.Applied).state
        assertEquals(3L, first.stellarCores)
        assertEquals(3L, second.stellarCores)
        assertEquals(veteran.id, second.veteranRobot?.id)
        assertTrue(GameId.of("codex_raw_iron") in second.permanentCodexEntryIds)
        assertTrue(GameId.of("nova_first_contact") in second.permanentArchiveIds)
        val committed = (engine.finalizeTransfer(second) as PrestigeCommandResult.Applied).state
        assertEquals(PlanetId.CRYOS_IX, committed.activePlanet)
        assertNull(committed.pendingTransfer)
        assertEquals(1, committed.completedTransfers)
    }

    @Test
    fun `shipyard and veteran are both mandatory`() {
        val engine = PlanetPrestigeEngine()
        val noShipyard = PrestigeSnapshot(false, emptySet(), emptySet(), emptySet(), listOf(veteran))
        assertEquals("launch_shipyard_locked", (engine.prepareTransfer(engine.initialState(), noShipyard, 0) as PrestigeCommandResult.Rejected).code)
        val novice = veteran.copy(masteryPoints = 5_999L)
        val noVeteran = noShipyard.copy(launchShipyardUnlocked = true, robots = listOf(novice))
        assertEquals("veteran_robot_required", (engine.prepareTransfer(engine.initialState(), noVeteran, 0) as PrestigeCommandResult.Rejected).code)
    }
}

class PrestigeStateCodecTest {
    @Test
    fun `pending transfer and permanent state survive round trip`() {
        val veteran = VeteranRobotSnapshot(
            GameId.of("robot_extractor_01"), "Aster", "EX-0001", 5,
            RobotTrait.PRECISE, 7_500, RobotStatistics(extracted = 99, activeSeconds = 80),
        )
        val pending = PendingPlanetTransfer(
            "transfer|safe", PlanetId.FERRUM_DELTA, PlanetId.CRYOS_IX, 3,
            setOf(GameId.of("codex_raw_iron")), setOf(GameId.of("nova_first_contact")),
            setOf(GameId.of("bonus_ferrum_legacy")), veteran, 1_000,
        )
        val state = PrestigeState(
            PlanetId.FERRUM_DELTA, 0, 0, emptySet(), emptySet(), emptySet(),
            null, pending, 1,
        )
        val codec = PrestigeStateCodec()
        assertEquals(state, codec.decode(codec.encode(state, 1_500)))
    }
}

class CryosIxContentAndEngineTest {
    @Test
    fun `factory exposes the complete Cryos IX budget`() {
        val content = CryosIxContentFactory.create()
        assertEquals("1.0.0", content.contentVersion)
        assertEquals(6, content.sectors.size)
        assertEquals(4, content.resources.size)
        assertEquals(4, content.refinedMaterialIds.size)
        assertEquals(8, content.recipes.size)
        assertEquals(5, content.technologies.size)
        assertEquals(8, content.modules.size)
        assertEquals(12, content.mainMissionIds.size)
        assertEquals(10, content.secondaryMissionIds.size)
        assertEquals(3, content.eventIds.size)
        assertEquals(2, content.narrativeDiscoveryIds.size)
        assertEquals(30, content.codexEntryIds.size)
        assertTrue(content.resources.values.filter { it.mandatory }.all { it.sourceSectorId in content.sectors })
    }

    @Test
    fun `thermal loop reaches the frontier and cannot bypass required systems`() {
        val content = CryosIxContentFactory.create()
        val engine = CryosIxEngine(content)
        var state = engine.initialState(GameId.of("robot_extractor_01"))
        assertTrue(engine.completePlanetaryObjective(state) is CryosCommandResult.Rejected)
        state = applied(engine.installBase(state))

        fun apply(result: CryosCommandResult) { state = applied(result) }
        fun power() {
            while (state.energy < 350) apply(engine.generateEnergy(state))
            while (state.heat < 400) {
                if (state.energy < 40) apply(engine.generateEnergy(state))
                apply(engine.heatBase(state))
            }
        }

        val rawCryonite = GameId.of("raw_cryonite")
        val rawGlass = GameId.of("raw_ice_silicate")
        repeat(18) {
            power()
            apply(engine.extract(state, rawCryonite))
            apply(engine.extract(state, rawGlass))
            apply(engine.refine(state, GameId.of("recipe_cryonite_plate")))
            apply(engine.refine(state, GameId.of("recipe_thermal_glass")))
        }
        repeat(5) {
            power(); apply(engine.buildThermalNode(state))
            power(); apply(engine.unlockNextSector(state))
        }
        power(); apply(engine.craftNextModule(state))
        repeat(3) { power(); apply(engine.installNextTechnology(state)) }
        power(); apply(engine.completePlanetaryObjective(state))

        assertTrue(state.frontierUnlocked)
        assertEquals(6, state.unlockedSectorIds.size)
        assertEquals(5, state.thermalNodes)
        assertEquals(12, state.completedMainMissionIds.size)
        assertNotNull(state.veteranRobotId)
    }

    private fun applied(result: CryosCommandResult): CryosIxState =
        (result as? CryosCommandResult.Applied)?.state ?: error((result as CryosCommandResult.Rejected).code)
}

class CryosIxStateCodecTest {
    @Test
    fun `thermal state and veteran survive round trip`() {
        val definitions = CryosIxContentFactory.create()
        val engine = CryosIxEngine(definitions)
        var state = engine.initialState(GameId.of("robot_extractor_01"))
        state = (engine.installBase(state) as CryosCommandResult.Applied).state
        state = (engine.generateEnergy(state) as CryosCommandResult.Applied).state
        val codec = CryosIxStateCodec()
        val restored = codec.decode(codec.encode(state, definitions.contentVersion, 2_000))
        assertEquals(state, restored)
    }
}
