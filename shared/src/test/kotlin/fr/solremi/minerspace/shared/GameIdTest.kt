package fr.solremi.minerspace.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameIdTest {
    @Test
    fun `accepts stable lowercase identifiers`() {
        assertEquals("raw_iron_ore", GameId.of("raw_iron_ore").value)
    }

    @Test
    fun `rejects display names as identifiers`() {
        assertThrows(IllegalArgumentException::class.java) {
            GameId.of("Iron Ore")
        }
    }
}
