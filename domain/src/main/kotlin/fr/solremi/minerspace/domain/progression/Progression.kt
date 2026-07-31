package fr.solremi.minerspace.domain.progression

import fr.solremi.minerspace.shared.GameId

enum class ProgressMetric { RAW_TOTAL, RAW_IRON, RAW_COPPER, RAW_CRYSTAL, REFINED_TOTAL, REFINED_IRON, COMPONENT_TOTAL, POWER_CELLS, TECHNOLOGIES, SECTORS, RARE_DISCOVERIES, ROBOT_LEVEL_SUM, ROBOT_MASTERY, MODULES, SPECIALIZATION, SPACE_DOLLARS }
enum class MissionKind { MAIN, SECONDARY, ACHIEVEMENT }
enum class ContractTier { SIMPLE, PROFITABLE, AMBITIOUS }
enum class CodexCategory { RESOURCE, INDUSTRY, EXPLORATION, ROBOT, STRATEGY }

data class ProgressSnapshot(
    val inventory: Map<GameId, Long>,
    val spaceDollars: Long,
    val installedTechnologyCount: Int,
    val unlockedSectorCount: Int,
    val rareDiscoveryCount: Int,
    val robotLevelSum: Int,
    val robotMasteryPoints: Long,
    val ownedModuleCount: Int,
    val specializationChosen: Boolean,
) {
    init {
        require(inventory.values.none { it < 0L })
        require(spaceDollars >= 0L && installedTechnologyCount >= 0 && unlockedSectorCount >= 0)
        require(rareDiscoveryCount >= 0 && robotLevelSum >= 0 && robotMasteryPoints >= 0L && ownedModuleCount >= 0)
    }

    fun metricValue(metric: ProgressMetric): Long = when (metric) {
        ProgressMetric.RAW_TOTAL -> sumResources("raw_")
        ProgressMetric.RAW_IRON -> inventory[RAW_IRON] ?: 0L
        ProgressMetric.RAW_COPPER -> inventory[RAW_COPPER] ?: 0L
        ProgressMetric.RAW_CRYSTAL -> inventory[RAW_CRYSTAL] ?: 0L
        ProgressMetric.REFINED_TOTAL -> sumResources("refined_")
        ProgressMetric.REFINED_IRON -> inventory[REFINED_IRON] ?: 0L
        ProgressMetric.COMPONENT_TOTAL -> sumResources("component_")
        ProgressMetric.POWER_CELLS -> inventory[POWER_CELL] ?: 0L
        ProgressMetric.TECHNOLOGIES -> installedTechnologyCount.toLong()
        ProgressMetric.SECTORS -> unlockedSectorCount.toLong()
        ProgressMetric.RARE_DISCOVERIES -> rareDiscoveryCount.toLong()
        ProgressMetric.ROBOT_LEVEL_SUM -> robotLevelSum.toLong()
        ProgressMetric.ROBOT_MASTERY -> robotMasteryPoints
        ProgressMetric.MODULES -> ownedModuleCount.toLong()
        ProgressMetric.SPECIALIZATION -> if (specializationChosen) 1L else 0L
        ProgressMetric.SPACE_DOLLARS -> spaceDollars
    }

    private fun sumResources(prefix: String): Long = inventory.entries.asSequence()
        .filter { it.key.value.startsWith(prefix) }
        .fold(0L) { total, entry -> Math.addExact(total, entry.value) }

    private companion object {
        val RAW_IRON = GameId.of("raw_iron")
        val RAW_COPPER = GameId.of("raw_copper")
        val RAW_CRYSTAL = GameId.of("raw_crystal")
        val REFINED_IRON = GameId.of("refined_iron_ingot")
        val POWER_CELL = GameId.of("component_power_cell")
    }
}

data class TutorialStepDefinition(val id: GameId, val phaseLabel: String, val titleKey: String, val actionKey: String, val metric: ProgressMetric, val target: Long) {
    init { require(phaseLabel.isNotBlank() && titleKey.isNotBlank() && actionKey.isNotBlank() && target > 0L) }
}

data class MissionDefinition(val id: GameId, val kind: MissionKind, val titleKey: String, val metric: ProgressMetric, val target: Long, val rewardSpaceDollars: Long, val requiredMissionIds: Set<GameId>) {
    init { require(titleKey.isNotBlank() && target > 0L && rewardSpaceDollars >= 0L && id !in requiredMissionIds) }
}

data class ContractDefinition(val id: GameId, val tier: ContractTier, val titleKey: String, val resourceId: GameId, val quantity: Long, val rewardSpaceDollars: Long, val requiredMissionIds: Set<GameId>) {
    init { require(titleKey.isNotBlank() && quantity > 0L && rewardSpaceDollars > 0L) }
}

data class CodexEntryDefinition(val id: GameId, val category: CodexCategory, val titleKey: String, val metric: ProgressMetric, val target: Long, val requiredMissionIds: Set<GameId>, val collectionId: GameId?) {
    init { require(titleKey.isNotBlank() && target >= 0L) }
}

data class CollectionDefinition(val id: GameId, val titleKey: String, val entryIds: Set<GameId>, val rewardSpaceDollars: Long) {
    init { require(titleKey.isNotBlank() && entryIds.isNotEmpty() && rewardSpaceDollars >= 0L) }
}

data class ProgressionDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val tutorialSteps: List<TutorialStepDefinition>,
    val missions: Map<GameId, MissionDefinition>,
    val contracts: List<ContractDefinition>,
    val codexEntries: Map<GameId, CodexEntryDefinition>,
    val collections: Map<GameId, CollectionDefinition>,
) {
    init {
        require(schemaVersion > 0 && contentVersion.isNotBlank())
        require(tutorialSteps.size == 7 && tutorialSteps.map { it.id }.distinct().size == tutorialSteps.size)
        require(missions.isNotEmpty())
        require(contracts.groupBy { it.tier }.keys == ContractTier.entries.toSet())
        require(codexEntries.isNotEmpty())
        missions.values.forEach { require(it.requiredMissionIds.all(missions::containsKey)) }
        contracts.forEach { require(it.requiredMissionIds.all(missions::containsKey)) }
        codexEntries.values.forEach { entry ->
            require(entry.requiredMissionIds.all(missions::containsKey))
            entry.collectionId?.let { require(collections.containsKey(it)) }
        }
        collections.values.forEach { require(it.entryIds.all(codexEntries::containsKey)) }
        require(noMissionDependencyCycle())
    }

    private fun noMissionDependencyCycle(): Boolean {
        val visiting = mutableSetOf<GameId>(); val visited = mutableSetOf<GameId>()
        fun visit(id: GameId): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            val valid = missions.getValue(id).requiredMissionIds.all(::visit)
            visiting.remove(id); visited += id
            return valid
        }
        return missions.keys.all(::visit)
    }
}

data class ProgressionState(
    val tutorialStepIndex: Int,
    val completedTutorialIds: Set<GameId>,
    val claimedMissionIds: Set<GameId>,
    val contractCycles: Map<ContractTier, Int>,
    val completedContractCount: Long,
    val discoveredCodexEntryIds: Set<GameId>,
    val claimedCollectionIds: Set<GameId>,
    val selectedObjectiveId: GameId?,
    val transactionSequence: Long,
) {
    init { require(tutorialStepIndex >= 0 && contractCycles.values.none { it < 0 } && completedContractCount >= 0L && transactionSequence >= 0L) }
}

data class TutorialProgress(val step: TutorialStepDefinition?, val current: Long, val completed: Int, val total: Int)
data class MissionView(val definition: MissionDefinition, val current: Long, val completed: Boolean) { val claimable get() = completed }
data class ContractView(val occurrenceId: String, val definition: ContractDefinition, val currentInventory: Long, val unlocked: Boolean) { val deliverable get() = unlocked && currentInventory >= definition.quantity }
data class CodexEntryView(val definition: CodexEntryDefinition, val discovered: Boolean, val current: Long)
data class CollectionView(val definition: CollectionDefinition, val discoveredEntries: Int, val claimed: Boolean) { val complete get() = discoveredEntries == definition.entryIds.size; val claimable get() = complete && !claimed }
data class EconomicDelta(val spaceDollarsDelta: Long = 0L, val inventoryDelta: Map<GameId, Long> = emptyMap()) { init { require(inventoryDelta.values.none { it == 0L }) } }
data class ProgressionTransaction(val sequence: Long, val reason: String, val referenceId: String, val delta: EconomicDelta)

sealed interface ProgressionCommandResult {
    val state: ProgressionState
    data class Applied(override val state: ProgressionState, val transaction: ProgressionTransaction) : ProgressionCommandResult
    data class Rejected(override val state: ProgressionState, val code: String) : ProgressionCommandResult
}

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
            completedTutorialIds = (source.completedTutorialIds.filterTo(linkedSetOf(), tutorialIds::contains) + definitions.tutorialSteps.take(index).map { it.id }).toSet(),
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
            completed += step.id; index++
        }
        val discovered = state.discoveredCodexEntryIds.toMutableSet()
        definitions.codexEntries.values.forEach { entry ->
            if (state.claimedMissionIds.containsAll(entry.requiredMissionIds) && snapshot.metricValue(entry.metric) >= entry.target) discovered += entry.id
        }
        state = state.copy(tutorialStepIndex = index, completedTutorialIds = completed, discoveredCodexEntryIds = discovered)
        val active = objectiveViews(state, snapshot).mapTo(linkedSetOf()) { it.definition.id }
        return state.copy(selectedObjectiveId = state.selectedObjectiveId?.takeIf(active::contains) ?: active.firstOrNull())
    }

    fun tutorialProgress(state: ProgressionState, snapshot: ProgressSnapshot): TutorialProgress {
        val normalized = normalize(state)
        val step = definitions.tutorialSteps.getOrNull(normalized.tutorialStepIndex)
        return TutorialProgress(step, step?.let { snapshot.metricValue(it.metric).coerceAtMost(it.target) } ?: 0L, normalized.tutorialStepIndex, definitions.tutorialSteps.size)
    }

    fun objectiveViews(state: ProgressionState, snapshot: ProgressSnapshot): List<MissionView> = definitions.missions.values.asSequence()
        .filter { state.claimedMissionIds.containsAll(it.requiredMissionIds) && it.id !in state.claimedMissionIds }
        .map { mission -> val value = snapshot.metricValue(mission.metric); MissionView(mission, value.coerceAtMost(mission.target), value >= mission.target) }
        .sortedWith(compareBy<MissionView> { it.definition.kind.ordinal }.thenByDescending { it.completed }.thenBy { it.definition.id.value })
        .toList()

    fun claimMission(state: ProgressionState, missionId: GameId, snapshot: ProgressSnapshot): ProgressionCommandResult {
        val mission = definitions.missions[missionId] ?: return ProgressionCommandResult.Rejected(state, "unknown_mission")
        if (missionId in state.claimedMissionIds) return ProgressionCommandResult.Rejected(state, "mission_already_claimed")
        if (!state.claimedMissionIds.containsAll(mission.requiredMissionIds)) return ProgressionCommandResult.Rejected(state, "mission_prerequisite_missing")
        if (snapshot.metricValue(mission.metric) < mission.target) return ProgressionCommandResult.Rejected(state, "mission_incomplete")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(claimedMissionIds = state.claimedMissionIds + missionId, selectedObjectiveId = null, transactionSequence = sequence)
        return ProgressionCommandResult.Applied(next, ProgressionTransaction(sequence, if (mission.kind == MissionKind.ACHIEVEMENT) "claim_achievement" else "claim_mission", missionId.value, EconomicDelta(spaceDollarsDelta = mission.rewardSpaceDollars)))
    }

    fun activeContracts(state: ProgressionState, snapshot: ProgressSnapshot): List<ContractView> = ContractTier.entries.map { tier ->
        val pool = definitions.contracts.filter { it.tier == tier }
        val cycle = state.contractCycles[tier] ?: 0
        val definition = pool[cycle % pool.size]
        ContractView("${definition.id.value}#$cycle", definition, snapshot.inventory[definition.resourceId] ?: 0L, state.claimedMissionIds.containsAll(definition.requiredMissionIds))
    }

    fun deliverContract(state: ProgressionState, occurrenceId: String, snapshot: ProgressSnapshot): ProgressionCommandResult {
        val active = activeContracts(state, snapshot).firstOrNull { it.occurrenceId == occurrenceId } ?: return ProgressionCommandResult.Rejected(state, "contract_not_active")
        if (!active.unlocked) return ProgressionCommandResult.Rejected(state, "contract_locked")
        if (!active.deliverable) return ProgressionCommandResult.Rejected(state, "contract_inventory_missing")
        val tier = active.definition.tier
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            contractCycles = state.contractCycles + (tier to Math.addExact(state.contractCycles.getValue(tier), 1)),
            completedContractCount = Math.addExact(state.completedContractCount, 1L),
            transactionSequence = sequence,
        )
        return ProgressionCommandResult.Applied(next, ProgressionTransaction(sequence, "deliver_contract", occurrenceId, EconomicDelta(active.definition.rewardSpaceDollars, mapOf(active.definition.resourceId to -active.definition.quantity))))
    }

    fun visibleCodexEntries(state: ProgressionState, snapshot: ProgressSnapshot): List<CodexEntryView> = definitions.codexEntries.values.asSequence()
        .filter { entry -> entry.id in state.discoveredCodexEntryIds || (state.claimedMissionIds.containsAll(entry.requiredMissionIds) && (entry.target == 0L || snapshot.metricValue(entry.metric) > 0L)) }
        .map { CodexEntryView(it, it.id in state.discoveredCodexEntryIds, snapshot.metricValue(it.metric).coerceAtMost(it.target)) }
        .sortedWith(compareByDescending<CodexEntryView> { it.discovered }.thenBy { it.definition.id.value })
        .toList()

    fun collectionViews(state: ProgressionState): List<CollectionView> = definitions.collections.values.map { collection ->
        CollectionView(collection, collection.entryIds.count(state.discoveredCodexEntryIds::contains), collection.id in state.claimedCollectionIds)
    }.filter { it.discoveredEntries > 0 || it.claimed }

    fun claimCollection(state: ProgressionState, collectionId: GameId): ProgressionCommandResult {
        val collection = definitions.collections[collectionId] ?: return ProgressionCommandResult.Rejected(state, "unknown_collection")
        if (collectionId in state.claimedCollectionIds) return ProgressionCommandResult.Rejected(state, "collection_already_claimed")
        if (!state.discoveredCodexEntryIds.containsAll(collection.entryIds)) return ProgressionCommandResult.Rejected(state, "collection_incomplete")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(claimedCollectionIds = state.claimedCollectionIds + collectionId, transactionSequence = sequence)
        return ProgressionCommandResult.Applied(next, ProgressionTransaction(sequence, "claim_collection", collectionId.value, EconomicDelta(spaceDollarsDelta = collection.rewardSpaceDollars)))
    }

    fun selectObjective(state: ProgressionState, missionId: GameId): ProgressionState = if (missionId in definitions.missions) state.copy(selectedObjectiveId = missionId) else state
}
