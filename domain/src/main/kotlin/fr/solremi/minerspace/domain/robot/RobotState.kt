package fr.solremi.minerspace.domain.robot

import fr.solremi.minerspace.shared.GameId

data class RobotStatistics(
    val extracted: Long = 0L,
    val refined: Long = 0L,
    val assembled: Long = 0L,
    val transported: Long = 0L,
    val activeSeconds: Long = 0L,
) {
    init {
        require(extracted >= 0L && refined >= 0L && assembled >= 0L && transported >= 0L)
        require(activeSeconds >= 0L)
    }

    fun workFor(family: RobotFamily): Long = when (family) {
        RobotFamily.EXTRACTOR -> extracted
        RobotFamily.REFINER -> refined
        RobotFamily.ASSEMBLER -> assembled
        RobotFamily.LOGISTICS -> transported
    }
}

data class RobotInstance(
    val id: GameId,
    val family: RobotFamily,
    val displayName: String,
    val serialNumber: String,
    val level: Int,
    val trait: RobotTrait,
    val masteryPoints: Long,
    val priority: AutomationPriority,
    val statistics: RobotStatistics,
) {
    init {
        require(displayName.isNotBlank())
        require(serialNumber.isNotBlank())
        require(level >= 1)
        require(masteryPoints >= 0L)
    }
}

data class RobotAutomationState(
    val robots: Map<GameId, RobotInstance>,
    val lastLogisticsEpochMillis: Long,
    val priorityCursor: Int,
    val renderQuality: RenderQuality,
    val transactionSequence: Long,
) {
    init {
        require(lastLogisticsEpochMillis >= 0L)
        require(priorityCursor >= 0)
        require(transactionSequence >= 0L)
        require(robots.keys.size == robots.size)
        require(robots.values.map { it.serialNumber }.distinct().size == robots.size)
    }
}

data class PendingDeposit(
    val depositId: GameId,
    val resourceId: GameId,
    val pendingQuantity: Long,
) {
    init { require(pendingQuantity >= 0L) }
}

data class LogisticsResult(
    val automation: RobotAutomationState,
    val pendingByDeposit: Map<GameId, Long>,
    val inventory: Map<GameId, Long>,
    val movedByResource: Map<GameId, Long>,
    val elapsedSeconds: Long,
) {
    val totalMoved: Long get() = movedByResource.values.sum()
}

data class QueueTask(val id: String, val durationSeconds: Long) {
    init {
        require(id.isNotBlank())
        require(durationSeconds > 0L)
    }
}

data class QueueAssignment(
    val taskId: String,
    val laneIndex: Int,
    val startsAtSecond: Long,
    val finishesAtSecond: Long,
)

data class RobotTransaction(
    val sequence: Long,
    val reason: String,
    val robotId: GameId,
    val spaceDollarCost: Long = 0L,
)

sealed interface RobotCommandResult {
    val state: RobotAutomationState

    data class Applied(
        override val state: RobotAutomationState,
        val transaction: RobotTransaction,
    ) : RobotCommandResult

    data class Rejected(
        override val state: RobotAutomationState,
        val code: String,
    ) : RobotCommandResult
}
