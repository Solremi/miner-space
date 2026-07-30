package fr.solremi.minerspace.simulation.offline

import fr.solremi.minerspace.domain.assembly.AssemblyEngine
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.advanceExtraction
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.shared.GameId

data class OfflineProgressPolicy(
    val capacitySeconds: Long = 8L * 60L * 60L,
    val backwardClockToleranceMillis: Long = 2L * 60L * 1_000L,
) {
    init {
        require(capacitySeconds in 0L..24L * 60L * 60L)
        require(backwardClockToleranceMillis >= 0L)
    }
}

data class OfflineProgressReport(
    val absentSeconds: Long,
    val simulatedSeconds: Long,
    val capped: Boolean,
    val clockMovedBackward: Boolean,
    val extractedByResource: Map<GameId, Long>,
    val depletedDepositIds: Set<GameId>,
    val storageBlockedDepositIds: Set<GameId>,
    val refiningCompleted: Int,
    val assemblyCompleted: Int,
) {
    val hasMeaningfulProgress: Boolean
        get() = simulatedSeconds >= 30L || extractedByResource.isNotEmpty() ||
            refiningCompleted > 0 || assemblyCompleted > 0 || capped || clockMovedBackward
}

data class OfflineProgressResult(
    val state: ManufacturingGameState,
    val report: OfflineProgressReport,
    val effectiveNowEpochMillis: Long,
)

class OfflineProgressEngine(
    private val economy: CoreEconomyEngine,
    private val refiner: RefiningEngine,
    private val assembler: AssemblyEngine,
) {
    fun apply(
        state: ManufacturingGameState,
        savedAtEpochMillis: Long,
        nowEpochMillis: Long,
        policy: OfflineProgressPolicy = OfflineProgressPolicy(),
    ): OfflineProgressResult {
        require(savedAtEpochMillis >= 0L)
        require(nowEpochMillis >= 0L)

        val clockMovedBackward = nowEpochMillis + policy.backwardClockToleranceMillis < savedAtEpochMillis
        val rawAbsentMillis = if (clockMovedBackward) {
            0L
        } else {
            (nowEpochMillis - savedAtEpochMillis).coerceAtLeast(0L)
        }
        val absentSeconds = rawAbsentMillis / 1_000L
        val simulatedSeconds = minOf(absentSeconds, policy.capacitySeconds)
        val effectiveNow = Math.addExact(savedAtEpochMillis, Math.multiplyExact(simulatedSeconds, 1_000L))

        val refiningReadyBefore = state.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val assemblyReadyBefore = state.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }
        val extraction = economy.advanceExtraction(
            state.economy,
            simulatedSeconds,
            assembler.productionMultipliers(state.assembly),
        )
        var progressed = state.copy(economy = extraction.state)
        progressed = progressed.withRefining(refiner.reconcile(progressed.refiningView(), effectiveNow))
        progressed = assembler.reconcile(progressed, effectiveNow)

        val extractedByResource = linkedMapOf<GameId, Long>()
        extraction.extractedByDeposit.forEach { (depositId, quantity) ->
            val resourceId = economy.definitions.deposits.getValue(depositId).resourceId
            extractedByResource[resourceId] = Math.addExact(extractedByResource[resourceId] ?: 0L, quantity)
        }
        val depleted = economy.definitions.deposits.keys.filterTo(linkedSetOf()) { id ->
            (state.economy.deposits[id]?.remainingReserve ?: 0L) > 0L &&
                progressed.economy.deposits.getValue(id).remainingReserve == 0L
        }
        val storageBlocked = economy.definitions.deposits.values.mapNotNullTo(linkedSetOf()) { definition ->
            val deposit = progressed.economy.deposits.getValue(definition.id)
            val resource = economy.definitions.resources.getValue(definition.resourceId)
            val stored = progressed.economy.inventory[definition.resourceId] ?: 0L
            val pendingForResource = economy.definitions.deposits.values
                .filter { it.resourceId == definition.resourceId }
                .sumOf { progressed.economy.deposits.getValue(it.id).pendingCollection }
            val blocked = deposit.remainingReserve > 0L &&
                (deposit.pendingCollection >= definition.transportCapacity || stored + pendingForResource >= resource.storageCapacity)
            definition.id.takeIf { blocked }
        }

        return OfflineProgressResult(
            state = progressed,
            report = OfflineProgressReport(
                absentSeconds = absentSeconds,
                simulatedSeconds = simulatedSeconds,
                capped = absentSeconds > simulatedSeconds,
                clockMovedBackward = clockMovedBackward,
                extractedByResource = extractedByResource,
                depletedDepositIds = depleted,
                storageBlockedDepositIds = storageBlocked,
                refiningCompleted = (progressed.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT } - refiningReadyBefore).coerceAtLeast(0),
                assemblyCompleted = (progressed.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT } - assemblyReadyBefore).coerceAtLeast(0),
            ),
            effectiveNowEpochMillis = effectiveNow,
        )
    }
}
