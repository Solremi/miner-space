package fr.solremi.minerspace.game.navigation

import fr.solremi.minerspace.domain.prestige.PendingPlanetTransfer
import fr.solremi.minerspace.domain.prestige.PlanetId
import fr.solremi.minerspace.domain.prestige.PlanetPrestigeEngine
import fr.solremi.minerspace.domain.prestige.VeteranRobotSnapshot
import fr.solremi.minerspace.domain.robot.RobotStatistics
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InitialRouteResolverTest {
    private val initial = PlanetPrestigeEngine().initialState()

    @Test
    fun `Ferrum is the default route`() {
        assertEquals(InitialRoute.FERRUM, InitialRouteResolver.resolve(initial, false))
    }

    @Test
    fun `pending transfer has priority over every planet route`() {
        val pending = PendingPlanetTransfer(
            transferId = "transfer_1",
            sourcePlanet = PlanetId.FERRUM_DELTA,
            destinationPlanet = PlanetId.CRYOS_IX,
            expectedStellarCores = 3L,
            preservedCodexEntryIds = emptySet(),
            preservedArchiveIds = emptySet(),
            preservedBonusIds = emptySet(),
            veteranRobot = VeteranRobotSnapshot(
                id = GameId.of("robot_veteran"),
                displayName = "Aster",
                serialNumber = "FD-EX-0001",
                level = 5,
                trait = RobotTrait.PRECISE,
                masteryPoints = 6_000L,
                statistics = RobotStatistics(),
            ),
            preparedAtEpochMillis = 1L,
        )
        val state = initial.copy(pendingTransfer = pending)
        assertEquals(InitialRoute.PLANET_TRANSFER, InitialRouteResolver.resolve(state, true))
    }

    @Test
    fun `Cryos with an active world resumes the frontier`() {
        val cryos = initial.copy(activePlanet = PlanetId.CRYOS_IX)
        assertEquals(InitialRoute.FRONTIER, InitialRouteResolver.resolve(cryos, true))
        assertEquals(InitialRoute.CRYOS, InitialRouteResolver.resolve(cryos, false))
    }
}
