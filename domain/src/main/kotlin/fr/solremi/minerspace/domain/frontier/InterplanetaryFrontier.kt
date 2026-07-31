package fr.solremi.minerspace.domain.frontier

import fr.solremi.minerspace.shared.GameId

enum class FrontierVisualFamily { VOLCANIC, CRYSTALLINE, DERELICT }
enum class FrontierDifficulty(val estimatedDays: Int, val sectorCount: Int, val modifierCount: Int) {
    SCOUT(2, 5, 2), EXPEDITION(4, 6, 3), DEEP(7, 7, 3)
}
enum class FrontierCapability { EXTRACTION, REFINING, NETWORK, EVENT, ARTIFACT, CONSTRUCTION }
enum class FrontierRewardKind { PERMANENT_BONUS, COSMETIC, COLLECTION }
enum class FrontierWorldStatus { ACTIVE, COMPLETED }

data class FrontierSectorTemplate(
    val id: GameId,
    val family: FrontierVisualFamily,
    val name: String,
    val capabilities: Set<FrontierCapability>,
    val hazard: Int,
) {
    init {
        require(name.isNotBlank())
        require(capabilities.isNotEmpty())
        require(hazard in 1..10)
    }
}

data class FrontierModifierDefinition(
    val id: GameId,
    val name: String,
    val compatibleFamilies: Set<FrontierVisualFamily>,
    val incompatibleModifierIds: Set<GameId>,
    val requiredCapabilities: Set<FrontierCapability>,
    val progressMultiplierMillionths: Long,
    val rewardMultiplierMillionths: Long,
) {
    init {
        require(name.isNotBlank())
        require(compatibleFamilies.isNotEmpty())
        require(id !in incompatibleModifierIds)
        require(progressMultiplierMillionths in 500_000L..1_500_000L)
        require(rewardMultiplierMillionths in 750_000L..1_750_000L)
    }
}

data class FrontierObjectiveDefinition(
    val id: GameId,
    val name: String,
    val requiredCapability: FrontierCapability,
    val baseTarget: Long,
    val rewardKind: FrontierRewardKind,
) {
    init { require(name.isNotBlank() && baseTarget > 0L) }
}

data class FrontierDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val sectorTemplates: Map<GameId, FrontierSectorTemplate>,
    val modifiers: Map<GameId, FrontierModifierDefinition>,
    val objectives: Map<GameId, FrontierObjectiveDefinition>,
) {
    init {
        require(schemaVersion > 0 && contentVersion.isNotBlank())
        require(FrontierVisualFamily.entries.all { family -> sectorTemplates.values.count { it.family == family } == 8 })
        require(modifiers.size == 12)
        require(objectives.size == 6)
        require(objectives.values.map { it.requiredCapability }.toSet() == FrontierCapability.entries.toSet())
        modifiers.values.forEach { modifier ->
            require(modifier.incompatibleModifierIds.all(modifiers::containsKey))
        }
        FrontierVisualFamily.entries.forEach { family ->
            val capabilities = sectorTemplates.values.filter { it.family == family }.flatMapTo(linkedSetOf()) { it.capabilities }
            require(capabilities.containsAll(FrontierCapability.entries))
        }
    }
}

data class GeneratedFrontierSector(
    val id: GameId,
    val templateId: GameId,
    val requiredSectorId: GameId?,
)

data class FrontierWorldDefinition(
    val id: GameId,
    val seed: Long,
    val generationIndex: Int,
    val family: FrontierVisualFamily,
    val difficulty: FrontierDifficulty,
    val modifierIds: Set<GameId>,
    val objectiveId: GameId,
    val sectors: List<GeneratedFrontierSector>,
    val targetProgress: Long,
    val rewardKind: FrontierRewardKind,
    val rewardAmount: Long,
    val estimatedDays: Int,
) {
    init {
        require(generationIndex >= 0)
        require(modifierIds.size == difficulty.modifierCount)
        require(sectors.size == difficulty.sectorCount)
        require(targetProgress > 0L && rewardAmount > 0L)
        require(estimatedDays == difficulty.estimatedDays)
        require(sectors.map { it.id }.distinct().size == sectors.size)
    }

    val signature: String get() = "${family.name}|${modifierIds.map { it.value }.sorted().joinToString(",")}" 
}

data class FrontierWorldProgress(
    val definition: FrontierWorldDefinition,
    val progress: Long,
    val actionCount: Int,
    val status: FrontierWorldStatus,
    val startedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
) {
    init {
        require(progress >= 0L && actionCount >= 0)
        require(startedAtEpochMillis >= 0L && updatedAtEpochMillis >= startedAtEpochMillis)
        if (status == FrontierWorldStatus.COMPLETED) require(completedAtEpochMillis != null)
    }
}

data class FrontierState(
    val seed: Long,
    val nextGenerationIndex: Int,
    val worlds: Map<GameId, FrontierWorldProgress>,
    val activeWorldId: GameId?,
    val lastGeneratedSignature: String?,
    val permanentBonusIds: Set<GameId>,
    val cosmeticIds: Set<GameId>,
    val collectionIds: Set<GameId>,
    val completedWorldCount: Int,
    val transactionSequence: Long,
) {
    init {
        require(nextGenerationIndex >= 0 && completedWorldCount >= 0 && transactionSequence >= 0L)
        require(activeWorldId == null || worlds.containsKey(activeWorldId))
    }
}

data class FrontierTransaction(
    val sequence: Long,
    val reason: String,
    val worldId: GameId,
    val rewardId: GameId? = null,
)

sealed interface FrontierCommandResult {
    val state: FrontierState
    data class Applied(override val state: FrontierState, val transaction: FrontierTransaction) : FrontierCommandResult
    data class Rejected(override val state: FrontierState, val code: String) : FrontierCommandResult
}
