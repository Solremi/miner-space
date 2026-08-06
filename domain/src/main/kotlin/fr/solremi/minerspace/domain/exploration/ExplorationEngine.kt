package fr.solremi.minerspace.domain.exploration

import fr.solremi.minerspace.shared.GameId

class ExplorationEngine(
    val definitions: ExplorationDefinitions,
) {
    fun initialState(): ExplorationState {
        val initial = definitions.sectors.values.filter { it.initiallyUnlocked }.mapTo(linkedSetOf()) { it.id }
        return ExplorationState(
            revealedSectorIds = initial,
            unlockedSectorIds = initial,
            discoveredRareDepositIds = emptySet(),
            spentSpaceDollars = 0L,
            spentComponents = emptyMap(),
            activeMissionSectorId = definitions.sectors.values.firstOrNull { it.initiallyUnlocked && it.missionTarget }?.id,
            transactionSequence = 0L,
        )
    }

    fun normalize(source: ExplorationState): ExplorationState {
        val validIds = definitions.sectors.keys
        val unlocked = source.unlockedSectorIds.filterTo(linkedSetOf(), validIds::contains)
        definitions.sectors.values.filter { it.initiallyUnlocked }.forEach { unlocked += it.id }
        val revealed = source.revealedSectorIds.filterTo(linkedSetOf(), validIds::contains).apply { addAll(unlocked) }
        val rareIds = definitions.sectors.values.mapNotNullTo(linkedSetOf()) { it.rareDepositId }
        return source.copy(
            revealedSectorIds = revealed,
            unlockedSectorIds = unlocked,
            discoveredRareDepositIds = source.discoveredRareDepositIds.filterTo(linkedSetOf(), rareIds::contains),
            spentSpaceDollars = source.spentSpaceDollars.coerceAtLeast(0L),
            spentComponents = source.spentComponents.mapValues { it.value.coerceAtLeast(0L) }.filterValues { it > 0L },
            activeMissionSectorId = source.activeMissionSectorId?.takeIf(validIds::contains),
            transactionSequence = source.transactionSequence.coerceAtLeast(0L),
        )
    }

    fun availability(
        state: ExplorationState,
        sectorId: GameId,
        access: ExplorationAccess,
    ): SectorAvailability {
        val sector = definitions.sectors.getValue(sectorId)
        val availableMoney = access.spaceDollars
        val availableComponents = access.inventory
        return SectorAvailability(
            revealed = sectorId in state.revealedSectorIds,
            unlocked = sectorId in state.unlockedSectorIds,
            scannerSatisfied = access.scannerLevel >= sector.scannerLevelRequired,
            sectorsSatisfied = state.unlockedSectorIds.containsAll(sector.requiredSectorIds),
            technologiesSatisfied = access.installedTechnologyIds.containsAll(sector.requiredTechnologyIds),
            moneySatisfied = availableMoney >= sector.unlockCostSpaceDollars,
            componentsSatisfied = sector.requiredComponents.all { (id, quantity) ->
                (availableComponents[id] ?: 0L) >= quantity
            },
            availableSpaceDollars = availableMoney,
            availableComponents = availableComponents,
        )
    }

    fun scan(
        state: ExplorationState,
        sectorId: GameId,
        access: ExplorationAccess,
    ): ExplorationCommandResult {
        val sector = definitions.sectors[sectorId]
            ?: return ExplorationCommandResult.Rejected(state, "unknown_sector")
        if (sectorId in state.revealedSectorIds) {
            return ExplorationCommandResult.Rejected(state, "sector_already_scanned")
        }
        val availability = availability(state, sectorId, access)
        if (!availability.sectorsSatisfied) return ExplorationCommandResult.Rejected(state, "sector_path_locked")
        if (!availability.scannerSatisfied) return ExplorationCommandResult.Rejected(state, "scanner_level_low")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            revealedSectorIds = state.revealedSectorIds + sector.id,
            activeMissionSectorId = if (sector.missionTarget) sector.id else state.activeMissionSectorId,
            transactionSequence = sequence,
        )
        return ExplorationCommandResult.Applied(
            next,
            ExplorationTransaction(sequence, "scan_sector", sector.id),
        )
    }

    fun unlock(
        state: ExplorationState,
        sectorId: GameId,
        access: ExplorationAccess,
    ): ExplorationCommandResult {
        val sector = definitions.sectors[sectorId]
            ?: return ExplorationCommandResult.Rejected(state, "unknown_sector")
        if (sectorId in state.unlockedSectorIds) {
            return ExplorationCommandResult.Rejected(state, "sector_already_open")
        }
        val availability = availability(state, sectorId, access)
        val rejection = when {
            !availability.revealed -> "sector_not_scanned"
            !availability.sectorsSatisfied -> "sector_path_locked"
            !availability.scannerSatisfied -> "scanner_level_low"
            !availability.technologiesSatisfied -> "technology_prerequisite_missing"
            !availability.moneySatisfied -> "insufficient_space_dollars"
            !availability.componentsSatisfied -> "missing_sector_component"
            else -> null
        }
        if (rejection != null) return ExplorationCommandResult.Rejected(state, rejection)

        val componentSpending = state.spentComponents.toMutableMap()
        sector.requiredComponents.forEach { (id, quantity) ->
            componentSpending[id] = Math.addExact(componentSpending[id] ?: 0L, quantity)
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            unlockedSectorIds = state.unlockedSectorIds + sector.id,
            discoveredRareDepositIds = sector.rareDepositId?.let { state.discoveredRareDepositIds + it }
                ?: state.discoveredRareDepositIds,
            spentSpaceDollars = Math.addExact(state.spentSpaceDollars, sector.unlockCostSpaceDollars),
            spentComponents = componentSpending,
            activeMissionSectorId = if (sector.missionTarget) sector.id else state.activeMissionSectorId,
            transactionSequence = sequence,
        )
        return ExplorationCommandResult.Applied(
            next,
            ExplorationTransaction(
                sequence = sequence,
                reason = "unlock_sector",
                sectorId = sector.id,
                spaceDollarCost = sector.unlockCostSpaceDollars,
                componentCosts = sector.requiredComponents,
                rareDepositId = sector.rareDepositId,
            ),
        )
    }

    fun setMissionTarget(state: ExplorationState, sectorId: GameId): ExplorationCommandResult {
        val sector = definitions.sectors[sectorId]
            ?: return ExplorationCommandResult.Rejected(state, "unknown_sector")
        if (!sector.missionTarget) return ExplorationCommandResult.Rejected(state, "sector_not_mission_target")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return ExplorationCommandResult.Applied(
            state.copy(activeMissionSectorId = sectorId, transactionSequence = sequence),
            ExplorationTransaction(sequence, "set_mission_target", sectorId),
        )
    }
}
