package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class VersionedFieldsTest {
    @Test
    fun `writer and reader preserve typed values`() {
        val bytes = VersionedFieldWriter()
            .put("format", 2)
            .put("enabled", true)
            .put("quantity", 42L)
            .put("name", "miner")
            .encode()
        val reader = VersionedFieldReader.decode(bytes, "test")

        assertEquals(2, reader.int("format"))
        assertEquals(true, reader.boolean("enabled"))
        assertEquals(42L, reader.long("quantity"))
        assertEquals("miner", reader.string("name"))
    }

    @Test
    fun `duplicate and unknown fields are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            VersionedFieldReader.decode("format=1\nformat=2\n".toByteArray(), "test")
        }
        val reader = VersionedFieldReader.decode("format=1\nextra=x\n".toByteArray(), "test")
        assertThrows(IllegalArgumentException::class.java) {
            reader.requireOnly("format")
        }
    }

    @Test
    fun `game id collections round trip deterministically`() {
        val a = GameId.of("resource_a")
        val b = GameId.of("resource_b")
        assertEquals(
            setOf(a, b),
            SaveFieldCollections.decodeIds(SaveFieldCollections.encodeIds(setOf(b, a))),
        )
        assertEquals(
            mapOf(a to 2L, b to 4L),
            SaveFieldCollections.decodeQuantities(
                SaveFieldCollections.encodeQuantities(mapOf(b to 4L, a to 2L)),
            ),
        )
    }
}
