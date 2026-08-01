package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class CoalescingSaveServiceTest {
    @Test
    fun `latest deferred payload wins for the same slot`() {
        val delegate = MemorySaveService()
        val executor = Executors.newSingleThreadExecutor()
        val service = CoalescingSaveService(delegate, executor)

        assertTrue(service.enqueue(payload("robots", "one", 1L)))
        assertTrue(service.enqueue(payload("robots", "two", 2L)))
        assertTrue(service.flush(2_000L))

        assertEquals("two", delegate.loadLatest("robots")!!.bytes.toString(Charsets.UTF_8))
        service.close()
    }

    @Test
    fun `synchronous transaction save supersedes an older deferred autosave`() {
        val delegate = MemorySaveService()
        val executor = Executors.newSingleThreadExecutor()
        val service = CoalescingSaveService(delegate, executor)

        service.enqueue(payload("primary", "old", 1L))
        assertEquals(SaveWriteStatus.WRITTEN, service.save(payload("primary", "critical", 2L)))
        service.flush(2_000L)

        assertEquals("critical", delegate.loadLatest("primary")!!.bytes.toString(Charsets.UTF_8))
        service.close()
    }

    @Test
    fun `clear invalidates a queued autosave`() {
        val delegate = MemorySaveService()
        val executor = Executors.newSingleThreadExecutor()
        val service = CoalescingSaveService(delegate, executor)

        service.enqueue(payload("presentation", "queued", 1L))
        service.clear("presentation")
        service.flush(2_000L)

        assertNull(delegate.loadLatest("presentation"))
        service.close()
    }

    private fun payload(slotId: String, value: String, time: Long) = SavePayload(
        slotId = slotId,
        schemaVersion = 1,
        contentVersion = "test",
        bytes = value.toByteArray(),
        savedAtEpochMillis = time,
    )

    private class MemorySaveService : SaveService {
        private val values = linkedMapOf<String, SavePayload>()

        @Synchronized
        override fun loadLatest(slotId: String): SavePayload? = values[slotId]

        @Synchronized
        override fun save(payload: SavePayload): SaveWriteStatus {
            val sequence = (values[payload.slotId]?.sequence ?: 0L) + 1L
            values[payload.slotId] = payload.copy(sequence = sequence)
            return SaveWriteStatus.WRITTEN
        }

        @Synchronized
        override fun clear(slotId: String) {
            values.remove(slotId)
        }
    }
}
