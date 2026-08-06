package fr.solremi.minerspace.domain.assembly

import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningGameState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.shared.GameId

enum class AssemblyJobStatus { QUEUED, RUNNING, READY_TO_COLLECT }

data class AssemblyJob(
    val id: String,
    val recipeId: GameId,
    val queuedAtEpochMillis: Long,
    val startsAtEpochMillis: Long,
    val finishesAtEpochMillis: Long,
    val reservedInputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val outputKind: AssemblyOutputKind,
    val status: AssemblyJobStatus,
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

data class AssemblyState(
    val jobs: List<AssemblyJob>,
    val installedTechnologyIds: Set<GameId>,
    val nextJobSequence: Long,
) {
    init {
        require(nextJobSequence >= 1L)
        require(jobs.map { it.id }.distinct().size == jobs.size)
    }

    companion object {
        fun empty(): AssemblyState = AssemblyState(emptyList(), emptySet(), 1L)
    }
}

data class ManufacturingGameState(
    val economy: EconomyState,
    val refining: RefiningState,
    val assembly: AssemblyState,
) {
    fun refiningView(): RefiningGameState = RefiningGameState(economy, refining)

    fun withRefining(state: RefiningGameState): ManufacturingGameState = copy(
        economy = state.economy,
        refining = state.refining,
    )
}

data class AssemblyTransaction(
    val reason: String,
    val jobId: String? = null,
    val inventoryDeltas: Map<GameId, Long> = emptyMap(),
    val technologyId: GameId? = null,
)

sealed interface AssemblyCommandResult {
    val state: ManufacturingGameState

    data class Applied(
        override val state: ManufacturingGameState,
        val transaction: AssemblyTransaction,
    ) : AssemblyCommandResult

    data class Rejected(
        override val state: ManufacturingGameState,
        val code: String,
    ) : AssemblyCommandResult
}

data class ProductionComparison(
    val basePerMinute: Long,
    val currentPerMinute: Long,
    val projectedPerMinute: Long,
)
