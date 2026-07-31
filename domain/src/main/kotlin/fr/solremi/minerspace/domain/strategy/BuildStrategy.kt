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

private fun StrategyBonuses.visibleStatCount(): Int = listOf(
    extraction, refiningSpeed, assemblySpeed, logistics, storage, rareFind,
).count { it != 0L }

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

data class OwnedModule(
    val instanceId: String,
    val definitionId: GameId,
    val level: Int,
    val equippedRobotId: GameId?,
) {
    init {
        require(instanceId.isNotBlank())
        require(level >= 1)
    }
}

data class StrategyState(
    val activeSpecialization: SpecializationId?,
    val trialUsed: Boolean,
    val specializationChangedAtEpochMillis: Long,
    val modules: Map<String, OwnedModule>,
    val nextModuleSequence: Long,
    val transactionSequence: Long,
) {
    init {
        require(specializationChangedAtEpochMillis >= 0L)
        require(nextModuleSequence >= 1L)
        require(transactionSequence >= 0L)
        require(modules.keys == modules.values.map { it.instanceId }.toSet())
    }

    companion object {
        fun empty() = StrategyState(null, false, 0L, emptyMap(), 1L, 0L)
    }
}

data class StrategyAccess(
    val nowEpochMillis: Long,
    val spaceDollars: Long,
    val inventory: Map<GameId, Long>,
    val robotLevels: Map<GameId, Int>,
) {
    init {
        require(nowEpochMillis >= 0L)
        require(spaceDollars >= 0L)
        require(inventory.values.all { it >= 0L })
    }
}

data class StrategyTransaction(
    val sequence: Long,
    val reason: String,
    val spaceDollarDelta: Long = 0L,
    val inventoryDeltas: Map<GameId, Long> = emptyMap(),
    val moduleInstanceId: String? = null,
)

sealed interface StrategyCommandResult {
    val state: StrategyState
    data class Applied(override val state: StrategyState, val transaction: StrategyTransaction) : StrategyCommandResult
    data class Rejected(override val state: StrategyState, val code: String) : StrategyCommandResult
}

data class StrategyComparison(
    val extractionMillionths: Long,
    val refiningSpeedMillionths: Long,
    val assemblySpeedMillionths: Long,
    val logisticsMillionths: Long,
)

class StrategyEngine(val definitions: StrategyDefinitions) {
    fun normalize(source: StrategyState): StrategyState {
        val validModules = source.modules.values.asSequence()
            .filter { definitions.modules.containsKey(it.definitionId) }
            .distinctBy { it.instanceId }
            .associateBy { it.instanceId }
        return source.copy(
            modules = validModules,
            nextModuleSequence = source.nextModuleSequence.coerceAtLeast(1L),
            transactionSequence = source.transactionSequence.coerceAtLeast(0L),
            specializationChangedAtEpochMillis = source.specializationChangedAtEpochMillis.coerceAtLeast(0L),
        )
    }

    fun slotCount(robotLevel: Int): Int = (1 + (robotLevel.coerceAtLeast(1) - 1) / 2).coerceAtMost(3)

    fun chooseSpecialization(
        state: StrategyState,
        id: SpecializationId,
        access: StrategyAccess,
    ): StrategyCommandResult {
        if (state.activeSpecialization == id) return StrategyCommandResult.Rejected(state, "specialization_already_active")
        val definition = definitions.specializations.getValue(id)
        val firstChoice = state.activeSpecialization == null
        val freeTrial = firstChoice && !state.trialUsed
        val cooldownEnds = state.specializationChangedAtEpochMillis + definition.cooldownSeconds * 1_000L
        if (!firstChoice && access.nowEpochMillis < cooldownEnds) return StrategyCommandResult.Rejected(state, "specialization_cooldown")
        val cost = if (freeTrial) 0L else definition.changeCostSpaceDollars
        if (access.spaceDollars < cost) return StrategyCommandResult.Rejected(state, "insufficient_space_dollars")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(
                activeSpecialization = id,
                trialUsed = true,
                specializationChangedAtEpochMillis = access.nowEpochMillis,
                transactionSequence = sequence,
            ),
            StrategyTransaction(sequence, "change_specialization", -cost),
        )
    }

    fun craft(state: StrategyState, definitionId: GameId, access: StrategyAccess): StrategyCommandResult {
        val definition = definitions.modules[definitionId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_module")
        if (access.spaceDollars < definition.craftCostSpaceDollars) return StrategyCommandResult.Rejected(state, "insufficient_space_dollars")
        if (!definition.craftInputs.all { (id, quantity) -> (access.inventory[id] ?: 0L) >= quantity }) {
            return StrategyCommandResult.Rejected(state, "missing_module_materials")
        }
        val instanceId = "module-%06d".format(state.nextModuleSequence)
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val deltas = definition.craftInputs.mapValues { -it.value }
        return StrategyCommandResult.Applied(
            state.copy(
                modules = state.modules + (instanceId to OwnedModule(instanceId, definition.id, 1, null)),
                nextModuleSequence = Math.addExact(state.nextModuleSequence, 1L),
                transactionSequence = sequence,
            ),
            StrategyTransaction(sequence, "craft_module", -definition.craftCostSpaceDollars, deltas, instanceId),
        )
    }

    fun equip(
        state: StrategyState,
        instanceId: String,
        robotId: GameId,
        access: StrategyAccess,
    ): StrategyCommandResult {
        val module = state.modules[instanceId] ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        val slots = slotCount(access.robotLevels[robotId] ?: return StrategyCommandResult.Rejected(state, "unknown_robot"))
        val equipped = state.modules.values.count { it.equippedRobotId == robotId && it.instanceId != instanceId }
        if (equipped >= slots) return StrategyCommandResult.Rejected(state, "module_slots_full")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(
                modules = state.modules + (instanceId to module.copy(equippedRobotId = robotId)),
                transactionSequence = sequence,
            ),
            StrategyTransaction(sequence, "equip_module", moduleInstanceId = instanceId),
        )
    }

    fun unequip(state: StrategyState, instanceId: String): StrategyCommandResult {
        val module = state.modules[instanceId] ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        if (module.equippedRobotId == null) return StrategyCommandResult.Rejected(state, "module_not_equipped")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(modules = state.modules + (instanceId to module.copy(equippedRobotId = null)), transactionSequence = sequence),
            StrategyTransaction(sequence, "unequip_module", moduleInstanceId = instanceId),
        )
    }

    fun upgrade(state: StrategyState, instanceId: String, access: StrategyAccess): StrategyCommandResult {
        val module = state.modules[instanceId] ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        val definition = definitions.modules.getValue(module.definitionId)
        if (module.level >= definition.maxLevel) return StrategyCommandResult.Rejected(state, "module_max_level")
        val cost = definition.upgradeCostsSpaceDollars[module.level]
        if (access.spaceDollars < cost) return StrategyCommandResult.Rejected(state, "insufficient_space_dollars")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(modules = state.modules + (instanceId to module.copy(level = module.level + 1)), transactionSequence = sequence),
            StrategyTransaction(sequence, "upgrade_module", -cost, moduleInstanceId = instanceId),
        )
    }

    fun dismantle(state: StrategyState, instanceId: String): StrategyCommandResult {
        val module = state.modules[instanceId] ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        val definition = definitions.modules.getValue(module.definitionId)
        val refund = definition.craftInputs.mapValues { (_, quantity) -> (quantity * 70L) / 100L }.filterValues { it > 0L }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(modules = state.modules - instanceId, transactionSequence = sequence),
            StrategyTransaction(sequence, "dismantle_module", inventoryDeltas = refund, moduleInstanceId = instanceId),
        )
    }

    fun bonuses(state: StrategyState, robotId: GameId? = null): StrategyBonuses {
        var total = state.activeSpecialization?.let { definitions.specializations.getValue(it).bonuses } ?: StrategyBonuses()
        val equipped = state.modules.values.filter { robotId == null || it.equippedRobotId == robotId }
            .filter { it.equippedRobotId != null }
        equipped.forEach { owned ->
            val definition = definitions.modules.getValue(owned.definitionId)
            total += definition.baseBonuses.scaled(owned.level)
        }
        val sets = equipped.groupingBy { definitions.modules.getValue(it.definitionId).setId }.eachCount()
        definitions.synergies.forEach { synergy ->
            if ((sets[synergy.setId] ?: 0) >= synergy.requiredPieces) total += synergy.bonuses
        }
        return total
    }

    fun compare(state: StrategyState, preview: SpecializationId? = state.activeSpecialization): StrategyComparison {
        val candidate = state.copy(activeSpecialization = preview)
        val b = bonuses(candidate)
        return StrategyComparison(
            b.multiplier(ModuleStat.EXTRACTION),
            b.multiplier(ModuleStat.REFINING_SPEED),
            b.multiplier(ModuleStat.ASSEMBLY_SPEED),
            b.multiplier(ModuleStat.LOGISTICS),
        )
    }

    fun dominantCategoryCount(id: SpecializationId): Int {
        val all = SpecializationId.entries.associateWith { definitions.specializations.getValue(it).bonuses }
        val b = all.getValue(id)
        val values = listOf(b.extraction, b.refiningSpeed, b.assemblySpeed, b.logistics)
        return values.indices.count { index -> values[index] == all.values.maxOf { candidate -> listOf(candidate.extraction, candidate.refiningSpeed, candidate.assemblySpeed, candidate.logistics)[index] } }
    }

    private fun StrategyBonuses.scaled(level: Int) = StrategyBonuses(
        extraction * level,
        refiningSpeed * level,
        assemblySpeed * level,
        logistics * level,
        storage * level,
        rareFind * level,
    )
}
