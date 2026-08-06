package fr.solremi.minerspace.domain.refining

import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.shared.GameId

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
