package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.presentation.PresentationSettings
import fr.solremi.minerspace.domain.presentation.VisualQuality
import fr.solremi.minerspace.domain.services.SavePayload

class PresentationSettingsCodec {
    fun encode(
        settings: PresentationSettings,
        savedAtEpochMillis: Long,
        slotId: String = SLOT_ID,
    ): SavePayload {
        require(savedAtEpochMillis >= 0L)
        val text = buildString {
            appendLine("format=$FORMAT_VERSION")
            appendLine("quality=${settings.quality.name}")
            appendLine("effectsEnabled=${settings.effectsEnabled}")
            appendLine("reducedMotion=${settings.reducedMotion}")
            appendLine("vibrationEnabled=${settings.vibrationEnabled}")
            appendLine("soundEnabled=${settings.soundEnabled}")
            appendLine("masterVolumePercent=${settings.masterVolumePercent}")
        }
        return SavePayload(
            slotId = slotId,
            schemaVersion = FORMAT_VERSION,
            contentVersion = CONTENT_VERSION,
            bytes = text.toByteArray(Charsets.UTF_8),
            savedAtEpochMillis = savedAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): PresentationSettings {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion == FORMAT_VERSION)
        val fields = payload.bytes.toString(Charsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Invalid presentation snapshot line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        require(fields.getValue("format").toInt() == FORMAT_VERSION)
        return PresentationSettings(
            quality = VisualQuality.valueOf(fields.getValue("quality")),
            effectsEnabled = fields.getValue("effectsEnabled").toBooleanStrict(),
            reducedMotion = fields.getValue("reducedMotion").toBooleanStrict(),
            vibrationEnabled = fields.getValue("vibrationEnabled").toBooleanStrict(),
            soundEnabled = fields.getValue("soundEnabled").toBooleanStrict(),
            masterVolumePercent = fields.getValue("masterVolumePercent").toInt(),
        )
    }

    companion object {
        const val SLOT_ID = "presentation"
        const val FORMAT_VERSION = 1
        const val CONTENT_VERSION = "0.12.0"
    }
}
