package fr.solremi.minerspace.domain.event

import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeteorEventEngineTest {
    private val standard = GameId.of("meteor_fragment_standard")
    private val rare = GameId.of("rare_meteor_core")
    private val definition = MeteorEventDefinition(
        schemaVersion = 1,
        contentVersion = "0.7.0",
        durationMillis = 60_000L,
        spawnIntervalMillis = 1_400L,
        maxActiveFragments = 18,
        fragmentLifetimeMillis = 6_500L,
        rareSpawnAtMillis = 30_000L,
        standardResourceId = standard,
        rareResourceId = rare,
        standardRewardPerFragment = 1L,
        rareRewardQuantity = 1L,
        captureRadiusMillionths = 55_000,
        assistedCaptureRadiusMillionths = 85_000,
        assistAutoCollectIntervalMillis = 2_500L,
    )

    @Test
    fun `sixty second event stays capped and reaches summary`() {
        val engine = MeteorEventEngine(definition)
        var state = engine.start("meteor_test", 42L, assistanceEnabled = false)
        repeat(600) {
            state = engine.advance(state, 100L)
            assertTrue(state.fragments.size <= definition.maxActiveFragments)
        }
        assertEquals(MeteorEventPhase.SUMMARY, state.phase)
        assertTrue(state.rareSpawned)
        assertTrue(state.fragments.isEmpty())
    }

    @Test
    fun `assistance keeps interaction accessible and can collect rare fragment`() {
        val engine = MeteorEventEngine(definition)
        var state = engine.start("meteor_assist", 99L, assistanceEnabled = true)
        repeat(400) { state = engine.advance(state, 100L) }
        assertTrue(state.standardCollected > 0L)
        assertEquals(1L, state.rareCollected)
        assertTrue(MeteorEventEngine.CODEX_RARE in state.codexEntryIds)
    }

    @Test
    fun `captured fragment cannot be collected twice`() {
        val engine = MeteorEventEngine(definition)
        var state = engine.start("meteor_capture", 7L, assistanceEnabled = false)
        state = engine.advance(state, 1_500L)
        val fragment = state.fragments.first()
        val point = engine.position(fragment, state.elapsedActiveMillis)
        val first = engine.capture(state, point.xMillionths, point.yMillionths)
        val second = engine.capture(first.state, point.xMillionths, point.yMillionths)
        assertEquals(MeteorFragmentKind.STANDARD, first.captured)
        assertEquals(null, second.captured)
        assertEquals(1L, first.state.standardCollected)
        assertEquals(1L, second.state.standardCollected)
    }

    @Test
    fun `two phase reward recovers both crash windows without duplication`() {
        val engine = MeteorEventEngine(definition)
        val reward = MeteorRewardEngine(definition, mapOf(standard to 5_000L, rare to 50L))
        val summary = engine.start("meteor_reward", 1L).copy(
            phase = MeteorEventPhase.SUMMARY,
            elapsedActiveMillis = 60_000L,
            standardCollected = 12L,
            rareCollected = 1L,
        )
        val initial = ManufacturingGameState(
            economy = EconomyState(
                inventory = mapOf(standard to 3L, rare to 0L),
                deposits = emptyMap(),
                spaceDollars = 0L,
                transactionSequence = 0L,
            ),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        val prepared = reward.prepare(initial, summary)!!
        assertEquals(15L, prepared.event.expectedStandardInventory)
        assertEquals(1L, prepared.event.expectedRareInventory)

        val crashBeforeMain = reward.reconcile(initial, prepared.event) as MeteorRewardResult.Applied
        assertTrue(crashBeforeMain.stateChanged)
        assertEquals(15L, crashBeforeMain.state.economy.inventory.getValue(standard))
        assertEquals(1L, crashBeforeMain.state.economy.inventory.getValue(rare))

        val crashAfterMain = reward.reconcile(prepared.state, prepared.event) as MeteorRewardResult.Applied
        assertFalse(crashAfterMain.stateChanged)
        assertEquals(15L, crashAfterMain.state.economy.inventory.getValue(standard))
        assertEquals(MeteorEventPhase.COMMITTED, crashAfterMain.event.phase)
    }
}
