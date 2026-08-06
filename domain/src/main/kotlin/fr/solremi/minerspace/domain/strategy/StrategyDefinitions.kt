package fr.solremi.minerspace.domain.strategy

import fr.solremi.minerspace.domain.economy.MULTIPLIER_SCALE
import fr.solremi.minerspace.shared.GameId

enum class SpecializationId { INDUSTRIAL, LOGISTICS, RESEARCH, PROSPECTOR }
enum class ModuleSetId { FORGE, SURVEY, NONE }
enum class ModuleStat { EXTRACTION, REFINING_SPEED, ASSEMBLY_SPEED, LOGISTICS, STORAGE, RARE_FIND }

data class StrategyBonuses(
    val extraction: Long = 0L,
    val refiningSpeed: Long = 0L,
    val assemblySpeed: Long = 0L,
    val logistics: Long = 0L,
    val storage: Long = 0L,
    val rareFind: Long = 0L,
) {
    init {
        listOf(extraction, refiningSpeed, assemblySpeed, logistics, storage, rareFind).forEach {
            require(it in -500_000L..1_000_000L)
        }
    }

    operator fun plus(other: StrategyBonuses) = StrategyBonuses(
        extraction + other.extraction,
        refiningSpeed + other.refiningSpeed,
        assemblySpeed + other.assemblySpeed,
        logistics + other.logistics,
        storage + other.storage,
        rareFind + other.rareFind,
    )

    fun multiplier(stat: ModuleStat): Long = MULTIPLIER_SCALE + when (stat) {
        ModuleStat.EXTRACTION -> extraction
        ModuleStat.REFINING_SPEED -> refiningSpeed
        ModuleStat.ASSEMBLY_SPEED -> assemblySpeed
        ModuleStat.LOGISTICS -> logistics
        ModuleStat.STORAGE -> storage
        ModuleStat.RARE_FIND -> rareFind
    }

    internal fun scaled(level: Int) = StrategyBonuses(
        extraction * level,
        refiningSpeed * level,
        assemblySpeed * level,
        logistics * level,
        storage * level,
        rareFind * level,
    )

    internal fun visibleStatCount(): Int = listOf(
        extraction,
        refiningSpeed,
        assemblySpeed,
        logistics,
        storage,
        rareFind,
    ).count { it != 0L }
}

data class SpecializationDefinition(
    val id: SpecializationId,
    val nameKey: String,
    val bonuses: StrategyBonuses,
    val changeCostSpaceDollars: Long,
    val cooldownSeconds: Long,
) {
    init {
        require(nameKey.isNotBlank())
        require(changeCostSpaceDollars >= 0L)
        require(cooldownSeconds >= 0L)
    }
}

data class ModuleDefinition(
    val id: GameId,
    val nameKey: String,
    val setId: ModuleSetId,
    val baseBonuses: StrategyBonuses,
    val craftInputs: Map<GameId, Long>,
    val craftCostSpaceDollars: Long,
    val upgradeCostsSpaceDollars: List<Long>,
    val maxLevel: Int,
) {
    init {
        require(nameKey.isNotBlank())
        require(craftInputs.isNotEmpty())
        require(craftInputs.values.all { it > 0L })
        require(craftCostSpaceDollars >= 0L)
        require(maxLevel in 1..5)
        require(upgradeCostsSpaceDollars.size == maxLevel)
        require(upgradeCostsSpaceDollars.first() == 0L)
        require(baseBonuses.visibleStatCount() in 1..4)
    }
}

data class SynergyDefinition(
    val setId: ModuleSetId,
    val requiredPieces: Int,
    val bonuses: StrategyBonuses,
) {
    init {
        require(setId != ModuleSetId.NONE)
        require(requiredPieces in 2..3)
    }
}

data class StrategyDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val specializations: Map<SpecializationId, SpecializationDefinition>,
    val modules: Map<GameId, ModuleDefinition>,
    val synergies: List<SynergyDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(specializations.keys == SpecializationId.entries.toSet())
        require(modules.size >= 6)
        require(synergies.groupBy { it.setId }.keys.containsAll(setOf(ModuleSetId.FORGE, ModuleSetId.SURVEY)))
    }
}
