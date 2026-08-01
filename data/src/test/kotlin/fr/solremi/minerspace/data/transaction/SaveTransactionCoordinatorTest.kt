package fr.solremi.minerspace.data.transaction

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaveTransactionCoordinatorTest {
    @Test
    fun `two slots are committed and journal is removed`() {
        val save = FakeSaveService()
        val result = SaveTransactionCoordinator(save).execute(
            transactionId = "transaction_1",
            mutations = listOf(write("primary", "economy"), write("sectors", "sector_state")),
            nowEpochMillis = 1_000L,
        )

        assertEquals(SaveTransactionStatus.COMMITTED, result.status)
        assertEquals("economy", save.text("primary"))
        assertEquals("sector_state", save.text("sectors"))
        assertFalse(save.contains(SaveTransactionCoordinator.JOURNAL_SLOT_ID))
    }

    @Test
    fun `partial write is resumed without rewriting completed slots`() {
        val save = FakeSaveService(failingSlot = "sectors")
        val coordinator = SaveTransactionCoordinator(save)
        val first = coordinator.execute(
            transactionId = "transaction_2",
            mutations = listOf(write("primary", "economy"), write("sectors", "sector_state")),
            nowEpochMillis = 2_000L,
        )

        assertEquals(SaveTransactionStatus.PENDING, first.status)
        assertTrue(save.contains(SaveTransactionCoordinator.JOURNAL_SLOT_ID))
        val primaryWrites = save.writeCount("primary")

        save.failingSlot = null
        val recovered = coordinator.recoverPending()

        assertEquals(SaveTransactionStatus.COMMITTED, recovered.status)
        assertEquals(primaryWrites, save.writeCount("primary"))
        assertEquals("sector_state", save.text("sectors"))
        assertFalse(save.contains(SaveTransactionCoordinator.JOURNAL_SLOT_ID))
    }

    @Test
    fun `clear mutation is idempotent during recovery`() {
        val save = FakeSaveService(failingSlot = "prestige")
        save.save(payload("primary", "old"))
        val coordinator = SaveTransactionCoordinator(save)

        val first = coordinator.execute(
            transactionId = "transaction_3",
            mutations = listOf(SaveMutation.clear("primary"), write("prestige", "cryos")),
            nowEpochMillis = 3_000L,
        )
        assertEquals(SaveTransactionStatus.PENDING, first.status)
        assertFalse(save.contains("primary"))

        save.failingSlot = null
        assertEquals(SaveTransactionStatus.COMMITTED, coordinator.recoverPending().status)
        assertFalse(save.contains("primary"))
        assertEquals("cryos", save.text("prestige"))
    }

    @Test
    fun `another transaction cannot replace a pending journal`() {
        val save = FakeSaveService(failingSlot = "sectors")
        val coordinator = SaveTransactionCoordinator(save)
        coordinator.execute("transaction_4", listOf(write("sectors", "one")), 4_000L)

        val blocked = coordinator.execute("transaction_5", listOf(write("sectors", "two")), 5_000L)

        assertEquals(SaveTransactionStatus.BUSY, blocked.status)
        assertEquals("transaction_4", blocked.transactionId)
    }

    private fun write(slotId: String, text: String): SaveMutation = SaveMutation.write(payload(slotId, text))

    private fun payload(slotId: String, text: String): SavePayload = SavePayload(
        slotId = slotId,
        schemaVersion = 1,
        contentVersion = "test",
        bytes = text.encodeToByteArray(),
        savedAtEpochMillis = 1L,
    )

    private class FakeSaveService(
        var failingSlot: String? = null,
    ) : SaveService {
        private val slots = linkedMapOf<String, SavePayload>()
        private val writes = linkedMapOf<String, Int>()

        override fun loadLatest(slotId: String): SavePayload? = slots[slotId]

        override fun save(payload: SavePayload): SaveWriteStatus {
            if (payload.slotId == failingSlot) return SaveWriteStatus.FAILED
            writes[payload.slotId] = writeCount(payload.slotId) + 1
            slots[payload.slotId] = payload.copy(sequence = (slots[payload.slotId]?.sequence ?: 0L) + 1L)
            return SaveWriteStatus.WRITTEN
        }

        override fun clear(slotId: String) {
            slots.remove(slotId)
        }

        fun contains(slotId: String): Boolean = slotId in slots
        fun text(slotId: String): String? = slots[slotId]?.bytes?.decodeToString()
        fun writeCount(slotId: String): Int = writes[slotId] ?: 0
    }
}
