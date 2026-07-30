package fr.solremi.minerspace.data.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeteorContentLoaderTest {
    @Test
    fun `loads versioned meteor prototype content`() {
        val content = javaClass.getResource("/data/meteor-event.json")!!.readText()
        val definition = MeteorContentLoader().parse(content)
        assertEquals("0.7.0", definition.contentVersion)
        assertEquals(60_000L, definition.durationMillis)
        assertEquals(18, definition.maxActiveFragments)
        assertTrue(definition.assistedCaptureRadiusMillionths > definition.captureRadiusMillionths)
    }
}
