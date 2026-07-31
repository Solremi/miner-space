package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.presentation.PresentationSettings
import fr.solremi.minerspace.domain.presentation.VisualQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PresentationSettingsCodecTest {
    @Test
    fun `settings survive a complete round trip`() {
        val codec = PresentationSettingsCodec()
        val source = PresentationSettings(
            quality = VisualQuality.HIGH,
            effectsEnabled = false,
            reducedMotion = true,
            vibrationEnabled = false,
            soundEnabled = true,
            masterVolumePercent = 25,
        )
        val payload = codec.encode(source, 12_345L)
        assertEquals(PresentationSettingsCodec.SLOT_ID, payload.slotId)
        assertEquals(source, codec.decode(payload))
    }
}
