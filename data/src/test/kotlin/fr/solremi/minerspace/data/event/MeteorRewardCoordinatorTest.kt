package fr.solremi.minerspace.data.event

import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.MeteorEventCodec
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.domain.event.MeteorEventDefinition
import fr.solremi.minerspace.domain.event.MeteorEventPhase
import fr.solremi.minerspace.domain.event.MeteorEventState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.SilentGameLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeteorRewardCoordinatorTest {
    private val standard = GameId.of("meteor_fragment")
    private val rare = GameId.of("meteor_core")
    private val deposit = GameId.of("deposit_meteor_test")

    private val economyDefinitions = EconomyDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        resources = mapOf(
            standard to ResourceDefinition(standard, "meteor.fragment", 1L, 1_000L, true),
            rare to ResourceDefinition(rare, "meteor.core", 0L, 100L, false),
        ),
        deposits = mapOf(
            deposit to DepositDefinition(
                id = deposit,
                resourceId = standard,
                initialReserve = 1_000L,
                extractionPerSecond = 1L,
                transportCapacity = 100L,
            ),
        ),
    )

    private val definition = MeteorEventDefinition(
        schemaVersion = 1,
        contentVersion = "test",
        durationMillis = 45_000L,
        spawnIntervalMillis = 1_000L,
        maxActiveFragments = 12,
        fragmentLifetimeMillis = 5_000L,
        rareSpawnAtMillis = 20_000L,
        standardResourceId = standard,
        rareResourceId = rare,
        standardRewardPerFragment = 3L,
        rareRewardQuantity = 2L,
        captureRadiusMillionths = 50_000,
        assistedCaptureRadiusMillionths = 75_000,
        assistAutoCollectIntervalMillis = 2_000L,
    )

    @Test
    fun `interrupted meteor reward resumes without duplicating inventory`() {
        val save = MemorySaveService()
        val main = ManufacturingGameState(
            economy = CoreEconomyEngine(economyDefinitions).initialState(),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        val event = summaryEvent()
        seed(save, main, event)
        save.failNextWrites(MeteorEventCodec.SLOT_ID, 2)
        val coordinator = MeteorRewardCoordinator(
            save = save,
            clock = FixedClock,
            logger = SilentGameLogger,
            definition = definition,
            economyDefinitions = economyDefinitions,
        )

        val first = coordinator.commit(main, event)
        assertTrue(first is MeteorRewardCommitResult.Pending)
        assertTrue(save.loadLatest(SaveTransactionCoordinator.JOURNAL_SLOT_ID) != null)

        save.failNextWrites(MeteorEventCodec.SLOT_ID, 0)
        assertEquals(
            SaveTransactionStatus.COMMITTED,
            SaveTransactionCoordinator(save).recoverPending().status,
        )
        assertEquals(
            SaveTransactionStatus.NO_PENDING,
            SaveTransactionCoordinator(save).recoverPending().status,
        )

        val persistedMain = ManufacturingSnapshotCodec().decode(save.loadLatest()!!)
        val persistedEvent = MeteorEventCodec().decode(save.loadLatest(MeteorEventCodec.SLOT_ID)!!)
        assertEquals(6L, persistedMain.economy.inventory.getValue(standard))
        assertEquals(2L, persistedMain.economy.inventory.getValue(rare))
        assertEquals(MeteorEventPhase.COMMITTED, persistedEvent.phase)
    }

    @Test
    fun `legacy committing event is finalized transactionally`() {
        val save = MemorySaveService()
        val main = ManufacturingGameState(
            economy = CoreEconomyEngine(economyDefinitions).initialState(),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        val prepared = summaryEvent().copy(
            phase = MeteorEventPhase.COMMITTING,
            expectedStandardInventory = 6L,
            expectedRareInventory = 2L,
        )
        seed(save, main, prepared)
        val coordinator = MeteorRewardCoordinator(
            save = save,
            clock = FixedClock,
            logger = SilentGameLogger,
            definition = definition,
            economyDefinitions = economyDefinitions,
        )

        val result = coordinator.commit(main, prepared)

        assertTrue(result is MeteorRewardCommitResult.Committed)
        result as MeteorRewardCommitResult.Committed
        assertEquals(6L, result.main.economy.inventory.getValue(standard))
        assertEquals(2L, result.main.economy.inventory.getValue(rare))
        assertEquals(MeteorEventPhase.COMMITTED, result.event.phase)
    }

    private fun summaryEvent(): MeteorEventState = MeteorEventState(
        eventId = "meteor_test",
        seed = 1L,
        phase = MeteorEventPhase.SUMMARY,
        elapsedActiveMillis = definition.durationMillis,
        nextSpawnIndex = 10L,
        rareSpawned = true,
        fragments = emptyList(),
        standardCollected = 2L,
        rareCollected = 1L,
        assistanceEnabled = true,
        lastAssistAtMillis = 0L,
        expectedStandardInventory = null,
        expectedRareInventory = null,
        codexEntryIds = emptySet(),
        transactionSequence = 4L,
    )

    private fun seed(
        save: SaveService,
        main: ManufacturingGameState,
        event: MeteorEventState,
    ) {
        assertEquals(
            SaveWriteStatus.WRITTEN,
            save.save(
                ManufacturingSnapshotCodec().encode(
                    state = main,
                    contentVersion = economyDefinitions.contentVersion,
                    savedAtEpochMillis = FixedClock.nowEpochMillis(),
                ),
            ),
        )
        assertEquals(
            SaveWriteStatus.WRITTEN,
            save.save(
                MeteorEventCodec().encode(
                    state = event,
                    contentVersion = definition.contentVersion,
                    savedAtEpochMillis = FixedClock.nowEpochMillis(),
                ),
            ),
        )
    }

    private object FixedClock : ClockService {
        override fun nowEpochMillis(): Long = 20_000L
        override fun monotonicMillis(): Long = 20_000L
    }

    private class MemorySaveService : SaveService {
        private val payloads = linkedMapOf<String, SavePayload>()
        private val failures = mutableMapOf<String, Int>()

        fun failNextWrites(slotId: String, count: Int) {
            failures[slotId] = count.coerceAtLeast(0)
        }

        override fun loadLatest(slotId: String): SavePayload? = payloads[slotId]

        override fun save(payload: SavePayload): SaveWriteStatus {
            val remaining = failures[payload.slotId] ?: 0
            if (remaining > 0) {
                failures[payload.slotId] = remaining - 1
                return SaveWriteStatus.FAILED
            }
            val sequence = (payloads[payload.slotId]?.sequence ?: 0L) + 1L
            payloads[payload.slotId] = payload.copy(sequence = sequence)
            return SaveWriteStatus.WRITTEN
        }

        override fun clear(slotId: String) {
            payloads.remove(slotId)
        }
    }
}
