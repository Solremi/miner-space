package fr.solremi.minerspace.domain.exploration

import fr.solremi.minerspace.shared.GameId

data class ExplorationState(
    val revealedSectorIds: Set<GameId>,
    val unlockedSectorIds: Set<GameId>,
    val discoveredRareDepositIds: Set<GameId>,
    val spentSpaceDollars: Long,
    val spentComponents: Map<GameId, Long>,
    val activeMissionSectorId: GameId?,
    val transactionSequence: Long,
) {
    init {
        require(spentSpaceDollars >= 0L)
        require(spentComponents.values.none { it < 0L })
        require(transactionSequence >= 0L)
        require(revealedSectorIds.containsAll(unlockedSectorIds))
    }
}

data class ExplorationAccess(
    val scannerLevel: Int,
    val spaceDollars: Long,
    val inventory: Map<GameId, Long>,
    val installedTechnologyIds: Set<GameId>,
) {
    init {
        require(scannerLevel >= 1)
        require(spaceDollars >= 0L)
        require(inventory.values.none { it < 0L })
    }
}

data class SectorAvailability(
    val revealed: Boolean,
    val unlocked: Boolean,
    val scannerSatisfied: Boolean,
    val sectorsSatisfied: Boolean,
    val technologiesSatisfied: Boolean,
    val moneySatisfied: Boolean,
    val componentsSatisfied: Boolean,
    val availableSpaceDollars: Long,
    val availableComponents: Map<GameId, Long>,
) {
    val canScan: Boolean get() = !revealed && scannerSatisfied && sectorsSatisfied
    val canUnlock: Boolean get() = revealed && !unlocked && scannerSatisfied && sectorsSatisfied &&
        technologiesSatisfied && moneySatisfied && componentsSatisfied
}
