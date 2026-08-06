package fr.solremi.minerspace.domain.economy

import fr.solremi.minerspace.shared.GameId

data class DepositState(
    val remainingReserve: Long,
    val pendingCollection: Long,
) {
    init {
        require(remainingReserve >= 0L)
        require(pendingCollection >= 0L)
    }
}

data class EconomyState(
    val inventory: Map<GameId, Long>,
    val deposits: Map<GameId, DepositState>,
    val spaceDollars: Long,
    val transactionSequence: Long,
) {
    init {
        require(inventory.values.none { it < 0L })
        require(spaceDollars >= 0L)
        require(transactionSequence >= 0L)
    }
}
