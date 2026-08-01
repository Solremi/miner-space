package fr.solremi.minerspace.data.transaction

import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.ProgressionStateCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.data.save.StrategyStateCodec
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.progression.ProgressionState
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.strategy.StrategyState
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger

data class AtomicStateCommitResult(
    val committed: Boolean,
    val transaction: SaveTransactionResult,
)

/**
 * Commits a deterministic set of save payloads through the persistent transaction journal.
 * A retry with the same namespace and state sequences resumes the existing journal.
 */
class AtomicSaveBundle(
    private val save: SaveService,
    private val clock: ClockService,
    private val logger: GameLogger = SilentGameLogger,
) {
    private val transactions = SaveTransactionCoordinator(save)

    fun commit(
        namespace: String,
        primarySequence: Long,
        secondarySequence: Long,
        payloads: List<SavePayload>,
    ): AtomicStateCommitResult {
        require(NAMESPACE_PATTERN.matches(namespace)) { "Invalid transaction namespace: $namespace" }
        require(primarySequence >= 0L && secondarySequence >= 0L)
        require(payloads.size >= 2)
        require(payloads.map { it.slotId }.distinct().size == payloads.size)

        val transactionId = buildTransactionId(namespace, primarySequence, secondarySequence)
        val first = transactions.execute(
            transactionId = transactionId,
            mutations = payloads.map(SaveMutation::write),
            nowEpochMillis = clock.nowEpochMillis().coerceAtLeast(0L),
        )
        val final = if (
            first.status == SaveTransactionStatus.PENDING &&
            first.transactionId == transactionId
        ) {
            transactions.recoverPending()
        } else {
            first
        }
        val committed = final.status == SaveTransactionStatus.COMMITTED
        if (committed) {
            logger.info(TAG, "Committed atomic state transaction $transactionId")
        } else {
            logger.error(
                TAG,
                "Atomic state transaction $transactionId remains ${final.status}; slot=${final.failedSlotId.orEmpty()}",
            )
        }
        return AtomicStateCommitResult(committed, final)
    }

    private fun buildTransactionId(namespace: String, first: Long, second: Long): String {
        val suffix = "_${first}_$second"
        val prefix = namespace.take((64 - suffix.length).coerceAtLeast(1)).trimEnd('_')
        return "$prefix$suffix"
    }

    private companion object {
        const val TAG = "AtomicSaveBundle"
        val NAMESPACE_PATTERN = Regex("[a-z0-9_]{1,40}")
    }
}

class RobotStateTransactionCoordinator(
    save: SaveService,
    clock: ClockService,
    logger: GameLogger = SilentGameLogger,
) {
    private val bundle = AtomicSaveBundle(save, clock, logger)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val robotCodec = RobotFleetCodec()

    fun commit(
        main: ManufacturingGameState,
        robots: RobotAutomationState,
        mainContentVersion: String,
        robotContentVersion: String,
        reason: String,
        savedAtEpochMillis: Long,
    ): AtomicStateCommitResult = bundle.commit(
        namespace = "robots_${reason.token()}",
        primarySequence = main.economy.transactionSequence,
        secondarySequence = robots.transactionSequence,
        payloads = listOf(
            mainCodec.encode(main, mainContentVersion, savedAtEpochMillis),
            robotCodec.encode(robots, robotContentVersion, savedAtEpochMillis),
        ),
    )
}

class StrategyStateTransactionCoordinator(
    save: SaveService,
    clock: ClockService,
    logger: GameLogger = SilentGameLogger,
) {
    private val bundle = AtomicSaveBundle(save, clock, logger)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val strategyCodec = StrategyStateCodec()

    fun commit(
        main: ManufacturingGameState,
        strategy: StrategyState,
        mainContentVersion: String,
        strategyContentVersion: String,
        reason: String,
        savedAtEpochMillis: Long,
    ): AtomicStateCommitResult = bundle.commit(
        namespace = "strategy_${reason.token()}",
        primarySequence = main.economy.transactionSequence,
        secondarySequence = strategy.transactionSequence,
        payloads = listOf(
            mainCodec.encode(main, mainContentVersion, savedAtEpochMillis),
            strategyCodec.encode(strategy, strategyContentVersion, savedAtEpochMillis),
        ),
    )
}

class ProgressionStateTransactionCoordinator(
    save: SaveService,
    clock: ClockService,
    logger: GameLogger = SilentGameLogger,
) {
    private val bundle = AtomicSaveBundle(save, clock, logger)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val progressionCodec = ProgressionStateCodec()

    fun commit(
        main: ManufacturingGameState,
        progression: ProgressionState,
        mainContentVersion: String,
        progressionContentVersion: String,
        reason: String,
        savedAtEpochMillis: Long,
    ): AtomicStateCommitResult = bundle.commit(
        namespace = "progress_${reason.token()}",
        primarySequence = main.economy.transactionSequence,
        secondarySequence = progression.transactionSequence,
        payloads = listOf(
            mainCodec.encode(main, mainContentVersion, savedAtEpochMillis),
            progressionCodec.encode(progression, progressionContentVersion, savedAtEpochMillis),
        ),
    )
}

private fun String.token(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')
    .take(20)
    .ifBlank { "change" }
