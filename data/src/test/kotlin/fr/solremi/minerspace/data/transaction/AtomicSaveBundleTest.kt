package fr.solremi.minerspace.data.transaction

import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.SilentGameLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AtomicSaveBundleTest {
    @Test
    fun `interrupted two slot bundle resumes without replacing the committed first slot`() {
        val save = MemorySaveService()
        val first = payload("primary", "first")
        val second = payload("robots", "second")
        save.failNextWrites("robots", 2)
        val bundle = AtomicSaveBundle(save, FixedClock, SilentGameLogger)

        val pending = bundle.commit(
            namespace = "robots_upgrade",
            primarySequence = 4L,
            secondarySequence = 7L,
            payloads = listOf(first, second),
        )

        assertFalse(pending.committed)
        assertEquals(SaveTransactionStatus.PENDING, pending.transaction.status)
        assertNotNull(save.loadLatest(SaveTransactionCoordinator.JOURNAL_SLOT_ID))
        val firstSequence = save.loadLatest("primary")!!.sequence

        save.failNextWrites("robots", 0)
        val recovered = SaveTransactionCoordinator(save).recoverPending()

        assertEquals(SaveTransactionStatus.COMMITTED, recovered.status)
        assertEquals(firstSequence, save.loadLatest("primary")!!.sequence)
        assertTrue(save.loadLatest("primary")!!.bytes.contentEquals(first.bytes))
        assertTrue(save.loadLatest("robots")!!.bytes.contentEquals(second.bytes))
        assertNull(save.loadLatest(SaveTransactionCoordinator.JOURNAL_SLOT_ID))
    }

    private fun payload(slot: String, text: String) = SavePayload(
        slotId = slot,
        schemaVersion = 1,
        contentVersion = "test",
        bytes = text.toByteArray(),
        savedAtEpochMillis = FixedClock.nowEpochMillis(),
    )

    private object FixedClock : ClockService {
        override fun nowEpochMillis(): Long = 42_000L
        override fun monotonicMillis(): Long = 42_000L
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
