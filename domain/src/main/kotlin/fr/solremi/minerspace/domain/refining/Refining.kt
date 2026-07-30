package fr.solremi.minerspace.domain.refining

import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.economy.FixedPointMath
import fr.solremi.minerspace.shared.GameId
import java.math.BigInteger

const val BASIS_POINTS_SCALE: Long = 10_000L

data class RecipeDefinition(
    val id: GameId,
    val nameKey: String,
    val inputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val durationSeconds: Long,
) {
    init {
        require(nameKey.isNotBlank())
        require(inputs.isNotEmpty())
        require(inputs.values.all { it > 0L })
        require(outputQuantity > 0L)
        require(durationSeconds > 0L)
    }
}

data class RefinerRobotDefinition(
    val id: GameId,
    val nameKey: String,
    val queueCapacity: Int,
) {
    init {
        require(nameKey.isNotBlank())
        require(queueCapacity > 0)
    }
}

data class RefiningDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val robot: RefinerRobotDefinition,
    val recipes: Map<GameId, RecipeDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(recipes.isNotEmpty())
    }
}

enum class RefiningJobStatus {
    QUEUED,
    RUNNING,
    READY_TO_COLLECT,
}

data class RefiningJob(
    val id: String,
    val recipeId: GameId,
    val queuedAtEpochMillis: Long,
    val startsAtEpochMillis: Long,
    val finishesAtEpochMillis: Long,
    val reservedInputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val status: RefiningJobStatus,
) {
    init {
        require(id.isNotBlank())
        require(queuedAtEpochMillis >= 0L)
        require(startsAtEpochMillis >= queuedAtEpochMillis)
        require(finishesAtEpochMillis > startsAtEpochMillis)
        require(reservedInputs.isNotEmpty())
        require(reservedInputs.values.all { it > 0L })
        require(outputQuantity > 0L)
    }
}

data class RefiningState(
    val jobs: List<RefiningJob>,
    val refundBuffer: Map<GameId, Long>,
    val nextJobSequence: Long,
) {
    init {
        require(refundBuffer.values.none { it < 0L })
        require(nextJobSequence >= 1L)
        require(jobs.map { it.id }.distinct().size == jobs.size)
    }

    companion object {
        fun empty(): RefiningState = RefiningState(
            jobs = emptyList(),
            refundBuffer = emptyMap(),
            nextJobSequence = 1L,
        )
    }
}

data class RefiningGameState(
    val economy: EconomyState,
    val refining: RefiningState,
)

data class RefiningTransaction(
    val reason: String,
    val jobId: String? = null,
    val inventoryDeltas: Map<GameId, Long> = emptyMap(),
    val refundBufferDeltas: Map<GameId, Long> = emptyMap(),
)

sealed interface RefiningCommandResult {
    val state: RefiningGameState

    data class Applied(
        override val state: RefiningGameState,
        val transaction: RefiningTransaction,
    ) : RefiningCommandResult

    data class Rejected(
        override val state: RefiningGameState,
        val code: String,
    ) : RefiningCommandResult
}

class RefiningEngine(
    private val definitions: RefiningDefinitions,
    private val storageCapacities: Map<GameId, Long>,
) {
    fun reconcile(state: RefiningGameState, nowEpochMillis: Long): RefiningGameState {
        require(nowEpochMillis >= 0L)
        val updated = state.refining.jobs.map { job ->
            val status = when {
                nowEpochMillis >= job.finishesAtEpochMillis -> RefiningJobStatus.READY_TO_COLLECT
                nowEpochMillis >= job.startsAtEpochMillis -> RefiningJobStatus.RUNNING
                else -> RefiningJobStatus.QUEUED
            }
            if (status == job.status) job else job.copy(status = status)
        }
        return if (updated == state.refining.jobs) state else state.copy(
            refining = state.refining.copy(jobs = updated),
        )
    }

    fun launch(
        state: RefiningGameState,
        recipeId: GameId,
        nowEpochMillis: Long,
    ): RefiningCommandResult {
        val current = reconcile(state, nowEpochMillis)
        val recipe = definitions.recipes[recipeId]
            ?: return RefiningCommandResult.Rejected(current, "unknown_recipe")
        if (current.refining.jobs.size >= definitions.robot.queueCapacity) {
            return RefiningCommandResult.Rejected(current, "queue_full")
        }
        recipe.inputs.forEach { (resourceId, required) ->
            if ((current.economy.inventory[resourceId] ?: 0L) < required) {
                return RefiningCommandResult.Rejected(current, "missing_input:${resourceId.value}")
            }
        }

        val inventory = current.economy.inventory.toMutableMap()
        recipe.inputs.forEach { (resourceId, required) ->
            inventory[resourceId] = inventory.getValue(resourceId) - required
        }
        val lastFinish = current.refining.jobs.maxOfOrNull { it.finishesAtEpochMillis } ?: nowEpochMillis
        val startsAt = maxOf(nowEpochMillis, lastFinish)
        val finishesAt = Math.addExact(startsAt, Math.multiplyExact(recipe.durationSeconds, 1_000L))
        val sequence = current.refining.nextJobSequence
        val jobId = "rf_job_$sequence"
        val job = RefiningJob(
            id = jobId,
            recipeId = recipe.id,
            queuedAtEpochMillis = nowEpochMillis,
            startsAtEpochMillis = startsAt,
            finishesAtEpochMillis = finishesAt,
            reservedInputs = recipe.inputs,
            outputResourceId = recipe.outputResourceId,
            outputQuantity = recipe.outputQuantity,
            status = if (startsAt <= nowEpochMillis) RefiningJobStatus.RUNNING else RefiningJobStatus.QUEUED,
        )
        val next = current.copy(
            economy = current.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(current.economy.transactionSequence, 1L),
            ),
            refining = current.refining.copy(
                jobs = current.refining.jobs + job,
                nextJobSequence = FixedPointMath.addExact(sequence, 1L),
            ),
        )
        return RefiningCommandResult.Applied(
            next,
            RefiningTransaction(
                reason = "launch_refining",
                jobId = jobId,
                inventoryDeltas = recipe.inputs.mapValues { -it.value },
            ),
        )
    }

    fun cancel(
        state: RefiningGameState,
        jobId: String,
        nowEpochMillis: Long,
    ): RefiningCommandResult {
        val current = reconcile(state, nowEpochMillis)
        val job = current.refining.jobs.firstOrNull { it.id == jobId }
            ?: return RefiningCommandResult.Rejected(current, "unknown_job")
        if (job.status == RefiningJobStatus.READY_TO_COLLECT) {
            return RefiningCommandResult.Rejected(current, "job_already_completed")
        }

        val refundBasisPoints = refundBasisPoints(job, nowEpochMillis)
        val refunds = job.reservedInputs.mapValues { (_, amount) ->
            FixedPointMath.floorMultiply(amount, refundBasisPoints * 100L)
        }.filterValues { it > 0L }
        val inventory = current.economy.inventory.toMutableMap()
        val refundBuffer = current.refining.refundBuffer.toMutableMap()
        val inventoryDeltas = linkedMapOf<GameId, Long>()
        val bufferDeltas = linkedMapOf<GameId, Long>()
        refunds.forEach { (resourceId, refund) ->
            val capacity = storageCapacities[resourceId] ?: Long.MAX_VALUE
            val stored = inventory[resourceId] ?: 0L
            val immediate = minOf(refund, (capacity - stored).coerceAtLeast(0L))
            val buffered = refund - immediate
            if (immediate > 0L) {
                inventory[resourceId] = FixedPointMath.addExact(stored, immediate)
                inventoryDeltas[resourceId] = immediate
            }
            if (buffered > 0L) {
                refundBuffer[resourceId] = FixedPointMath.addExact(refundBuffer[resourceId] ?: 0L, buffered)
                bufferDeltas[resourceId] = buffered
            }
        }
        val remaining = current.refining.jobs.filterNot { it.id == jobId }
        val rescheduled = reschedule(remaining, nowEpochMillis)
        val next = current.copy(
            economy = current.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(current.economy.transactionSequence, 1L),
            ),
            refining = current.refining.copy(
                jobs = rescheduled,
                refundBuffer = refundBuffer,
            ),
        )
        return RefiningCommandResult.Applied(
            next,
            RefiningTransaction(
                reason = "cancel_refining:$refundBasisPoints",
                jobId = jobId,
                inventoryDeltas = inventoryDeltas,
                refundBufferDeltas = bufferDeltas,
            ),
        )
    }

    fun collect(
        state: RefiningGameState,
        jobId: String,
        nowEpochMillis: Long,
    ): RefiningCommandResult {
        val current = reconcile(state, nowEpochMillis)
        val job = current.refining.jobs.firstOrNull { it.id == jobId }
            ?: return RefiningCommandResult.Rejected(current, "unknown_job")
        if (job.status != RefiningJobStatus.READY_TO_COLLECT) {
            return RefiningCommandResult.Rejected(current, "job_not_completed")
        }
        val capacity = storageCapacities[job.outputResourceId]
            ?: return RefiningCommandResult.Rejected(current, "unknown_output_storage")
        val stored = current.economy.inventory[job.outputResourceId] ?: 0L
        if (capacity - stored < job.outputQuantity) {
            return RefiningCommandResult.Rejected(current, "output_storage_full")
        }
        val inventory = current.economy.inventory.toMutableMap().apply {
            this[job.outputResourceId] = FixedPointMath.addExact(stored, job.outputQuantity)
        }
        val next = current.copy(
            economy = current.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(current.economy.transactionSequence, 1L),
            ),
            refining = current.refining.copy(
                jobs = current.refining.jobs.filterNot { it.id == jobId },
            ),
        )
        return RefiningCommandResult.Applied(
            next,
            RefiningTransaction(
                reason = "collect_refining",
                jobId = jobId,
                inventoryDeltas = mapOf(job.outputResourceId to job.outputQuantity),
            ),
        )
    }

    fun collectRefunds(state: RefiningGameState): RefiningCommandResult {
        if (state.refining.refundBuffer.isEmpty()) {
            return RefiningCommandResult.Rejected(state, "no_refunds")
        }
        val inventory = state.economy.inventory.toMutableMap()
        val remaining = linkedMapOf<GameId, Long>()
        val deltas = linkedMapOf<GameId, Long>()
        state.refining.refundBuffer.forEach { (resourceId, amount) ->
            val capacity = storageCapacities[resourceId] ?: Long.MAX_VALUE
            val stored = inventory[resourceId] ?: 0L
            val collected = minOf(amount, (capacity - stored).coerceAtLeast(0L))
            if (collected > 0L) {
                inventory[resourceId] = FixedPointMath.addExact(stored, collected)
                deltas[resourceId] = collected
            }
            if (amount > collected) remaining[resourceId] = amount - collected
        }
        if (deltas.isEmpty()) return RefiningCommandResult.Rejected(state, "refund_storage_full")
        val next = state.copy(
            economy = state.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(state.economy.transactionSequence, 1L),
            ),
            refining = state.refining.copy(refundBuffer = remaining),
        )
        return RefiningCommandResult.Applied(
            next,
            RefiningTransaction("collect_refunds", inventoryDeltas = deltas),
        )
    }

    fun refundBasisPoints(job: RefiningJob, nowEpochMillis: Long): Long {
        if (nowEpochMillis <= job.startsAtEpochMillis) return BASIS_POINTS_SCALE
        val duration = job.finishesAtEpochMillis - job.startsAtEpochMillis
        val elapsed = (nowEpochMillis - job.startsAtEpochMillis).coerceIn(0L, duration)
        val progress = BigInteger.valueOf(elapsed)
            .multiply(BigInteger.valueOf(BASIS_POINTS_SCALE))
            .divide(BigInteger.valueOf(duration))
            .longValueExact()
        return when {
            progress < 1_000L -> 10_000L
            progress < 9_000L -> 8_000L
            else -> 0L
        }
    }

    private fun reschedule(jobs: List<RefiningJob>, nowEpochMillis: Long): List<RefiningJob> {
        var cursor = nowEpochMillis
        return jobs.map { job ->
            if (job.status == RefiningJobStatus.READY_TO_COLLECT) return@map job
            val duration = job.finishesAtEpochMillis - job.startsAtEpochMillis
            val start = maxOf(cursor, nowEpochMillis)
            val finish = Math.addExact(start, duration)
            cursor = finish
            job.copy(
                startsAtEpochMillis = start,
                finishesAtEpochMillis = finish,
                status = if (start <= nowEpochMillis) RefiningJobStatus.RUNNING else RefiningJobStatus.QUEUED,
            )
        }
    }
}
