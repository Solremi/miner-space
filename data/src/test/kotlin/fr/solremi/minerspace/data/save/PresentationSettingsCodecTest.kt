package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.presentation.ColorVisionMode
import fr.solremi.minerspace.domain.presentation.PresentationSettings
import fr.solremi.minerspace.domain.presentation.VisualQuality
import fr.solremi.minerspace.domain.services.SavePayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PresentationSettingsCodecTest {
    @Test
    fun `accessibility settings survive a complete round trip`() {
        val codec = PresentationSettingsCodec()
        val source = PresentationSettings(
            quality = VisualQuality.HIGH,
            effectsEnabled = false,
            reducedMotion = true,
            reducedFlashes = true,
            highContrast = true,
            colorVisionMode = ColorVisionMode.DEUTERANOPIA,
            textScalePercent = 130,
            vibrationEnabled = false,
            soundEnabled = true,
            masterVolumePercent = 25,
        )
        val payload = codec.encode(source, 12_345L)
        assertEquals(PresentationSettingsCodec.SLOT_ID, payload.slotId)
        assertEquals(PresentationSettingsCodec.FORMAT_VERSION, payload.schemaVersion)
        assertEquals(source, codec.decode(payload))
    }

    @Test
    fun `format one migrates with safe accessibility defaults`() {
        val bytes = """
            format=1
            quality=LOW
            effectsEnabled=true
            reducedMotion=true
            vibrationEnabled=false
            soundEnabled=true
            masterVolumePercent=50
        """.trimIndent().toByteArray()
        val legacy = SavePayload(
            slotId = PresentationSettingsCodec.SLOT_ID,
            schemaVersion = 1,
            contentVersion = "0.12.0",
            bytes = bytes,
            savedAtEpochMillis = 1L,
        )
        val migrated = PresentationSettingsCodec().decode(legacy)
        assertEquals(100, migrated.textScalePercent)
        assertEquals(false, migrated.highContrast)
        assertEquals(false, migrated.reducedFlashes)
        assertEquals(ColorVisionMode.DEFAULT, migrated.colorVisionMode)
        assertEquals(false, migrated.vibrationEnabled)
    }
}
