package fr.solremi.minerspace.simulation

import fr.solremi.minerspace.domain.services.ClockService
import java.util.concurrent.atomic.AtomicLong

class FixedClockService(
    initialEpochMillis: Long = 0L,
    initialMonotonicMillis: Long = 0L,
) : ClockService {
    private val epoch = AtomicLong(initialEpochMillis)
    private val monotonic = AtomicLong(initialMonotonicMillis)

    override fun nowEpochMillis(): Long = epoch.get()

    override fun monotonicMillis(): Long = monotonic.get()

    fun advanceBy(durationMillis: Long) {
        require(durationMillis >= 0L) { "Simulation time cannot move backwards." }
        epoch.addAndGet(durationMillis)
        monotonic.addAndGet(durationMillis)
    }
}
