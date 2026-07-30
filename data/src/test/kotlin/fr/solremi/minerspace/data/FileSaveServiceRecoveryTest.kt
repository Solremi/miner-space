package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileSaveServiceRecoveryTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `corrupted newest snapshot restores the previous valid copy`() {
        val service = FileSaveService(directory)
        assertEquals(SaveWriteStatus.WRITTEN, service.save(payload("first", 1_000L)))
        assertEquals(SaveWriteStatus.WRITTEN, service.save(payload("second", 2_000L)))

        Files.write(directory.resolve("primary.b.msv"), byteArrayOf(1, 2, 3, 4))

        val restored = service.loadLatest()!!
        assertArrayEquals("first".toByteArray(), restored.bytes)
        assertEquals(1_000L, restored.savedAtEpochMillis)
        assertTrue(restored.recoveredFromFallback)
    }

    @Test
    fun `two alternating snapshots remain readable`() {
        val service = FileSaveService(directory)
        service.save(payload("one", 1_000L))
        service.save(payload("two", 2_000L))
        service.save(payload("three", 3_000L))

        val latest = service.loadLatest()!!
        assertArrayEquals("three".toByteArray(), latest.bytes)
        assertEquals(3L, latest.sequence)
        assertTrue(Files.exists(directory.resolve("primary.a.msv")))
        assertTrue(Files.exists(directory.resolve("primary.b.msv")))
    }

    private fun payload(value: String, savedAt: Long) = SavePayload(
        slotId = "primary",
        schemaVersion = 3,
        contentVersion = "0.5.0-test",
        bytes = value.toByteArray(),
        savedAtEpochMillis = savedAt,
    )
}
