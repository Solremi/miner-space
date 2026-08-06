package fr.solremi.minerspace.domain.narrative

import fr.solremi.minerspace.shared.GameId

data class NarrativeTransaction(
    val sequence: Long,
    val reason: String,
    val chapterId: GameId,
    val grant: PendingNarrativeGrant? = null,
)

sealed interface NarrativeCommandResult {
    val state: NarrativeState

    data class Applied(
        override val state: NarrativeState,
        val transaction: NarrativeTransaction,
    ) : NarrativeCommandResult

    data class Rejected(
        override val state: NarrativeState,
        val code: String,
    ) : NarrativeCommandResult
}
