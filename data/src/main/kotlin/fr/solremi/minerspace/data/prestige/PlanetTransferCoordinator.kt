package fr.solremi.minerspace.data.prestige

import fr.solremi.minerspace.data.cryos.CryosIxContentFactory
import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.data.save.CryosIxStateCodec
import fr.solremi.minerspace.data.save.NarrativeStateCodec
import fr.solremi.minerspace.data.save.PrestigeStateCodec
import fr.solremi.minerspace.data.save.ProgressionStateCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.data.save.SectorProgressCodec
import fr.solremi.minerspace.data.save.StrategyStateCodec
import fr.solremi.minerspace.data.transaction.SaveMutation
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionResult
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.cryos.CryosIxEngine
import fr.solremi.minerspace.domain.prestige.PlanetPrestigeEngine
import fr.solremi.minerspace.domain.prestige.PrestigeCommandResult
import fr.solremi.minerspace.domain.prestige.PrestigeSnapshot
import fr.solremi.minerspace.domain.prestige.PrestigeState
import fr.solremi.minerspace.domain.robot.RobotAutomationEngine
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.strategy.SpecializationId
import fr.solremi.minerspace.shared.GameId

sealed interface PlanetTransferCoordinationResult {
    val state: PrestigeState

    data class Committed(
        override val state: PrestigeState,
    ) : PlanetTransferCoordinationResult

    data class Pending(
        override val state: PrestigeState,
        val transaction: SaveTransactionResult,
    ) : PlanetTransferCoordinationResult

    data class Rejected(
        override val state: PrestigeState,
        val code: String,
    ) : PlanetTransferCoordinationResult
}

class PlanetTransferCoordinator(
    private val services: GameServices,
) {
    private val prestigeEngine = PlanetPrestigeEngine()
    private val prestigeCodec = PrestigeStateCodec()
    private val cryosDefinitions = CryosIxContentFactory.create()
    private val cryosCodec = CryosIxStateCodec()
    private val transactionCoordinator = SaveTransactionCoordinator(services.save)
    private val sectorCodec = SectorProgressCodec()
    private val progressionCodec = ProgressionStateCodec()
    private val narrativeCodec = NarrativeStateCodec()
    private val strategyCodec = StrategyStateCodec()
    private val robotCodec = RobotFleetCodec()
    private val robotDefinitions by lazy { RobotContentLoader().load(services.content) }
    private val robotEngine by lazy { RobotAutomationEngine(robotDefinitions) }

    fun loadState(): PrestigeState {
        val initial = prestigeEngine.initialState()
        val payload = loadPayload(PrestigeStateCodec.SLOT_ID, "prestige") ?: return initial
        return runCatching { prestigeEngine.normalize(prestigeCodec.decode(payload)) }
            .onFailure { services.logger.warning(TAG, "Prestige save could not be decoded; using the initial state.", it) }
            .getOrElse { initial }
    }

    fun snapshot(nowEpochMillis: Long): PrestigeSnapshot {
        require(nowEpochMillis >= 0L)
        val sectors = loadOptional(SectorProgressCodec.SLOT_ID, "sectors", sectorCodec::decode)
        val progression = loadOptional(ProgressionStateCodec.SLOT_ID, "progression", progressionCodec::decode)
        val narrative = loadOptional(NarrativeStateCodec.SLOT_ID, "narrative", narrativeCodec::decode)
        val strategy = loadOptional(StrategyStateCodec.SLOT_ID, "strategy", strategyCodec::decode)
        val robots = loadRobots(nowEpochMillis)
        val bonuses = linkedSetOf<GameId>()
        strategy?.activeSpecialization?.let { bonuses += specializationBonus(it) }
        if (strategy?.modules?.isNotEmpty() == true) bonuses += GameId.of("bonus_ferrum_modules")
        return PrestigeSnapshot(
            launchShipyardUnlocked = sectors?.unlockedSectorIds?.contains(LAUNCH_SHIPYARD) == true,
            discoveredCodexEntryIds = progression?.discoveredCodexEntryIds.orEmpty(),
            archiveIds = narrative?.let { it.readTransmissionIds + it.resolvedChapterIds }.orEmpty(),
            permanentBonusIds = bonuses,
            robots = robots.robots.values,
        )
    }

    fun prepareAndCommit(
        state: PrestigeState,
        snapshot: PrestigeSnapshot,
        nowEpochMillis: Long,
    ): PlanetTransferCoordinationResult = when (
        val prepared = prestigeEngine.prepareTransfer(state, snapshot, nowEpochMillis)
    ) {
        is PrestigeCommandResult.Rejected -> PlanetTransferCoordinationResult.Rejected(prepared.state, prepared.code)
        is PrestigeCommandResult.Applied -> commitPrepared(prepared.state, nowEpochMillis)
    }

    fun resumePrepared(
        state: PrestigeState,
        nowEpochMillis: Long,
    ): PlanetTransferCoordinationResult {
        if (state.pendingTransfer == null) {
            return PlanetTransferCoordinationResult.Rejected(state, "no_pending_transfer")
        }
        return commitPrepared(state, nowEpochMillis)
    }

    private fun commitPrepared(
        preparedState: PrestigeState,
        nowEpochMillis: Long,
    ): PlanetTransferCoordinationResult {
        val pending = preparedState.pendingTransfer
            ?: return PlanetTransferCoordinationResult.Rejected(preparedState, "no_pending_transfer")
        val reconciledState = when (val reconciled = prestigeEngine.reconcilePrepared(preparedState)) {
            is PrestigeCommandResult.Rejected ->
                return PlanetTransferCoordinationResult.Rejected(reconciled.state, reconciled.code)
            is PrestigeCommandResult.Applied -> reconciled.state
        }
        val finalizedState = when (val finalized = prestigeEngine.finalizeTransfer(reconciledState)) {
            is PrestigeCommandResult.Rejected ->
                return PlanetTransferCoordinationResult.Rejected(finalized.state, finalized.code)
            is PrestigeCommandResult.Applied -> finalized.state
        }

        val mutations = mutableListOf(
            SaveMutation.clear(PRIMARY_SLOT_ID),
            SaveMutation.clear(SectorProgressCodec.SLOT_ID),
            SaveMutation.clear(StrategyStateCodec.SLOT_ID),
            SaveMutation.clear(RobotFleetCodec.SLOT_ID),
            SaveMutation.clear(METEOR_SLOT_ID),
        )
        cryosPayload(pending.veteranRobot.id, nowEpochMillis)?.let { mutations += SaveMutation.write(it) }
        mutations += SaveMutation.write(prestigeCodec.encode(finalizedState, nowEpochMillis))

        val transaction = transactionCoordinator.execute(
            transactionId = pending.transferId,
            mutations = mutations,
            nowEpochMillis = nowEpochMillis,
        )
        return if (transaction.status == SaveTransactionStatus.COMMITTED) {
            PlanetTransferCoordinationResult.Committed(finalizedState)
        } else {
            PlanetTransferCoordinationResult.Pending(preparedState, transaction)
        }
    }

    private fun cryosPayload(veteranRobotId: GameId, nowEpochMillis: Long): SavePayload? {
        val existing = loadPayload(CryosIxStateCodec.SLOT_ID, "Cryos IX")
        if (existing != null) {
            val valid = runCatching { cryosCodec.decode(existing) }
                .onFailure { services.logger.warning(TAG, "Cryos IX save is invalid and will be recreated.", it) }
                .isSuccess
            if (valid) return null
        }
        val initial = CryosIxEngine(cryosDefinitions).initialState(veteranRobotId)
        return cryosCodec.encode(initial, cryosDefinitions.contentVersion, nowEpochMillis)
    }

    private fun loadRobots(nowEpochMillis: Long): RobotAutomationState {
        val initial = robotEngine.initialState(nowEpochMillis)
        val payload = loadPayload(RobotFleetCodec.SLOT_ID, "robots") ?: return initial
        return runCatching { robotEngine.normalize(robotCodec.decode(payload), nowEpochMillis) }
            .onFailure { services.logger.warning(TAG, "Robot save could not be decoded; using the initial fleet.", it) }
            .getOrElse { initial }
    }

    private fun loadPayload(slotId: String, label: String): SavePayload? = runCatching {
        services.save.loadLatest(slotId)
    }.onFailure {
        services.logger.warning(TAG, "Unable to read $label save slot.", it)
    }.getOrNull()

    private inline fun <T> loadOptional(
        slotId: String,
        label: String,
        decode: (SavePayload) -> T,
    ): T? {
        val payload = loadPayload(slotId, label) ?: return null
        return runCatching { decode(payload) }
            .onFailure { services.logger.warning(TAG, "$label save could not be decoded and will be ignored.", it) }
            .getOrNull()
    }

    private fun specializationBonus(value: SpecializationId): GameId =
        GameId.of("bonus_specialization_${value.name.lowercase()}")

    private companion object {
        const val TAG = "PlanetTransferCoordinator"
        const val PRIMARY_SLOT_ID = "primary"
        const val METEOR_SLOT_ID = "meteor_event"
        val LAUNCH_SHIPYARD: GameId = GameId.of("sector_launch_shipyard")
    }
}
