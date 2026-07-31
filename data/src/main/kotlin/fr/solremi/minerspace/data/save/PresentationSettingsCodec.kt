package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.presentation.ColorVisionMode
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
            appendLine("reducedFlashes=${settings.reducedFlashes}")
            appendLine("highContrast=${settings.highContrast}")
            appendLine("colorVisionMode=${settings.colorVisionMode.name}")
            appendLine("textScalePercent=${settings.textScalePercent}")
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
        require(payload.schemaVersion in SUPPORTED_FORMATS)
        val fields = payload.bytes.toString(Charsets.UTF_8)
            .lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val separator = line.indexOf('=')
                require(separator > 0) { "Invalid presentation snapshot line" }
                line.substring(0, separator) to line.substring(separator + 1)
            }
        val format = fields.getValue("format").toInt()
        require(format in SUPPORTED_FORMATS)
        return when (format) {
            1 -> decodeV1(fields)
            FORMAT_VERSION -> decodeV2(fields)
            else -> error("Unsupported presentation format $format")
        }
    }

    private fun decodeV1(fields: Map<String, String>) = PresentationSettings(
        quality = VisualQuality.valueOf(fields.getValue("quality")),
        effectsEnabled = fields.getValue("effectsEnabled").toBooleanStrict(),
        reducedMotion = fields.getValue("reducedMotion").toBooleanStrict(),
        vibrationEnabled = fields.getValue("vibrationEnabled").toBooleanStrict(),
        soundEnabled = fields.getValue("soundEnabled").toBooleanStrict(),
        masterVolumePercent = fields.getValue("masterVolumePercent").toInt(),
    )

    private fun decodeV2(fields: Map<String, String>) = PresentationSettings(
        quality = VisualQuality.valueOf(fields.getValue("quality")),
        effectsEnabled = fields.getValue("effectsEnabled").toBooleanStrict(),
        reducedMotion = fields.getValue("reducedMotion").toBooleanStrict(),
        reducedFlashes = fields.getValue("reducedFlashes").toBooleanStrict(),
        highContrast = fields.getValue("highContrast").toBooleanStrict(),
        colorVisionMode = ColorVisionMode.valueOf(fields.getValue("colorVisionMode")),
        textScalePercent = fields.getValue("textScalePercent").toInt(),
        vibrationEnabled = fields.getValue("vibrationEnabled").toBooleanStrict(),
        soundEnabled = fields.getValue("soundEnabled").toBooleanStrict(),
        masterVolumePercent = fields.getValue("masterVolumePercent").toInt(),
    )

    companion object {
        const val SLOT_ID = "presentation"
        const val FORMAT_VERSION = 2
        const val CONTENT_VERSION = "1.0.0"
        val SUPPORTED_FORMATS = 1..FORMAT_VERSION
    }
}
