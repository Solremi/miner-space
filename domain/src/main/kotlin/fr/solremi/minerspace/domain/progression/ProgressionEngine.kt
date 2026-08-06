package fr.solremi.minerspace.domain.progression

import fr.solremi.minerspace.shared.GameId

class ProgressionEngine(val definitions: ProgressionDefinitions) {
    fun initialState(): ProgressionState = ProgressionState(
        tutorialStepIndex = 0,
        completedTutorialIds = emptySet(),
        claimedMissionIds = emptySet(),
        contractCycles = ContractTier.entries.associateWith { 0 },
        completedContractCount = 0L,
        discoveredCodexEntryIds = emptySet(),
        claimedCollectionIds = emptySet(),
        selectedObjectiveId = null,
        transactionSequence = 0L,
    )

    fun normalize(source: ProgressionState): ProgressionState {
        val tutorialIds = definitions.tutorialSteps.mapTo(linkedSetOf()) { it.id }
        val missionIds = definitions.missions.keys
        val codexIds = definitions.codexEntries.keys
        val collectionIds = definitions.collections.keys
        val index = source.tutorialStepIndex.coerceIn(0, definitions.tutorialSteps.size)
        return source.copy(
            tutorialStepIndex = index,
            completedTutorialIds = (
                source.completedTutorialIds.filterTo(linkedSetOf(), tutorialIds::contains) +
                    definitions.tutorialSteps.take(index).map { it.id }
                ).toSet(),
            claimedMissionIds = source.claimedMissionIds.filterTo(linkedSetOf(), missionIds::contains),
            contractCycles = ContractTier.entries.associateWith { (source.contractCycles[it] ?: 0).coerceAtLeast(0) },
            completedContractCount = source.completedContractCount.coerceAtLeast(0L),
            discoveredCodexEntryIds = source.discoveredCodexEntryIds.filterTo(linkedSetOf(), codexIds::contains),
            claimedCollectionIds = source.claimedCollectionIds.filterTo(linkedSetOf(), collectionIds::contains),
            selectedObjectiveId = source.selectedObjectiveId?.takeIf(missionIds::contains),
            transactionSequence = source.transactionSequence.coerceAtLeast(0L),
        )
    }

    fun synchronize(source: ProgressionState, snapshot: ProgressSnapshot): ProgressionState {
        var state = normalize(source)
        var index = state.tutorialStepIndex
        val completed = state.completedTutorialIds.toMutableSet()
        while (index < definitions.tutorialSteps.size) {
            val step = definitions.tutorialSteps[index]
            if (snapshot.metricValue(step.metric) < step.target) break
            completed += step.id
            index++
        }
        val discovered = state.discoveredCodexEntryIds.toMutableSet()
        definitions.codexEntries.values.forEach { entry ->
            if (
                state.claimedMissionIds.containsAll(entry.requiredMissionIds) &&
                snapshot.metricValue(entry.metric) >= entry.target
            ) {
                discovered += entry.id
            }
        }
        state = state.copy(
            tutorialStepIndex = index,
            completedTutorialIds = completed,
            discoveredCodexEntryIds = discovered,
        )
        val active = objectiveViews(state, snapshot).mapTo(linkedSetOf()) { it.definition.id }
        return state.copy(
            selectedObjectiveId = state.selectedObjectiveId?.takeIf(active::contains) ?: active.firstOrNull(),
        )
    }

    fun tutorialProgress(state: ProgressionState, snapshot: ProgressSnapshot): TutorialProgress {
        val normalized = normalize(state)
        val step = definitions.tutorialSteps.getOrNull(normalized.tutorialStepIndex)
        return TutorialProgress(
            step,
            step?.let { snapshot.metricValue(it.metric).coerceAtMost(it.target) } ?: 0L,
            normalized.tutorialStepIndex,
            definitions.tutorialSteps.size,
        )
    }

    fun objectiveViews(state: ProgressionState, snapshot: ProgressSnapshot): List<MissionView> =
        definitions.missions.values.asSequence()
            .filter { state.claimedMissionIds.containsAll(it.requiredMissionIds) && it.id !in state.claimedMissionIds }
            .map { mission ->
                val value = snapshot.metricValue(mission.metric)
                MissionView(mission, value.coerceAtMost(mission.target), value >= mission.target)
            }
            .sortedWith(
                compareBy<MissionView> { it.definition.kind.ordinal }
                    .thenByDescending { it.completed }
                    .thenBy { it.definition.id.value },
            )
            .toList()

    fun claimMission(
        state: ProgressionState,
        missionId: GameId,
        snapshot: ProgressSnapshot,
    ): ProgressionCommandResult {
        val mission = definitions.missions[missionId]
            ?: return ProgressionCommandResult.Rejected(state, "unknown_mission")
        if (missionId in state.claimedMissionIds) {
            return ProgressionCommandResult.Rejected(state, "mission_already_claimed")
        }
        if (!state.claimedMissionIds.containsAll(mission.requiredMissionIds)) {
            return ProgressionCommandResult.Rejected(state, "mission_prerequisite_missing")
        }
        if (snapshot.metricValue(mission.metric) < mission.target) {
            return ProgressionCommandResult.Rejected(state, "mission_incomplete")
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            claimedMissionIds = state.claimedMissionIds + missionId,
            selectedObjectiveId = null,
            transactionSequence = sequence,
        )
        return ProgressionCommandResult.Applied(
            next,
            ProgressionTransaction(
                sequence,
                if (mission.kind == MissionKind.ACHIEVEMENT) "claim_achievement" else "claim_mission",
                missionId.value,
                EconomicDelta(spaceDollarsDelta = mission.rewardSpaceDollars),
            ),
        )
    }

    fun activeContracts(state: ProgressionState, snapshot: ProgressSnapshot): List<ContractView> =
        ContractTier.entries.map { tier ->
            val pool = definitions.contracts.filter { it.tier == tier }
            val cycle = state.contractCycles[tier] ?: 0
            val definition = pool[cycle % pool.size]
            ContractView(
                "${definition.id.value}#$cycle",
                definition,
                snapshot.inventory[definition.resourceId] ?: 0L,
                state.claimedMissionIds.containsAll(definition.requiredMissionIds),
            )
        }

    fun deliverContract(
        state: ProgressionState,
        occurrenceId: String,
        snapshot: ProgressSnapshot,
    ): ProgressionCommandResult {
        val active = activeContracts(state, snapshot).firstOrNull { it.occurrenceId == occurrenceId }
            ?: return ProgressionCommandResult.Rejected(state, "contract_not_active")
        if (!active.unlocked) return ProgressionCommandResult.Rejected(state, "contract_locked")
        if (!active.deliverable) return ProgressionCommandResult.Rejected(state, "contract_inventory_missing")
        val tier = active.definition.tier
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            contractCycles = state.contractCycles + (
                tier to Math.addExact(state.contractCycles.getValue(tier), 1)
                ),
            completedContractCount = Math.addExact(state.completedContractCount, 1L),
            transactionSequence = sequence,
        )
        return ProgressionCommandResult.Applied(
            next,
            ProgressionTransaction(
                sequence,
                "deliver_contract",
                occurrenceId,
                EconomicDelta(
                    active.definition.rewardSpaceDollars,
                    mapOf(active.definition.resourceId to -active.definition.quantity),
                ),
            ),
        )
    }

    fun visibleCodexEntries(state: ProgressionState, snapshot: ProgressSnapshot): List<CodexEntryView> =
        definitions.codexEntries.values.asSequence()
            .filter { entry ->
                entry.id in state.discoveredCodexEntryIds ||
                    (
                        state.claimedMissionIds.containsAll(entry.requiredMissionIds) &&
                            (entry.target == 0L || snapshot.metricValue(entry.metric) > 0L)
                        )
            }
            .map {
                CodexEntryView(
                    it,
                    it.id in state.discoveredCodexEntryIds,
                    snapshot.metricValue(it.metric).coerceAtMost(it.target),
                )
            }
            .sortedWith(
                compareByDescending<CodexEntryView> { it.discovered }
                    .thenBy { it.definition.id.value },
            )
            .toList()

    fun collectionViews(state: ProgressionState): List<CollectionView> =
        definitions.collections.values.map { collection ->
            CollectionView(
                collection,
                collection.entryIds.count(state.discoveredCodexEntryIds::contains),
                collection.id in state.claimedCollectionIds,
            )
        }.filter { it.discoveredEntries > 0 || it.claimed }

    fun claimCollection(
        state: ProgressionState,
        collectionId: GameId,
    ): ProgressionCommandResult {
        val collection = definitions.collections[collectionId]
            ?: return ProgressionCommandResult.Rejected(state, "unknown_collection")
        if (collectionId in state.claimedCollectionIds) {
            return ProgressionCommandResult.Rejected(state, "collection_already_claimed")
        }
        if (!state.discoveredCodexEntryIds.containsAll(collection.entryIds)) {
            return ProgressionCommandResult.Rejected(state, "collection_incomplete")
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            claimedCollectionIds = state.claimedCollectionIds + collectionId,
            transactionSequence = sequence,
        )
        return ProgressionCommandResult.Applied(
            next,
            ProgressionTransaction(
                sequence,
                "claim_collection",
                collectionId.value,
                EconomicDelta(spaceDollarsDelta = collection.rewardSpaceDollars),
            ),
        )
    }

    fun selectObjective(state: ProgressionState, missionId: GameId): ProgressionState =
        if (missionId in definitions.missions) state.copy(selectedObjectiveId = missionId) else state
}
