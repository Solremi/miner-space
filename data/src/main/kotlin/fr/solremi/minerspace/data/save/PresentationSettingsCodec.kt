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
        val bytes = VersionedFieldWriter()
            .put("format", FORMAT_VERSION)
            .put("quality", settings.quality.name)
            .put("effectsEnabled", settings.effectsEnabled)
            .put("reducedMotion", settings.reducedMotion)
            .put("reducedFlashes", settings.reducedFlashes)
            .put("highContrast", settings.highContrast)
            .put("colorVisionMode", settings.colorVisionMode.name)
            .put("textScalePercent", settings.textScalePercent)
            .put("vibrationEnabled", settings.vibrationEnabled)
            .put("soundEnabled", settings.soundEnabled)
            .put("masterVolumePercent", settings.masterVolumePercent)
            .encode()
        return SavePayload(
            slotId = slotId,
            schemaVersion = FORMAT_VERSION,
            contentVersion = CONTENT_VERSION,
            bytes = bytes,
            savedAtEpochMillis = savedAtEpochMillis,
        )
    }

    fun decode(payload: SavePayload): PresentationSettings {
        require(payload.slotId == SLOT_ID)
        require(payload.schemaVersion in SUPPORTED_FORMATS)
        val fields = VersionedFieldReader.decode(payload.bytes, "presentation snapshot")
        val format = fields.int("format")
        require(format in SUPPORTED_FORMATS)
        return when (format) {
            1 -> decodeV1(fields)
            FORMAT_VERSION -> decodeV2(fields)
            else -> error("Unsupported presentation format $format")
        }
    }

    private fun decodeV1(fields: VersionedFieldReader): PresentationSettings {
        fields.requireOnly(
            "format",
            "quality",
            "effectsEnabled",
            "reducedMotion",
            "vibrationEnabled",
            "soundEnabled",
            "masterVolumePercent",
        )
        return PresentationSettings(
            quality = fields.enum<VisualQuality>("quality"),
            effectsEnabled = fields.boolean("effectsEnabled"),
            reducedMotion = fields.boolean("reducedMotion"),
            vibrationEnabled = fields.boolean("vibrationEnabled"),
            soundEnabled = fields.boolean("soundEnabled"),
            masterVolumePercent = fields.int("masterVolumePercent"),
        )
    }

    private fun decodeV2(fields: VersionedFieldReader): PresentationSettings {
        fields.requireOnly(
            "format",
            "quality",
            "effectsEnabled",
            "reducedMotion",
            "reducedFlashes",
            "highContrast",
            "colorVisionMode",
            "textScalePercent",
            "vibrationEnabled",
            "soundEnabled",
            "masterVolumePercent",
        )
        return PresentationSettings(
            quality = fields.enum<VisualQuality>("quality"),
            effectsEnabled = fields.boolean("effectsEnabled"),
            reducedMotion = fields.boolean("reducedMotion"),
            reducedFlashes = fields.boolean("reducedFlashes"),
            highContrast = fields.boolean("highContrast"),
            colorVisionMode = fields.enum<ColorVisionMode>("colorVisionMode"),
            textScalePercent = fields.int("textScalePercent"),
            vibrationEnabled = fields.boolean("vibrationEnabled"),
            soundEnabled = fields.boolean("soundEnabled"),
            masterVolumePercent = fields.int("masterVolumePercent"),
        )
    }

    companion object {
        const val SLOT_ID = "presentation"
        const val FORMAT_VERSION = 2
        const val CONTENT_VERSION = "1.0.0"
        val SUPPORTED_FORMATS = 1..FORMAT_VERSION
    }
}
