package fr.solremi.minerspace.domain.presentation

enum class VisualQuality { LOW, MEDIUM, HIGH }

enum class FeedbackKind { INTERACTION, SUCCESS, ERROR, RARE, SECTOR_OPEN, PRODUCTION, LAUNCH }

data class PresentationSettings(
    val quality: VisualQuality = VisualQuality.MEDIUM,
    val effectsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val masterVolumePercent: Int = 75,
)

data class VisualBudget(
    val dustParticles: Int,
    val robotUnits: Int,
    val meteorTrails: Int,
    val sparkParticles: Int,
    val shaderPasses: Int,
)

class PresentationSettingsEngine {
    fun normalize(source: PresentationSettings): PresentationSettings = source.copy(
        masterVolumePercent = source.masterVolumePercent.coerceIn(0, 100),
    )

    fun cycleQuality(source: PresentationSettings): PresentationSettings {
        val values = VisualQuality.entries
        return normalize(source.copy(quality = values[(source.quality.ordinal + 1) % values.size]))
    }

    fun cycleVolume(source: PresentationSettings): PresentationSettings {
        val values = intArrayOf(0, 25, 50, 75, 100)
        val current = normalize(source).masterVolumePercent
        val next = values.firstOrNull { it > current } ?: values.first()
        return source.copy(masterVolumePercent = next)
    }

    fun visualBudget(source: PresentationSettings): VisualBudget {
        val settings = normalize(source)
        if (!settings.effectsEnabled) return VisualBudget(0, if (settings.quality == VisualQuality.LOW) 4 else 8, 0, 0, 0)
        val base = when (settings.quality) {
            VisualQuality.LOW -> VisualBudget(8, 8, 2, 8, 0)
            VisualQuality.MEDIUM -> VisualBudget(18, 16, 4, 18, 1)
            VisualQuality.HIGH -> VisualBudget(32, 28, 7, 32, 1)
        }
        return if (settings.reducedMotion) {
            base.copy(
                dustParticles = minOf(base.dustParticles, 6),
                robotUnits = minOf(base.robotUnits, 10),
                meteorTrails = minOf(base.meteorTrails, 1),
                sparkParticles = minOf(base.sparkParticles, 6),
            )
        } else base
    }

    fun normalizedVolume(source: PresentationSettings): Float =
        normalize(source).masterVolumePercent / 100f
}
