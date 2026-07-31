package fr.solremi.minerspace.game.presentation

import fr.solremi.minerspace.domain.presentation.FeedbackKind

data class FeedbackPulse(
    val kind: FeedbackKind,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val normalizedX: Float,
    val normalizedY: Float,
)

object GameFeedbackBus {
    private val pulses = mutableListOf<FeedbackPulse>()

    @Synchronized
    fun emit(kind: FeedbackKind, startedAtMillis: Long, normalizedX: Float = .5f, normalizedY: Float = .5f) {
        pulses += FeedbackPulse(
            kind = kind,
            startedAtMillis = startedAtMillis.coerceAtLeast(0L),
            durationMillis = when (kind) {
                FeedbackKind.INTERACTION -> 240L
                FeedbackKind.SUCCESS, FeedbackKind.PRODUCTION -> 520L
                FeedbackKind.ERROR -> 460L
                FeedbackKind.RARE -> 900L
                FeedbackKind.SECTOR_OPEN -> 820L
                FeedbackKind.LAUNCH -> 700L
            },
            normalizedX = normalizedX.coerceIn(0f, 1f),
            normalizedY = normalizedY.coerceIn(0f, 1f),
        )
        if (pulses.size > 24) pulses.removeAt(0)
    }

    @Synchronized
    fun active(nowMillis: Long): List<FeedbackPulse> {
        pulses.removeAll { nowMillis - it.startedAtMillis >= it.durationMillis }
        return pulses.toList()
    }
}
