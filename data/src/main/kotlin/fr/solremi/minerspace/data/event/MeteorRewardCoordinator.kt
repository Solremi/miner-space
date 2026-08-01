package fr.solremi.minerspace.data.event

import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.MeteorEventCodec
import fr.solremi.minerspace.data.transaction.SaveMutation
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionResult
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.event.MeteorEventDefinition
import fr.solremi.minerspace.domain.event.MeteorEventPhase
import fr.solremi.minerspace.domain.event.MeteorEventState
import fr.solremi.minerspace.domain.event.MeteorRewardEngine
import fr.solremi.minerspace.domain.event.MeteorRewardResult
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.shared.GameLogger

sealed interface MeteorRewardCommitResult {
    val main: ManufacturingGameState
    val event: MeteorEventState

    data class Committed(
        override val main: ManufacturingGameState,
        override val event: MeteorEventState,
    ) : MeteorRewardCommitResult

    data class Rejected(
        override val main: ManufacturingGameState,
        override val event: MeteorEventState,
        val code: String,
    ) : MeteorRewardCommitResult

    data class Pending(
        override val main: ManufacturingGameState,
        override val event: MeteorEventState,
        val transaction: SaveTransactionResult,
    ) : MeteorRewardCommitResult
}

class MeteorRewardCoordinator(
    private val save: SaveService,
    private val clock: ClockService,
    private val logger: GameLogger,
    private val definition: MeteorEventDefinition,
    private val economyDefinitions: EconomyDefinitions,
) {
    private val mainCodec = ManufacturingSnapshotCodec()
    private val eventCodec = MeteorEventCodec()
    private val rewardEngine = MeteorRewardEngine(
        definition,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )
    private val transactionCoordinator = SaveTransactionCoordinator(save)

    fun loadMain(): ManufacturingGameState {
        val initial = ManufacturingGameState(
            CoreEconomyEngine(economyDefinitions).initialState(),
            RefiningState.empty(),
            AssemblyState.empty(),
        )
        val payload = loadLatest(ManufacturingSnapshotCodec.DEFAULT_SLOT, "manufacturing") ?: return initial
        return runCatching {
            require(payload.contentVersion == economyDefinitions.contentVersion)
            mainCodec.decode(payload)
        }.onFailure {
            logger.warning(TAG, "Meteor reward could not decode the manufacturing save.", it)
        }.getOrElse { initial }
    }

    fun commit(
        main: ManufacturingGameState,
        event: MeteorEventState,
    ): MeteorRewardCommitResult = when (event.phase) {
        MeteorEventPhase.SUMMARY -> {
            val preparation = rewardEngine.prepare(main, event)
                ?: return MeteorRewardCommitResult.Rejected(main, event, "meteor_reward_storage_full")
            commitPrepared(preparation.state, preparation.event, main, event)
        }

        MeteorEventPhase.COMMITTING -> commitPrepared(main, event, main, event)
        MeteorEventPhase.COMMITTED -> MeteorRewardCommitResult.Committed(main, event)
        MeteorEventPhase.ACTIVE -> MeteorRewardCommitResult.Rejected(main, event, "meteor_reward_not_ready")
    }

    fun recoverPending(): SaveTransactionResult = transactionCoordinator.recoverPending()

    private fun commitPrepared(
        preparedMain: ManufacturingGameState,
        preparedEvent: MeteorEventState,
        fallbackMain: ManufacturingGameState,
        fallbackEvent: MeteorEventState,
    ): MeteorRewardCommitResult {
        val reconciled = rewardEngine.reconcile(preparedMain, preparedEvent)
        if (reconciled is MeteorRewardResult.Rejected) {
            return MeteorRewardCommitResult.Rejected(fallbackMain, fallbackEvent, reconciled.code)
        }
        reconciled as MeteorRewardResult.Applied

        val savedAt = now()
        val transactionId = "meteor_reward_${preparedEvent.eventId}_${preparedEvent.transactionSequence}"
        val first = transactionCoordinator.execute(
            transactionId = transactionId,
            mutations = listOf(
                SaveMutation.write(
                    mainCodec.encode(
                        state = reconciled.state,
                        contentVersion = economyDefinitions.contentVersion,
                        savedAtEpochMillis = savedAt,
                    ),
                ),
                SaveMutation.write(
                    eventCodec.encode(
                        state = reconciled.event,
                        contentVersion = definition.contentVersion,
                        savedAtEpochMillis = savedAt,
                    ),
                ),
            ),
            nowEpochMillis = savedAt,
        )
        val final = if (
            first.status == SaveTransactionStatus.PENDING &&
            first.transactionId == transactionId
        ) {
            transactionCoordinator.recoverPending()
        } else {
            first
        }
        return if (final.status == SaveTransactionStatus.COMMITTED) {
            logger.info(TAG, "Meteor reward transaction committed: $transactionId")
            MeteorRewardCommitResult.Committed(reconciled.state, reconciled.event)
        } else {
            logger.error(
                TAG,
                "Meteor reward transaction remains ${final.status}; slot=${final.failedSlotId}",
            )
            MeteorRewardCommitResult.Pending(fallbackMain, fallbackEvent, final)
        }
    }

    private fun loadLatest(slotId: String, label: String): SavePayload? = runCatching {
        save.loadLatest(slotId)
    }.onFailure {
        logger.warning(TAG, "Unable to read $label save slot.", it)
    }.getOrNull()

    private fun now(): Long = clock.nowEpochMillis().coerceAtLeast(0L)

    companion object {
        private const val TAG = "MeteorRewardCoordinator"

        fun fromServices(
            services: GameServices,
            definition: MeteorEventDefinition,
        ): MeteorRewardCoordinator = MeteorRewardCoordinator(
            save = services.save,
            clock = services.clock,
            logger = services.logger,
            definition = definition,
            economyDefinitions = CoreEconomyContentLoader().load(services.content),
        )
    }
}
