package fr.solremi.minerspace.domain.economy

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreEconomyEngineTest {
    private val iron = GameId.of("raw_iron")
    private val deposit = GameId.of("deposit_iron_alpha")
    private val definitions = EconomyDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        resources = mapOf(
            iron to ResourceDefinition(
                id = iron,
                nameKey = "resource.raw_iron",
                unitSalePrice = 2L,
                storageCapacity = 1_000L,
                sellable = true,
            ),
        ),
        deposits = mapOf(
            deposit to DepositDefinition(
                id = deposit,
                resourceId = iron,
                initialReserve = 10_000L,
                extractionPerSecond = 3L,
                transportCapacity = 300L,
            ),
        ),
    )
    private val engine = CoreEconomyEngine(definitions)

    @Test
    fun `extract collect and sell are atomic without duplication`() {
        var state = engine.advanceExtraction(engine.initialState(), 60L).state
        assertEquals(180L, state.deposits.getValue(deposit).pendingCollection)
        assertEquals(9_820L, state.deposits.getValue(deposit).remainingReserve)

        val collected = engine.collect(state, deposit)
        assertTrue(collected is EconomyCommandResult.Applied)
        state = collected.state
        assertEquals(180L, state.inventory.getValue(iron))
        assertEquals(0L, state.deposits.getValue(deposit).pendingCollection)

        val duplicateCollection = engine.collect(state, deposit)
        assertTrue(duplicateCollection is EconomyCommandResult.Rejected)
        assertEquals(state, duplicateCollection.state)

        val sold = engine.sell(state, iron, 180L)
        assertTrue(sold is EconomyCommandResult.Applied)
        state = sold.state
        assertEquals(0L, state.inventory.getValue(iron))
        assertEquals(360L, state.spaceDollars)
        assertTrue(engine.validationErrors(state).isEmpty())
    }

    @Test
    fun `fixed point production floors only once`() {
        assertEquals(4L, FixedPointMath.floorMultiply(5L, 999_999L))
        assertEquals(5L, FixedPointMath.floorMultiply(5L, MULTIPLIER_SCALE))
    }
}
