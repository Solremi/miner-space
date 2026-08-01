package fr.solremi.minerspace.data.offline

import fr.solremi.minerspace.data.assembly.AssemblyContentLoader
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.refining.RefiningContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.ManufacturingStateMigrator
import fr.solremi.minerspace.domain.assembly.AssemblyEngine
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.advanceExtraction
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId

data class OfflineReturnReport(
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

data class OfflineReturnSession(
    val baseState: ManufacturingGameState?,
    val currentState: ManufacturingGameState?,
    val report: OfflineReturnReport?,
    val scopeId: String?,
    val savedAtEpochMillis: Long,
    val nowEpochMillis: Long,
    val recoveredOlderSnapshot: Boolean,
    val migrated: Boolean,
    val saveFailed: Boolean,
    val unrecoverable: Boolean,
    val doubled: Boolean = false,
) {
    val shouldShow: Boolean
        get() = unrecoverable || recoveredOlderSnapshot || migrated || saveFailed ||
            report?.hasMeaningfulProgress == true
}

class OfflineReturnCoordinator(
    private val services: GameServices,
) {
    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val refiningDefinitions = RefiningContentLoader().load(services.content)
    private val assemblyDefinitions = AssemblyContentLoader().load(services.content)
    private val codec = ManufacturingSnapshotCodec()
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val refiner = RefiningEngine(
        refiningDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )
    private val assembler = AssemblyEngine(
        assemblyDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )

    fun loadAndApplyStandard(): OfflineReturnSession {
        val payload = services.save.loadLatest() ?: return emptySession()
        return runCatching {
            require(refiningDefinitions.contentVersion == economyDefinitions.contentVersion)
            require(assemblyDefinitions.contentVersion == economyDefinitions.contentVersion)
            val decoded = codec.decodeWithMetadata(payload)
            val migrated = ManufacturingStateMigrator(
                economyDefinitions,
                refiningDefinitions,
                assemblyDefinitions,
            ).migrate(decoded.state)
            economy.requireValid(migrated.state.economy)
            val now = services.clock.nowEpochMillis().coerceAtLeast(0L)
            val standard = calculate(
                state = migrated.state,
                savedAtEpochMillis = payload.savedAtEpochMillis,
                nowEpochMillis = now,
                multiplier = 1,
            )
            val needsRewrite = decoded.requiresRewrite || migrated.changed ||
                payload.contentVersion != economyDefinitions.contentVersion ||
                payload.recoveredFromFallback || standard.report.simulatedSeconds > 0L
            val writeSucceeded = !needsRewrite || save(standard.state, now)
            OfflineReturnSession(
                baseState = migrated.state,
                currentState = standard.state,
                report = standard.report,
                scopeId = "return_${payload.sequence}_${payload.savedAtEpochMillis}",
                savedAtEpochMillis = payload.savedAtEpochMillis,
                nowEpochMillis = now,
                recoveredOlderSnapshot = payload.recoveredFromFallback,
                migrated = decoded.requiresRewrite || migrated.changed ||
                    payload.contentVersion != economyDefinitions.contentVersion,
                saveFailed = !writeSucceeded,
                unrecoverable = false,
            )
        }.onFailure {
            services.logger.error(TAG, "Offline return could not restore the manufacturing state.", it)
        }.getOrElse {
            services.save.clear()
            OfflineReturnSession(
                baseState = null,
                currentState = null,
                report = null,
                scopeId = null,
                savedAtEpochMillis = 0L,
                nowEpochMillis = services.clock.nowEpochMillis().coerceAtLeast(0L),
                recoveredOlderSnapshot = false,
                migrated = false,
                saveFailed = false,
                unrecoverable = true,
            )
        }
    }

    fun doubled(session: OfflineReturnSession): OfflineReturnSession? {
        val base = session.baseState ?: return null
        if (session.unrecoverable || session.report == null) return null
        val doubled = calculate(
            state = base,
            savedAtEpochMillis = session.savedAtEpochMillis,
            nowEpochMillis = session.nowEpochMillis,
            multiplier = 2,
        )
        return session.copy(
            currentState = doubled.state,
            report = doubled.report,
            doubled = true,
            saveFailed = false,
        )
    }

    fun encode(state: ManufacturingGameState, savedAtEpochMillis: Long): SavePayload = codec.encode(
        state = state,
        contentVersion = economyDefinitions.contentVersion,
        savedAtEpochMillis = savedAtEpochMillis.coerceAtLeast(0L),
    )

    private fun calculate(
        state: ManufacturingGameState,
        savedAtEpochMillis: Long,
        nowEpochMillis: Long,
        multiplier: Int,
    ): OfflineCalculation {
        require(multiplier in 1..2)
        val clockMovedBackward = nowEpochMillis + BACKWARD_TOLERANCE_MILLIS < savedAtEpochMillis
        val rawAbsentMillis = if (clockMovedBackward) 0L else
            (nowEpochMillis - savedAtEpochMillis).coerceAtLeast(0L)
        val absentSeconds = rawAbsentMillis / 1_000L
        val requestedSeconds = Math.multiplyExact(absentSeconds, multiplier.toLong())
        val simulatedSeconds = minOf(requestedSeconds, CAPACITY_SECONDS)
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
            val pending = economy.definitions.deposits.values
                .filter { it.resourceId == definition.resourceId }
                .sumOf { progressed.economy.deposits.getValue(it.id).pendingCollection }
            definition.id.takeIf {
                deposit.remainingReserve > 0L &&
                    (deposit.pendingCollection >= definition.transportCapacity ||
                        stored + pending >= resource.storageCapacity)
            }
        }
        return OfflineCalculation(
            progressed,
            OfflineReturnReport(
                absentSeconds = absentSeconds,
                simulatedSeconds = simulatedSeconds,
                capped = requestedSeconds > simulatedSeconds,
                clockMovedBackward = clockMovedBackward,
                extractedByResource = extractedByResource,
                depletedDepositIds = depleted,
                storageBlockedDepositIds = storageBlocked,
                refiningCompleted = (progressed.refining.jobs.count {
                    it.status == RefiningJobStatus.READY_TO_COLLECT
                } - refiningReadyBefore).coerceAtLeast(0),
                assemblyCompleted = (progressed.assembly.jobs.count {
                    it.status == AssemblyJobStatus.READY_TO_COLLECT
                } - assemblyReadyBefore).coerceAtLeast(0),
            ),
        )
    }

    private fun save(state: ManufacturingGameState, savedAt: Long): Boolean =
        services.save.save(encode(state, savedAt)) == SaveWriteStatus.WRITTEN

    private fun emptySession(): OfflineReturnSession = OfflineReturnSession(
        baseState = null,
        currentState = null,
        report = null,
        scopeId = null,
        savedAtEpochMillis = 0L,
        nowEpochMillis = services.clock.nowEpochMillis().coerceAtLeast(0L),
        recoveredOlderSnapshot = false,
        migrated = false,
        saveFailed = false,
        unrecoverable = false,
    )

    private data class OfflineCalculation(
        val state: ManufacturingGameState,
        val report: OfflineReturnReport,
    )

    private companion object {
        const val TAG = "OfflineReturnCoordinator"
        const val CAPACITY_SECONDS = 8L * 60L * 60L
        const val BACKWARD_TOLERANCE_MILLIS = 2L * 60L * 1_000L
    }
}
