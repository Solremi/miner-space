package fr.solremi.minerspace.simulation.economy

import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreEconomySimulatorTest {
    @Test
    fun `simulates 24 hours without negative value or overflow`() {
        val iron = GameId.of("raw_iron")
        val deposit = GameId.of("deposit_iron_alpha")
        val definitions = EconomyDefinitions(
            schemaVersion = 1,
            contentVersion = "test",
            resources = mapOf(
                iron to ResourceDefinition(
                    id = iron,
                    nameKey = "resource.raw_iron",
                    unitSalePrice = 2L,
                    storageCapacity = 5_000L,
                    sellable = true,
                ),
            ),
            deposits = mapOf(
                deposit to DepositDefinition(
                    id = deposit,
                    resourceId = iron,
                    initialReserve = 1_000_000L,
                    extractionPerSecond = 3L,
                    transportCapacity = 500L,
                ),
            ),
        )
        val engine = CoreEconomyEngine(definitions)
        val report = CoreEconomySimulator(engine).simulate(
            initialState = engine.initialState(),
            durationSeconds = 24L * 60L * 60L,
            stepSeconds = 60L,
        )

        assertEquals(86_400L, report.simulatedSeconds)
        assertTrue(report.validationErrors.isEmpty())
        assertTrue(report.finalState.spaceDollars >= 0L)
        assertTrue(report.finalState.inventory.values.all { it >= 0L })
        assertTrue(report.finalState.deposits.values.all {
            it.remainingReserve >= 0L && it.pendingCollection >= 0L
        })
    }
}
