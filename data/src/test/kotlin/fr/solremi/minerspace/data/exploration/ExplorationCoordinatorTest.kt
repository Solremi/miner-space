package fr.solremi.minerspace.data.exploration

import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.SectorProgressCodec
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.domain.exploration.ExplorationDefinitions
import fr.solremi.minerspace.domain.exploration.ExplorationEngine
import fr.solremi.minerspace.domain.exploration.SectorBounds
import fr.solremi.minerspace.domain.exploration.SectorDefinition
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

class ExplorationCoordinatorTest {
    private val rawIron = GameId.of("raw_iron")
    private val powerCell = GameId.of("component_power_cell")
    private val rareReward = GameId.of("rare_prismatic_ferrite")
    private val deposit = GameId.of("deposit_iron_alpha")
    private val core = GameId.of("sector_core_delta")
    private val target = GameId.of("sector_copper_ridge")
    private val rareDeposit = GameId.of("rare_deposit_prismatic_ferrite")

    private val economyDefinitions = EconomyDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        resources = listOf(
            ResourceDefinition(rawIron, "raw.iron", 1L, 1_000L, true),
            ResourceDefinition(powerCell, "component.power", 1L, 100L, true),
            ResourceDefinition(rareReward, "rare.prismatic", 0L, 10L, false),
        ).associateBy { it.id },
        deposits = mapOf(
            deposit to DepositDefinition(
                id = deposit,
                resourceId = rawIron,
                initialReserve = 1_000L,
                extractionPerSecond = 1L,
                transportCapacity = 100L,
            ),
        ),
    )

    private val explorationDefinitions = ExplorationDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        sectors = listOf(
            SectorDefinition(
                id = core,
                nameKey = "sector.core",
                strategicReason = "Base",
                bounds = SectorBounds(0, 0, 100, 100),
                unlockCostSpaceDollars = 0L,
                scannerLevelRequired = 1,
                requiredSectorIds = emptySet(),
                requiredTechnologyIds = emptySet(),
                requiredComponents = emptyMap(),
                rareDepositId = null,
                missionTarget = true,
                initiallyUnlocked = true,
            ),
            SectorDefinition(
                id = target,
                nameKey = "sector.target",
                strategicReason = "Ressources",
                bounds = SectorBounds(120, 0, 100, 100),
                unlockCostSpaceDollars = 20L,
                scannerLevelRequired = 1,
                requiredSectorIds = setOf(core),
                requiredTechnologyIds = emptySet(),
                requiredComponents = mapOf(powerCell to 1L),
                rareDepositId = rareDeposit,
                missionTarget = false,
                initiallyUnlocked = false,
            ),
        ).associateBy { it.id },
    )

    @Test
    fun `sector opening commits economy and exploration together`() {
        val save = MemorySaveService()
        seed(save)
        val coordinator = coordinator(save)

        val result = coordinator.unlock(coordinator.load(), target)
        assertTrue(result is ExplorationActionResult.Applied)
        result as ExplorationActionResult.Applied

        assertEquals(80L, result.session.manufacturing.economy.spaceDollars)
        assertEquals(1L, result.session.manufacturing.economy.inventory.getValue(powerCell))
        assertEquals(1L, result.session.manufacturing.economy.inventory.getValue(rareReward))
        assertTrue(target in result.session.exploration.unlockedSectorIds)
        assertEquals(null, save.loadLatest(SaveTransactionCoordinator.JOURNAL_SLOT_ID))

        val persistedMain = ManufacturingSnapshotCodec().decode(save.loadLatest("primary")!!)
        val persistedExploration = SectorProgressCodec().decode(
            save.loadLatest(SectorProgressCodec.SLOT_ID)!!,
        )
        assertEquals(result.session.manufacturing, persistedMain)
        assertEquals(result.session.exploration, persistedExploration)
    }

    @Test
    fun `interrupted sector opening is resumed without duplicating rare reward`() {
        val save = MemorySaveService()
        seed(save)
        save.failNextWrites(SectorProgressCodec.SLOT_ID, 2)
        val coordinator = coordinator(save)

        val result = coordinator.unlock(coordinator.load(), target)
        assertTrue(result is ExplorationActionResult.TransactionPending)
        assertTrue(save.loadLatest(SaveTransactionCoordinator.JOURNAL_SLOT_ID) != null)

        save.failNextWrites(SectorProgressCodec.SLOT_ID, 0)
        val recovered = SaveTransactionCoordinator(save).recoverPending()
        assertEquals(SaveTransactionStatus.COMMITTED, recovered.status)
        assertEquals(
            SaveTransactionStatus.NO_PENDING,
            SaveTransactionCoordinator(save).recoverPending().status,
        )

        val persistedMain = ManufacturingSnapshotCodec().decode(save.loadLatest("primary")!!)
        val persistedExploration = SectorProgressCodec().decode(
            save.loadLatest(SectorProgressCodec.SLOT_ID)!!,
        )
        assertEquals(80L, persistedMain.economy.spaceDollars)
        assertEquals(1L, persistedMain.economy.inventory.getValue(powerCell))
        assertEquals(1L, persistedMain.economy.inventory.getValue(rareReward))
        assertTrue(target in persistedExploration.unlockedSectorIds)
    }

    private fun coordinator(save: SaveService): ExplorationCoordinator =
        ExplorationCoordinator(
            save = save,
            clock = FixedClock,
            logger = SilentGameLogger,
            economyDefinitions = economyDefinitions,
            definitions = explorationDefinitions,
        )

    private fun seed(save: SaveService) {
        val economy = CoreEconomyEngine(economyDefinitions).initialState(100L)
        val inventory = economy.inventory.toMutableMap().apply {
            this[powerCell] = 2L
        }
        val manufacturing = ManufacturingGameState(
            economy = economy.copy(inventory = inventory),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        val initialExploration = ExplorationEngine(explorationDefinitions).initialState()
        val revealedExploration = initialExploration.copy(
            revealedSectorIds = initialExploration.revealedSectorIds + target,
        )
        assertEquals(
            SaveWriteStatus.WRITTEN,
            save.save(
                ManufacturingSnapshotCodec().encode(
                    manufacturing,
                    economyDefinitions.contentVersion,
                    savedAtEpochMillis = FixedClock.nowEpochMillis(),
                ),
            ),
        )
        assertEquals(
            SaveWriteStatus.WRITTEN,
            save.save(
                SectorProgressCodec().encode(
                    revealedExploration,
                    explorationDefinitions.contentVersion,
                    FixedClock.nowEpochMillis(),
                ),
            ),
        )
    }

    private object FixedClock : ClockService {
        override fun nowEpochMillis(): Long = 10_000L
        override fun monotonicMillis(): Long = 10_000L
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
