package fr.solremi.minerspace.domain.strategy

import fr.solremi.minerspace.shared.GameId

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

    fun slotCount(robotLevel: Int): Int =
        (1 + (robotLevel.coerceAtLeast(1) - 1) / 2).coerceAtMost(3)

    fun chooseSpecialization(
        state: StrategyState,
        id: SpecializationId,
        access: StrategyAccess,
    ): StrategyCommandResult {
        if (state.activeSpecialization == id) {
            return StrategyCommandResult.Rejected(state, "specialization_already_active")
        }
        val definition = definitions.specializations.getValue(id)
        val firstChoice = state.activeSpecialization == null
        val freeTrial = firstChoice && !state.trialUsed
        val cooldownEnds = state.specializationChangedAtEpochMillis + definition.cooldownSeconds * 1_000L
        if (!firstChoice && access.nowEpochMillis < cooldownEnds) {
            return StrategyCommandResult.Rejected(state, "specialization_cooldown")
        }
        val cost = if (freeTrial) 0L else definition.changeCostSpaceDollars
        if (access.spaceDollars < cost) {
            return StrategyCommandResult.Rejected(state, "insufficient_space_dollars")
        }
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

    fun craft(
        state: StrategyState,
        definitionId: GameId,
        access: StrategyAccess,
    ): StrategyCommandResult {
        val definition = definitions.modules[definitionId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_module")
        if (access.spaceDollars < definition.craftCostSpaceDollars) {
            return StrategyCommandResult.Rejected(state, "insufficient_space_dollars")
        }
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
            StrategyTransaction(
                sequence,
                "craft_module",
                -definition.craftCostSpaceDollars,
                deltas,
                instanceId,
            ),
        )
    }

    fun equip(
        state: StrategyState,
        instanceId: String,
        robotId: GameId,
        access: StrategyAccess,
    ): StrategyCommandResult {
        val module = state.modules[instanceId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        val robotLevel = access.robotLevels[robotId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_robot")
        val slots = slotCount(robotLevel)
        val equipped = state.modules.values.count {
            it.equippedRobotId == robotId && it.instanceId != instanceId
        }
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
        val module = state.modules[instanceId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        if (module.equippedRobotId == null) {
            return StrategyCommandResult.Rejected(state, "module_not_equipped")
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(
                modules = state.modules + (instanceId to module.copy(equippedRobotId = null)),
                transactionSequence = sequence,
            ),
            StrategyTransaction(sequence, "unequip_module", moduleInstanceId = instanceId),
        )
    }

    fun upgrade(
        state: StrategyState,
        instanceId: String,
        access: StrategyAccess,
    ): StrategyCommandResult {
        val module = state.modules[instanceId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        val definition = definitions.modules.getValue(module.definitionId)
        if (module.level >= definition.maxLevel) {
            return StrategyCommandResult.Rejected(state, "module_max_level")
        }
        val cost = definition.upgradeCostsSpaceDollars[module.level]
        if (access.spaceDollars < cost) {
            return StrategyCommandResult.Rejected(state, "insufficient_space_dollars")
        }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(
                modules = state.modules + (instanceId to module.copy(level = module.level + 1)),
                transactionSequence = sequence,
            ),
            StrategyTransaction(sequence, "upgrade_module", -cost, moduleInstanceId = instanceId),
        )
    }

    fun dismantle(state: StrategyState, instanceId: String): StrategyCommandResult {
        val module = state.modules[instanceId]
            ?: return StrategyCommandResult.Rejected(state, "unknown_module_instance")
        val definition = definitions.modules.getValue(module.definitionId)
        val refund = definition.craftInputs
            .mapValues { (_, quantity) -> (quantity * 70L) / 100L }
            .filterValues { it > 0L }
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return StrategyCommandResult.Applied(
            state.copy(modules = state.modules - instanceId, transactionSequence = sequence),
            StrategyTransaction(
                sequence,
                "dismantle_module",
                inventoryDeltas = refund,
                moduleInstanceId = instanceId,
            ),
        )
    }

    fun bonuses(state: StrategyState, robotId: GameId? = null): StrategyBonuses {
        var total = state.activeSpecialization
            ?.let { definitions.specializations.getValue(it).bonuses }
            ?: StrategyBonuses()
        val equipped = state.modules.values
            .filter { robotId == null || it.equippedRobotId == robotId }
            .filter { it.equippedRobotId != null }
        equipped.forEach { owned ->
            val definition = definitions.modules.getValue(owned.definitionId)
            total += definition.baseBonuses.scaled(owned.level)
        }
        val sets = equipped
            .groupingBy { definitions.modules.getValue(it.definitionId).setId }
            .eachCount()
        definitions.synergies.forEach { synergy ->
            if ((sets[synergy.setId] ?: 0) >= synergy.requiredPieces) total += synergy.bonuses
        }
        return total
    }

    fun compare(
        state: StrategyState,
        preview: SpecializationId? = state.activeSpecialization,
    ): StrategyComparison {
        val candidate = state.copy(activeSpecialization = preview)
        val bonuses = bonuses(candidate)
        return StrategyComparison(
            bonuses.multiplier(ModuleStat.EXTRACTION),
            bonuses.multiplier(ModuleStat.REFINING_SPEED),
            bonuses.multiplier(ModuleStat.ASSEMBLY_SPEED),
            bonuses.multiplier(ModuleStat.LOGISTICS),
        )
    }

    fun dominantCategoryCount(id: SpecializationId): Int {
        val all = SpecializationId.entries.associateWith {
            definitions.specializations.getValue(it).bonuses
        }
        val bonuses = all.getValue(id)
        val values = listOf(
            bonuses.extraction,
            bonuses.refiningSpeed,
            bonuses.assemblySpeed,
            bonuses.logistics,
        )
        return values.indices.count { index ->
            values[index] == all.values.maxOf { candidate ->
                listOf(
                    candidate.extraction,
                    candidate.refiningSpeed,
                    candidate.assemblySpeed,
                    candidate.logistics,
                )[index]
            }
        }
    }
}
