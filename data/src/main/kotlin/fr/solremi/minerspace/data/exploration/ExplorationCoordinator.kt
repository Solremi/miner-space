package fr.solremi.minerspace.data.exploration

import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.exploration.SectorContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.SectorProgressCodec
import fr.solremi.minerspace.data.transaction.SaveMutation
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionResult
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.exploration.ExplorationAccess
import fr.solremi.minerspace.domain.exploration.ExplorationCommandResult
import fr.solremi.minerspace.domain.exploration.ExplorationDefinitions
import fr.solremi.minerspace.domain.exploration.ExplorationEngine
import fr.solremi.minerspace.domain.exploration.ExplorationState
import fr.solremi.minerspace.domain.exploration.ExplorationTransaction
import fr.solremi.minerspace.domain.exploration.SectorAvailability
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.GameLogger

data class ExplorationSession(
    val manufacturing: ManufacturingGameState,
    val exploration: ExplorationState,
)

sealed interface ExplorationActionResult {
    val session: ExplorationSession

    data class Applied(
        override val session: ExplorationSession,
        val transaction: ExplorationTransaction,
    ) : ExplorationActionResult

    data class Rejected(
        override val session: ExplorationSession,
        val code: String,
    ) : ExplorationActionResult

    data class PersistenceFailed(
        override val session: ExplorationSession,
        val code: String,
    ) : ExplorationActionResult

    data class TransactionPending(
        override val session: ExplorationSession,
        val transaction: SaveTransactionResult,
    ) : ExplorationActionResult
}

class ExplorationCoordinator(
    private val save: SaveService,
    private val clock: ClockService,
    private val logger: GameLogger,
    val economyDefinitions: EconomyDefinitions,
    val definitions: ExplorationDefinitions,
) {
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val engine = ExplorationEngine(definitions)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val sectorCodec = SectorProgressCodec()
    private val transactionCoordinator = SaveTransactionCoordinator(save)

    fun load(): ExplorationSession = ExplorationSession(
        manufacturing = loadManufacturing(),
        exploration = loadExploration(),
    )

    fun access(session: ExplorationSession): ExplorationAccess {
        val technologies = session.manufacturing.assembly.installedTechnologyIds
        val scannerLevel = 1 +
            (if (TECH_EXTRACTION in technologies) 1 else 0) +
            (if (TECH_SORTING in technologies) 1 else 0)
        return ExplorationAccess(
            scannerLevel = scannerLevel,
            spaceDollars = session.manufacturing.economy.spaceDollars,
            inventory = session.manufacturing.economy.inventory,
            installedTechnologyIds = technologies,
        )
    }

    fun availability(session: ExplorationSession, sectorId: GameId): SectorAvailability =
        engine.availability(session.exploration, sectorId, access(session))

    fun saveExploration(session: ExplorationSession): Boolean {
        val payload = sectorCodec.encode(
            state = engine.normalize(session.exploration),
            contentVersion = definitions.contentVersion,
            savedAtEpochMillis = now(),
        )
        val status = runCatching { save.save(payload) }
            .onFailure { logger.error(TAG, "Unable to save exploration state.", it) }
            .getOrElse { SaveWriteStatus.FAILED }
        if (status != SaveWriteStatus.WRITTEN) {
            logger.warning(TAG, "Exploration save was not written: $status")
        }
        return status == SaveWriteStatus.WRITTEN
    }

    fun scan(session: ExplorationSession, sectorId: GameId): ExplorationActionResult =
        when (val result = engine.scan(session.exploration, sectorId, access(session))) {
            is ExplorationCommandResult.Rejected ->
                ExplorationActionResult.Rejected(session, result.code)

            is ExplorationCommandResult.Applied -> {
                val next = session.copy(exploration = result.state)
                if (saveExploration(next)) {
                    ExplorationActionResult.Applied(next, result.transaction)
                } else {
                    ExplorationActionResult.PersistenceFailed(session, "exploration_save_failed")
                }
            }
        }

    fun unlock(session: ExplorationSession, sectorId: GameId): ExplorationActionResult {
        val result = engine.unlock(session.exploration, sectorId, access(session))
        if (result is ExplorationCommandResult.Rejected) {
            return ExplorationActionResult.Rejected(session, result.code)
        }
        result as ExplorationCommandResult.Applied

        val nextManufacturing = applyUnlockCosts(session.manufacturing, result.transaction)
        val next = ExplorationSession(nextManufacturing, result.state)
        val savedAt = now()
        val transactionId = "sector_${result.transaction.sequence}_${sectorId.value}"
        val firstAttempt = transactionCoordinator.execute(
            transactionId = transactionId,
            mutations = listOf(
                SaveMutation.write(
                    mainCodec.encode(
                        state = next.manufacturing,
                        contentVersion = economyDefinitions.contentVersion,
                        savedAtEpochMillis = savedAt,
                    ),
                ),
                SaveMutation.write(
                    sectorCodec.encode(
                        state = next.exploration,
                        contentVersion = definitions.contentVersion,
                        savedAtEpochMillis = savedAt,
                    ),
                ),
            ),
            nowEpochMillis = savedAt,
        )
        val finalAttempt = if (
            firstAttempt.status == SaveTransactionStatus.PENDING &&
            firstAttempt.transactionId == transactionId
        ) {
            transactionCoordinator.recoverPending()
        } else {
            firstAttempt
        }

        return when (finalAttempt.status) {
            SaveTransactionStatus.COMMITTED ->
                ExplorationActionResult.Applied(next, result.transaction)

            SaveTransactionStatus.PREPARE_FAILED ->
                ExplorationActionResult.PersistenceFailed(session, "transaction_prepare_failed")

            else -> {
                logger.error(
                    TAG,
                    "Sector transaction $transactionId remains ${finalAttempt.status}; slot=${finalAttempt.failedSlotId}.",
                )
                ExplorationActionResult.TransactionPending(session, finalAttempt)
            }
        }
    }

    private fun loadManufacturing(): ManufacturingGameState {
        val initial = ManufacturingGameState(
            economy = economy.initialState(),
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        val payload = loadLatest(DEFAULT_MAIN_SLOT, "manufacturing") ?: return initial
        return runCatching {
            require(payload.contentVersion == economyDefinitions.contentVersion)
            val restored = mainCodec.decode(payload)
            economy.requireValid(restored.economy)
            restored
        }.onFailure {
            logger.warning(TAG, "Manufacturing save is invalid; using the initial state.", it)
        }.getOrElse { initial }
    }

    private fun loadExploration(): ExplorationState {
        val initial = engine.initialState()
        val payload = loadLatest(SectorProgressCodec.SLOT_ID, "exploration") ?: return initial
        return runCatching {
            require(payload.contentVersion == definitions.contentVersion)
            engine.normalize(sectorCodec.decode(payload))
        }.onFailure {
            logger.warning(TAG, "Exploration save is invalid; using the initial state.", it)
        }.getOrElse { initial }
    }

    private fun loadLatest(slotId: String, label: String): SavePayload? = runCatching {
        save.loadLatest(slotId)
    }.onFailure {
        logger.warning(TAG, "Unable to read $label save slot.", it)
    }.getOrNull()

    private fun applyUnlockCosts(
        source: ManufacturingGameState,
        transaction: ExplorationTransaction,
    ): ManufacturingGameState {
        val inventory = source.economy.inventory.toMutableMap()
        transaction.componentCosts.forEach { (id, quantity) ->
            inventory[id] = Math.subtractExact(inventory[id] ?: 0L, quantity)
        }
        transaction.rareDepositId
            ?.let(RARE_REWARDS::get)
            ?.let { rewardId ->
                inventory[rewardId] = Math.addExact(inventory[rewardId] ?: 0L, 1L)
            }

        val nextEconomy = source.economy.copy(
            inventory = inventory,
            spaceDollars = Math.subtractExact(
                source.economy.spaceDollars,
                transaction.spaceDollarCost,
            ),
            transactionSequence = Math.addExact(source.economy.transactionSequence, 1L),
        )
        economy.requireValid(nextEconomy)
        return source.copy(economy = nextEconomy)
    }

    private fun now(): Long = clock.nowEpochMillis().coerceAtLeast(0L)

    companion object {
        private const val TAG = "ExplorationCoordinator"
        private const val DEFAULT_MAIN_SLOT = "primary"

        private val TECH_EXTRACTION = GameId.of("tech_extraction_protocol")
        private val TECH_SORTING = GameId.of("tech_quantum_sorting")
        private val RARE_REWARDS = mapOf(
            GameId.of("rare_deposit_prismatic_ferrite") to GameId.of("rare_prismatic_ferrite"),
            GameId.of("rare_deposit_xenon") to GameId.of("rare_xenon_crystal"),
            GameId.of("rare_deposit_archive_fragment") to GameId.of("rare_archive_fragment"),
        )

        fun fromServices(services: GameServices): ExplorationCoordinator =
            ExplorationCoordinator(
                save = services.save,
                clock = services.clock,
                logger = services.logger,
                economyDefinitions = CoreEconomyContentLoader().load(services.content),
                definitions = SectorContentLoader().load(services.content),
            )
    }
}
