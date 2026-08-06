package fr.solremi.minerspace.domain.progression

import fr.solremi.minerspace.shared.GameId

data class ProgressionState(
    val tutorialStepIndex: Int,
    val completedTutorialIds: Set<GameId>,
    val claimedMissionIds: Set<GameId>,
    val contractCycles: Map<ContractTier, Int>,
    val completedContractCount: Long,
    val discoveredCodexEntryIds: Set<GameId>,
    val claimedCollectionIds: Set<GameId>,
    val selectedObjectiveId: GameId?,
    val transactionSequence: Long,
) {
    init {
        require(tutorialStepIndex >= 0)
        require(contractCycles.values.none { it < 0 })
        require(completedContractCount >= 0L)
        require(transactionSequence >= 0L)
    }
}

data class TutorialProgress(
    val step: TutorialStepDefinition?,
    val current: Long,
    val completed: Int,
    val total: Int,
)

data class MissionView(
    val definition: MissionDefinition,
    val current: Long,
    val completed: Boolean,
) {
    val claimable get() = completed
}

data class ContractView(
    val occurrenceId: String,
    val definition: ContractDefinition,
    val currentInventory: Long,
    val unlocked: Boolean,
) {
    val deliverable get() = unlocked && currentInventory >= definition.quantity
}

data class CodexEntryView(
    val definition: CodexEntryDefinition,
    val discovered: Boolean,
    val current: Long,
)

data class CollectionView(
    val definition: CollectionDefinition,
    val discoveredEntries: Int,
    val claimed: Boolean,
) {
    val complete get() = discoveredEntries == definition.entryIds.size
    val claimable get() = complete && !claimed
}

data class EconomicDelta(
    val spaceDollarsDelta: Long = 0L,
    val inventoryDelta: Map<GameId, Long> = emptyMap(),
) {
    init { require(inventoryDelta.values.none { it == 0L }) }
}

data class ProgressionTransaction(
    val sequence: Long,
    val reason: String,
    val referenceId: String,
    val delta: EconomicDelta,
)

sealed interface ProgressionCommandResult {
    val state: ProgressionState

    data class Applied(
        override val state: ProgressionState,
        val transaction: ProgressionTransaction,
    ) : ProgressionCommandResult

    data class Rejected(
        override val state: ProgressionState,
        val code: String,
    ) : ProgressionCommandResult
}
