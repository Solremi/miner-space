package fr.solremi.minerspace.domain.prestige

import fr.solremi.minerspace.domain.robot.RobotInstance
import fr.solremi.minerspace.domain.robot.RobotStatistics
import fr.solremi.minerspace.domain.robot.RobotTrait
import fr.solremi.minerspace.shared.GameId

enum class PlanetId { FERRUM_DELTA, CRYOS_IX }

data class VeteranRobotSnapshot(
    val id: GameId,
    val displayName: String,
    val serialNumber: String,
    val level: Int,
    val trait: RobotTrait,
    val masteryPoints: Long,
    val statistics: RobotStatistics,
) {
    init {
        require(displayName.isNotBlank() && serialNumber.isNotBlank())
        require(level >= 1 && masteryPoints >= 0L)
    }

    companion object {
        fun from(robot: RobotInstance): VeteranRobotSnapshot = VeteranRobotSnapshot(
            id = robot.id,
            displayName = robot.displayName,
            serialNumber = robot.serialNumber,
            level = robot.level,
            trait = robot.trait,
            masteryPoints = robot.masteryPoints,
            statistics = robot.statistics,
        )
    }
}

data class PrestigeSnapshot(
    val launchShipyardUnlocked: Boolean,
    val discoveredCodexEntryIds: Set<GameId>,
    val archiveIds: Set<GameId>,
    val permanentBonusIds: Set<GameId>,
    val robots: Collection<RobotInstance>,
) {
    init { require(robots.map { it.id }.distinct().size == robots.size) }
}

data class PendingPlanetTransfer(
    val transferId: String,
    val sourcePlanet: PlanetId,
    val destinationPlanet: PlanetId,
    val expectedStellarCores: Long,
    val preservedCodexEntryIds: Set<GameId>,
    val preservedArchiveIds: Set<GameId>,
    val preservedBonusIds: Set<GameId>,
    val veteranRobot: VeteranRobotSnapshot,
    val preparedAtEpochMillis: Long,
) {
    init {
        require(transferId.isNotBlank())
        require(sourcePlanet != destinationPlanet)
        require(expectedStellarCores >= 0L && preparedAtEpochMillis >= 0L)
    }
}

data class PrestigeState(
    val activePlanet: PlanetId,
    val stellarCores: Long,
    val completedTransfers: Int,
    val permanentCodexEntryIds: Set<GameId>,
    val permanentArchiveIds: Set<GameId>,
    val permanentBonusIds: Set<GameId>,
    val veteranRobot: VeteranRobotSnapshot?,
    val pendingTransfer: PendingPlanetTransfer?,
    val transactionSequence: Long,
) {
    init {
        require(stellarCores >= 0L && completedTransfers >= 0 && transactionSequence >= 0L)
        pendingTransfer?.let { require(it.sourcePlanet == activePlanet || activePlanet == it.destinationPlanet) }
    }
}

data class PrestigeTransaction(
    val sequence: Long,
    val reason: String,
    val transferId: String,
)

sealed interface PrestigeCommandResult {
    val state: PrestigeState

    data class Applied(
        override val state: PrestigeState,
        val transaction: PrestigeTransaction,
    ) : PrestigeCommandResult

    data class Rejected(
        override val state: PrestigeState,
        val code: String,
    ) : PrestigeCommandResult
}

class PlanetPrestigeEngine(
    private val stellarCoreReward: Long = 3L,
    private val veteranMasteryThreshold: Long = 6_000L,
) {
    init { require(stellarCoreReward > 0L && veteranMasteryThreshold > 0L) }

    fun initialState(): PrestigeState = PrestigeState(
        activePlanet = PlanetId.FERRUM_DELTA,
        stellarCores = 0L,
        completedTransfers = 0,
        permanentCodexEntryIds = emptySet(),
        permanentArchiveIds = emptySet(),
        permanentBonusIds = emptySet(),
        veteranRobot = null,
        pendingTransfer = null,
        transactionSequence = 0L,
    )

    fun normalize(source: PrestigeState): PrestigeState = source.copy(
        stellarCores = source.stellarCores.coerceAtLeast(0L),
        completedTransfers = source.completedTransfers.coerceAtLeast(0),
        transactionSequence = source.transactionSequence.coerceAtLeast(0L),
    )

    fun prepareTransfer(
        state: PrestigeState,
        snapshot: PrestigeSnapshot,
        nowEpochMillis: Long,
    ): PrestigeCommandResult {
        require(nowEpochMillis >= 0L)
        if (state.pendingTransfer != null) return PrestigeCommandResult.Rejected(state, "transfer_already_prepared")
        if (state.activePlanet != PlanetId.FERRUM_DELTA) return PrestigeCommandResult.Rejected(state, "source_planet_not_active")
        if (!snapshot.launchShipyardUnlocked) return PrestigeCommandResult.Rejected(state, "launch_shipyard_locked")
        val veteran = snapshot.robots
            .filter { it.masteryPoints >= veteranMasteryThreshold }
            .maxWithOrNull(compareBy<RobotInstance> { it.masteryPoints }.thenBy { it.statistics.activeSeconds })
            ?: return PrestigeCommandResult.Rejected(state, "veteran_robot_required")

        val sequence = Math.addExact(state.transactionSequence, 1L)
        val expectedCores = Math.addExact(state.stellarCores, stellarCoreReward)
        val transfer = PendingPlanetTransfer(
            transferId = "transfer_${sequence}_ferrum_cryos",
            sourcePlanet = PlanetId.FERRUM_DELTA,
            destinationPlanet = PlanetId.CRYOS_IX,
            expectedStellarCores = expectedCores,
            preservedCodexEntryIds = state.permanentCodexEntryIds + snapshot.discoveredCodexEntryIds,
            preservedArchiveIds = state.permanentArchiveIds + snapshot.archiveIds,
            preservedBonusIds = state.permanentBonusIds + snapshot.permanentBonusIds + LEGACY_BONUS,
            veteranRobot = VeteranRobotSnapshot.from(veteran),
            preparedAtEpochMillis = nowEpochMillis,
        )
        return PrestigeCommandResult.Applied(
            state.copy(pendingTransfer = transfer, transactionSequence = sequence),
            PrestigeTransaction(sequence, "prepare_planet_transfer", transfer.transferId),
        )
    }

    /** Idempotent reconciliation: repeated calls never add cores or duplicate permanent sets. */
    fun reconcilePrepared(state: PrestigeState): PrestigeCommandResult {
        val transfer = state.pendingTransfer ?: return PrestigeCommandResult.Rejected(state, "no_pending_transfer")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            activePlanet = transfer.destinationPlanet,
            stellarCores = maxOf(state.stellarCores, transfer.expectedStellarCores),
            permanentCodexEntryIds = state.permanentCodexEntryIds + transfer.preservedCodexEntryIds,
            permanentArchiveIds = state.permanentArchiveIds + transfer.preservedArchiveIds,
            permanentBonusIds = state.permanentBonusIds + transfer.preservedBonusIds,
            veteranRobot = transfer.veteranRobot,
            transactionSequence = sequence,
        )
        return PrestigeCommandResult.Applied(
            next,
            PrestigeTransaction(sequence, "reconcile_planet_transfer", transfer.transferId),
        )
    }

    fun finalizeTransfer(state: PrestigeState): PrestigeCommandResult {
        val transfer = state.pendingTransfer ?: return PrestigeCommandResult.Rejected(state, "no_pending_transfer")
        if (state.activePlanet != transfer.destinationPlanet) {
            return PrestigeCommandResult.Rejected(state, "destination_not_reconciled")
        }
        if (state.stellarCores < transfer.expectedStellarCores || state.veteranRobot?.id != transfer.veteranRobot.id) {
            return PrestigeCommandResult.Rejected(state, "permanent_state_incomplete")
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return PrestigeCommandResult.Applied(
            state.copy(
                completedTransfers = maxOf(state.completedTransfers, 1),
                pendingTransfer = null,
                transactionSequence = sequence,
            ),
            PrestigeTransaction(sequence, "finalize_planet_transfer", transfer.transferId),
        )
    }

    companion object {
        val LEGACY_BONUS: GameId = GameId.of("bonus_ferrum_legacy")
    }
}
