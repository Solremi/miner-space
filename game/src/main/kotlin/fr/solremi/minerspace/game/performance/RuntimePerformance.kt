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

object RuntimePerformanceBudgets {
    fun forQuality(
        quality: VisualQuality,
        lowMemoryDevice: Boolean = false,
    ): RuntimePerformanceBudget {
        val base = when (quality) {
            VisualQuality.LOW -> RuntimePerformanceBudget(
                maxVisibleRobots = 12,
                maxParticles = 40,
                maxMeteorTrails = 8,
                maxLoadedModels = 24,
                maxTextureEdge = 512,
                shaderPasses = 0,
                assetMemoryBudgetBytes = 96L * 1024L * 1024L,
            )
            VisualQuality.MEDIUM -> RuntimePerformanceBudget(
                maxVisibleRobots = 28,
                maxParticles = 100,
                maxMeteorTrails = 16,
                maxLoadedModels = 48,
                maxTextureEdge = 1024,
                shaderPasses = 1,
                assetMemoryBudgetBytes = 192L * 1024L * 1024L,
            )
            VisualQuality.HIGH -> RuntimePerformanceBudget(
                maxVisibleRobots = 50,
                maxParticles = 180,
                maxMeteorTrails = 28,
                maxLoadedModels = 80,
                maxTextureEdge = 2048,
                shaderPasses = 2,
                assetMemoryBudgetBytes = 320L * 1024L * 1024L,
            )
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
