package fr.solremi.minerspace.shared.diagnostics

import fr.solremi.minerspace.shared.SilentGameLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class GameDiagnosticsTest {
    @Test
    fun `ring buffer keeps the newest events in chronological order`() {
        val store = RingBufferGameDiagnosticStore(8)
        repeat(12) { index ->
            store.record(
                GameDiagnosticEvent(
                    timestampEpochMillis = index.toLong(),
                    level = DiagnosticLevel.INFO,
                    tag = "Test",
                    code = "log_${index.toString(16).padStart(16, '0')}",
                ),
            )
        }

        val snapshot = store.snapshot()
        assertEquals(8, snapshot.size)
        assertEquals(4L, snapshot.first().timestampEpochMillis)
        assertEquals(11L, snapshot.last().timestampEpochMillis)
    }

    @Test
    fun `logger stores only a fingerprint and exception class`() {
        val store = RingBufferGameDiagnosticStore()
        val logger = DiagnosticGameLogger(SilentGameLogger, store) { 42L }
        logger.error(
            "SaveService",
            "Unable to save /private/path/player-123 with balance 9999",
            IllegalStateException("secret message"),
        )

        val event = store.snapshot().single()
        assertEquals("SaveService", event.tag)
        assertEquals("java.lang.IllegalStateException", event.exceptionClass)
        assertFalse(event.code.contains("private"))
        assertEquals(20, event.code.length)
    }

    @Test
    fun `fingerprint is stable`() {
        assertEquals(
            DiagnosticGameLogger.fingerprint("same message"),
            DiagnosticGameLogger.fingerprint("same message"),
        )
    }
}
