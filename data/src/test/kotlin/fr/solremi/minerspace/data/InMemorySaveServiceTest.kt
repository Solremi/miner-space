package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InMemorySaveServiceTest {
    @Test
    fun `save payload is copied to prevent external mutation`() {
        val source = byteArrayOf(1, 2, 3)
        val service = InMemorySaveService()

        val status = service.save(
            SavePayload(
                slotId = "primary",
                schemaVersion = 1,
                contentVersion = "0.1.0",
                bytes = source,
            ),
        )
        source[0] = 9

        assertEquals(SaveWriteStatus.WRITTEN, status)
        assertArrayEquals(byteArrayOf(1, 2, 3), service.loadLatest()!!.bytes)
    }
}
