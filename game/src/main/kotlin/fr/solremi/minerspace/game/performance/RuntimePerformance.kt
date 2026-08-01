package fr.solremi.minerspace.game.performance

import fr.solremi.minerspace.domain.presentation.VisualQuality

data class RuntimePerformanceBudget(
    val maxVisibleRobots: Int,
    val maxParticles: Int,
    val maxMeteorTrails: Int,
    val maxLoadedModels: Int,
    val maxTextureEdge: Int,
    val shaderPasses: Int,
    val assetMemoryBudgetBytes: Long,
) {
    init {
        require(maxVisibleRobots in 1..50)
        require(maxParticles >= 0 && maxMeteorTrails >= 0)
        require(maxLoadedModels > 0)
        require(maxTextureEdge in setOf(512, 1024, 2048, 4096))
        require(shaderPasses in 0..4)
        require(assetMemoryBudgetBytes > 0L)
    }
}

object RuntimeMemoryProfile {
    val memoryClassMb: Int
        get() = (Runtime.getRuntime().maxMemory() / (1024L * 1024L))
            .coerceAtLeast(32L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    val lowMemoryDevice: Boolean
        get() = memoryClassMb <= LOW_MEMORY_THRESHOLD_MB

    private const val LOW_MEMORY_THRESHOLD_MB = 192
}

/** Allocation-free quality governor fed by FrameTimeMonitor.record(). */
object AdaptiveRuntimeGovernor {
    private const val WINDOW_SIZE = 120
    private const val SLOW_FRAME_NANOS = 27_000_000L
    private const val RECOVERY_WINDOWS = 6
    private var sampleCount = 0
    private var slowFrameCount = 0
    private var healthyWindows = 0
    private var qualityPenalty = 0

    @Synchronized
    fun record(frameNanos: Long) {
        if (frameNanos < 0L) return
        sampleCount++
        if (frameNanos >= SLOW_FRAME_NANOS) slowFrameCount++
        if (sampleCount < WINDOW_SIZE) return
        val slowRatio = slowFrameCount.toDouble() / sampleCount
        when {
            slowRatio >= 0.15 -> {
                qualityPenalty = minOf(2, qualityPenalty + 1)
                healthyWindows = 0
            }
            slowRatio <= 0.02 -> {
                healthyWindows++
                if (healthyWindows >= RECOVERY_WINDOWS) {
                    qualityPenalty = maxOf(0, qualityPenalty - 1)
                    healthyWindows = 0
                }
            }
            else -> healthyWindows = 0
        }
        sampleCount = 0
        slowFrameCount = 0
    }

    @Synchronized
    fun effectiveQuality(selected: VisualQuality, lowMemoryDevice: Boolean): VisualQuality {
        var effective = if (lowMemoryDevice && selected == VisualQuality.HIGH) VisualQuality.MEDIUM else selected
        repeat(qualityPenalty) {
            effective = when (effective) {
                VisualQuality.HIGH -> VisualQuality.MEDIUM
                VisualQuality.MEDIUM -> VisualQuality.LOW
                VisualQuality.LOW -> VisualQuality.LOW
            }
        }
        return effective
    }

    @Synchronized
    internal fun resetForTests() {
        sampleCount = 0
        slowFrameCount = 0
        healthyWindows = 0
        qualityPenalty = 0
    }
}

object RuntimePerformanceBudgets {
    fun forQuality(
        quality: VisualQuality,
        lowMemoryDevice: Boolean = RuntimeMemoryProfile.lowMemoryDevice,
        adaptive: Boolean = true,
    ): RuntimePerformanceBudget {
        val effective = if (adaptive) {
            AdaptiveRuntimeGovernor.effectiveQuality(quality, lowMemoryDevice)
        } else if (lowMemoryDevice && quality == VisualQuality.HIGH) {
            VisualQuality.MEDIUM
        } else {
            quality
        }
        val base = when (effective) {
            VisualQuality.LOW -> RuntimePerformanceBudget(12, 40, 8, 24, 512, 0, 96L * 1024L * 1024L)
            VisualQuality.MEDIUM -> RuntimePerformanceBudget(28, 100, 16, 48, 1024, 1, 192L * 1024L * 1024L)
            VisualQuality.HIGH -> RuntimePerformanceBudget(50, 180, 28, 80, 2048, 2, 320L * 1024L * 1024L)
        }
        return if (!lowMemoryDevice) base else base.copy(
            maxVisibleRobots = minOf(base.maxVisibleRobots, 16),
            maxParticles = minOf(base.maxParticles, 48),
            maxMeteorTrails = minOf(base.maxMeteorTrails, 10),
            maxLoadedModels = minOf(base.maxLoadedModels, 28),
            maxTextureEdge = minOf(base.maxTextureEdge, 1024),
            shaderPasses = minOf(base.shaderPasses, 1),
            assetMemoryBudgetBytes = minOf(base.assetMemoryBudgetBytes, 128L * 1024L * 1024L),
        )
    }
}

data class FrameTimeSnapshot(
    val sampleCount: Int,
    val averageMillis: Double,
    val p95Millis: Double,
    val maximumMillis: Double,
    val slowFrameCount: Int,
)

class FrameTimeMonitor(
    capacity: Int = 240,
    private val slowFrameThresholdNanos: Long = 25_000_000L,
) {
    private val samples = LongArray(capacity.also { require(it in 30..3_600) })
    private var cursor = 0
    private var count = 0

    fun record(frameNanos: Long) {
        require(frameNanos >= 0L)
        samples[cursor] = frameNanos
        cursor = (cursor + 1) % samples.size
        if (count < samples.size) count++
        AdaptiveRuntimeGovernor.record(frameNanos)
    }

    fun snapshot(): FrameTimeSnapshot {
        if (count == 0) return FrameTimeSnapshot(0, 0.0, 0.0, 0.0, 0)
        val copy = LongArray(count)
        for (index in 0 until count) copy[index] = samples[index]
        copy.sort()
        val sum = copy.fold(0.0) { total, value -> total + value }
        val p95Index = ((count - 1) * .95).toInt().coerceIn(0, count - 1)
        return FrameTimeSnapshot(
            sampleCount = count,
            averageMillis = sum / count / 1_000_000.0,
            p95Millis = copy[p95Index] / 1_000_000.0,
            maximumMillis = copy.last() / 1_000_000.0,
            slowFrameCount = copy.count { it >= slowFrameThresholdNanos },
        )
    }
}
