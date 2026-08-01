package fr.solremi.minerspace.data

import fr.solremi.minerspace.domain.services.DeferredSaveService
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Coalesces non-critical autosaves on a single IO thread while preserving synchronous
 * SaveService semantics for transactions. A synchronous save or clear invalidates any
 * older queued write for the same slot before touching the delegate.
 */
class CoalescingSaveService(
    private val delegate: SaveService,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "miner-space-save-io").apply { isDaemon = true }
    },
) : SaveService, DeferredSaveService {
    private data class Queued(
        val payload: SavePayload,
        val generation: Long,
    )

    private val ioLock = Any()
    private val pending = ConcurrentHashMap<String, Queued>()
    private val generations = ConcurrentHashMap<String, AtomicLong>()
    private val drainScheduled = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    override fun loadLatest(slotId: String): SavePayload? {
        pending[slotId]?.payload?.let { return it }
        return synchronized(ioLock) { delegate.loadLatest(slotId) }
    }

    override fun save(payload: SavePayload): SaveWriteStatus {
        if (closed.get()) return SaveWriteStatus.FAILED
        val generation = generation(payload.slotId).incrementAndGet()
        pending.remove(payload.slotId)
        return synchronized(ioLock) {
            if (generation != generation(payload.slotId).get()) {
                SaveWriteStatus.FAILED
            } else {
                delegate.save(payload)
            }
        }
    }

    override fun clear(slotId: String) {
        generation(slotId).incrementAndGet()
        pending.remove(slotId)
        synchronized(ioLock) { delegate.clear(slotId) }
    }

    override fun enqueue(payload: SavePayload): Boolean {
        if (closed.get()) return false
        val nextGeneration = generation(payload.slotId).incrementAndGet()
        pending[payload.slotId] = Queued(payload, nextGeneration)
        scheduleDrain()
        return true
    }

    override fun flush(timeoutMillis: Long): Boolean {
        require(timeoutMillis >= 0L)
        if (closed.get()) return pending.isEmpty()
        val future = executor.submit { drainAll() }
        return runCatching {
            future.get(timeoutMillis.coerceAtLeast(1L), TimeUnit.MILLISECONDS)
            pending.isEmpty()
        }.getOrDefault(false)
    }

    override fun close() {
        if (closed.get()) return
        flush(CLOSE_TIMEOUT_MILLIS)
        if (!closed.compareAndSet(false, true)) return
        executor.shutdown()
        runCatching { executor.awaitTermination(CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) }
        if (!executor.isTerminated) executor.shutdownNow()
    }

    private fun scheduleDrain() {
        if (!drainScheduled.compareAndSet(false, true)) return
        executor.execute {
            try {
                drainAll()
            } finally {
                drainScheduled.set(false)
                if (pending.isNotEmpty() && !closed.get()) scheduleDrain()
            }
        }
    }

    private fun drainAll() {
        while (true) {
            val entry = pending.entries.firstOrNull() ?: return
            val slotId = entry.key
            val candidate = entry.value
            synchronized(ioLock) {
                val current = pending[slotId]
                val currentGeneration = generation(slotId).get()
                if (current == candidate && currentGeneration == candidate.generation) {
                    pending.remove(slotId, candidate)
                    delegate.save(candidate.payload)
                } else {
                    pending.remove(slotId, candidate)
                }
            }
        }
    }

    private fun generation(slotId: String): AtomicLong =
        generations.computeIfAbsent(slotId) { AtomicLong(0L) }

    private companion object {
        const val CLOSE_TIMEOUT_MILLIS = 2_000L
    }
}
