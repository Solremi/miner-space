package fr.solremi.minerspace.domain.strategy

import fr.solremi.minerspace.shared.GameId

data class OwnedModule(
    val instanceId: String,
    val definitionId: GameId,
    val level: Int,
    val equippedRobotId: GameId?,
) {
    init {
        require(instanceId.isNotBlank())
        require(level >= 1)
    }
}

data class StrategyState(
    val activeSpecialization: SpecializationId?,
    val trialUsed: Boolean,
    val specializationChangedAtEpochMillis: Long,
    val modules: Map<String, OwnedModule>,
    val nextModuleSequence: Long,
    val transactionSequence: Long,
) {
    init {
        require(specializationChangedAtEpochMillis >= 0L)
        require(nextModuleSequence >= 1L)
        require(transactionSequence >= 0L)
        require(modules.keys == modules.values.map { it.instanceId }.toSet())
    }

    companion object {
        fun empty() = StrategyState(null, false, 0L, emptyMap(), 1L, 0L)
    }
}

data class StrategyAccess(
    val nowEpochMillis: Long,
    val spaceDollars: Long,
    val inventory: Map<GameId, Long>,
    val robotLevels: Map<GameId, Int>,
) {
    init {
        require(nowEpochMillis >= 0L)
        require(spaceDollars >= 0L)
        require(inventory.values.all { it >= 0L })
    }
}

data class StrategyTransaction(
    val sequence: Long,
    val reason: String,
    val spaceDollarDelta: Long = 0L,
    val inventoryDeltas: Map<GameId, Long> = emptyMap(),
    val moduleInstanceId: String? = null,
)

sealed interface StrategyCommandResult {
    val state: StrategyState

    data class Applied(
        override val state: StrategyState,
        val transaction: StrategyTransaction,
    ) : StrategyCommandResult

    data class Rejected(
        override val state: StrategyState,
        val code: String,
    ) : StrategyCommandResult
}

data class StrategyComparison(
    val extractionMillionths: Long,
    val refiningSpeedMillionths: Long,
    val assemblySpeedMillionths: Long,
    val logisticsMillionths: Long,
)
