package fr.solremi.minerspace.domain.presentation

enum class VisualQuality { LOW, MEDIUM, HIGH }
enum class ColorVisionMode { DEFAULT, DEUTERANOPIA, PROTANOPIA, TRITANOPIA, MONOCHROME }

enum class FeedbackKind { INTERACTION, SUCCESS, ERROR, RARE, SECTOR_OPEN, PRODUCTION, LAUNCH }

data class PresentationSettings(
    val quality: VisualQuality = VisualQuality.MEDIUM,
    val effectsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val reducedFlashes: Boolean = false,
    val highContrast: Boolean = false,
    val colorVisionMode: ColorVisionMode = ColorVisionMode.DEFAULT,
    val textScalePercent: Int = 100,
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
        textScalePercent = nearest(TEXT_SCALE_STEPS, source.textScalePercent.coerceIn(TEXT_SCALE_STEPS.first(), TEXT_SCALE_STEPS.last())),
        masterVolumePercent = source.masterVolumePercent.coerceIn(0, 100),
    )

    fun cycleQuality(source: PresentationSettings): PresentationSettings {
        val values = VisualQuality.entries
        return normalize(source.copy(quality = values[(source.quality.ordinal + 1) % values.size]))
    }

    fun cycleColorVisionMode(source: PresentationSettings): PresentationSettings {
        val values = ColorVisionMode.entries
        return normalize(source.copy(colorVisionMode = values[(source.colorVisionMode.ordinal + 1) % values.size]))
    }

    fun cycleTextScale(source: PresentationSettings): PresentationSettings {
        val current = normalize(source).textScalePercent
        val index = TEXT_SCALE_STEPS.indexOf(current).coerceAtLeast(0)
        return normalize(source.copy(textScalePercent = TEXT_SCALE_STEPS[(index + 1) % TEXT_SCALE_STEPS.size]))
    }

    fun cycleVolume(source: PresentationSettings): PresentationSettings {
        val current = normalize(source).masterVolumePercent
        val next = VOLUME_STEPS.firstOrNull { it > current } ?: VOLUME_STEPS.first()
        return normalize(source.copy(masterVolumePercent = next))
    }

    fun visualBudget(source: PresentationSettings): VisualBudget {
        val settings = normalize(source)
        if (!settings.effectsEnabled) return VisualBudget(0, if (settings.quality == VisualQuality.LOW) 4 else 8, 0, 0, 0)
        var budget = when (settings.quality) {
            VisualQuality.LOW -> VisualBudget(8, 8, 2, 8, 0)
            VisualQuality.MEDIUM -> VisualBudget(18, 16, 4, 18, 1)
            VisualQuality.HIGH -> VisualBudget(32, 28, 7, 32, 1)
        }
        if (settings.reducedMotion) {
            budget = budget.copy(
                dustParticles = minOf(budget.dustParticles, 6),
                robotUnits = minOf(budget.robotUnits, 10),
                meteorTrails = minOf(budget.meteorTrails, 1),
                sparkParticles = minOf(budget.sparkParticles, 6),
            )
        }
        if (settings.reducedFlashes) {
            budget = budget.copy(
                sparkParticles = minOf(budget.sparkParticles, 3),
                shaderPasses = 0,
            )
        }
        return budget
    }

    fun normalizedVolume(source: PresentationSettings): Float = normalize(source).masterVolumePercent / 100f
    fun fontScale(source: PresentationSettings): Float = normalize(source).textScalePercent / 100f
    fun feedbackAlpha(source: PresentationSettings): Float = if (normalize(source).reducedFlashes) .18f else .55f

    private fun nearest(values: IntArray, value: Int): Int = values.minBy { kotlin.math.abs(it - value) }

    private companion object {
        val TEXT_SCALE_STEPS = intArrayOf(100, 115, 130)
        val VOLUME_STEPS = intArrayOf(0, 25, 50, 75, 100)
    }
}
