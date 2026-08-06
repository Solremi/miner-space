package fr.solremi.minerspace.domain.economy

import fr.solremi.minerspace.shared.GameId

data class EconomyTransaction(
    val sequence: Long,
    val reason: String,
    val resourceDeltas: Map<GameId, Long> = emptyMap(),
    val spaceDollarDelta: Long = 0L,
)

sealed interface EconomyCommandResult {
    val state: EconomyState

    data class Applied(
        override val state: EconomyState,
        val transaction: EconomyTransaction,
    ) : EconomyCommandResult

    data class Rejected(
        override val state: EconomyState,
        val code: String,
    ) : EconomyCommandResult
}

data class ExtractionTickResult(
    val state: EconomyState,
    val extractedByDeposit: Map<GameId, Long>,
)
