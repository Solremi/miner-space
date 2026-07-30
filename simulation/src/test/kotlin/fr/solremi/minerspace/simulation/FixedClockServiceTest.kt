package fr.solremi.minerspace.simulation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FixedClockServiceTest {
    @Test
    fun `clock advances deterministically`() {
        val clock = FixedClockService(initialEpochMillis = 1_000L)

        clock.advanceBy(250L)

        assertEquals(1_250L, clock.nowEpochMillis())
        assertEquals(250L, clock.monotonicMillis())
    }

    @Test
    fun `clock refuses negative durations`() {
        val clock = FixedClockService()

        assertThrows(IllegalArgumentException::class.java) {
            clock.advanceBy(-1L)
        }
    }
}
