package fr.solremi.minerspace.domain.exploration

import fr.solremi.minerspace.shared.GameId

data class ExplorationTransaction(
    val sequence: Long,
    val reason: String,
    val sectorId: GameId,
    val spaceDollarCost: Long = 0L,
    val componentCosts: Map<GameId, Long> = emptyMap(),
    val rareDepositId: GameId? = null,
)

sealed interface ExplorationCommandResult {
    val state: ExplorationState

    data class Applied(
        override val state: ExplorationState,
        val transaction: ExplorationTransaction,
    ) : ExplorationCommandResult

    data class Rejected(
        override val state: ExplorationState,
        val code: String,
    ) : ExplorationCommandResult
}
