package fr.solremi.minerspace.data.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StrictJsonTest {
    @Test
    fun `parses nested unicode content`() {
        val root = StrictJson.parse(
            """{"name":"Ferrum \u0394","items":[1,true,null]}""",
        ).requireObject("root")

        assertEquals("Ferrum Δ", root.requireString("name"))
        assertEquals(3, root.requireArray("items").size)
    }

    @Test
    fun `rejects duplicate keys and fractional numbers`() {
        assertThrows(IllegalArgumentException::class.java) {
            StrictJson.parse("""{"id":1,"id":2}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            StrictJson.parse("""{"ratio":1.5}""")
        }
    }

    @Test
    fun `known key validation rejects accidental content fields`() {
        val root = StrictJson.parse(
            """{"schemaVersion":1,"contentVersion":"test","unexpected":true}""",
        ).requireObject("root")

        assertThrows(IllegalArgumentException::class.java) {
            root.requireKnownKeys("root", "schemaVersion", "contentVersion")
        }
    }
}
