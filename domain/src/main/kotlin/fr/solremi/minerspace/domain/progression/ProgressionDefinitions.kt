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
        val visiting = mutableSetOf<GameId>()
        val visited = mutableSetOf<GameId>()
        fun visit(id: GameId): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            val valid = missions.getValue(id).requiredMissionIds.all(::visit)
            visiting.remove(id)
            visited += id
            return valid
        }
        return missions.keys.all(::visit)
    }
}
