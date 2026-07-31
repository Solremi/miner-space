package fr.solremi.minerspace.domain.cryos

import fr.solremi.minerspace.shared.GameId

internal fun CryosIxEngine.progress(source: CryosIxState): CryosIxState {
    var main = source.completedMainMissionIds
    val conditions = listOf(
        source.baseInstalled,
        source.energy >= 200L,
        source.heat >= 220L,
        definitions.resources.keys.any { (source.inventory[it] ?: 0L) > 0L },
        definitions.refinedMaterialIds.any { (source.inventory[it] ?: 0L) > 0L },
        source.thermalNodes >= 1,
        source.unlockedSectorIds.size >= 2,
        source.installedTechnologyIds.isNotEmpty(),
        source.craftedModuleIds.isNotEmpty(),
        source.unlockedSectorIds.size >= 4,
        source.thermalNodes >= 4,
        source.frontierUnlocked,
    )
    conditions.forEachIndexed { index, complete -> if (complete) main = main + definitions.mainMissionIds[index] }
    var secondary = source.completedSecondaryMissionIds
    val secondaryConditions = listOf(
        source.coldExposure >= 50L,
        source.energy >= 350L,
        source.heat >= 350L,
        source.installedTechnologyIds.size >= 2,
        source.craftedModuleIds.size >= 2,
        source.resolvedEventIds.isNotEmpty(),
        source.unlockedSectorIds.size >= 5,
        source.thermalNodes >= 5,
        source.narrativeDiscoveryIds.isNotEmpty(),
        source.frontierUnlocked,
    )
    secondaryConditions.forEachIndexed { index, complete -> if (complete) secondary = secondary + definitions.secondaryMissionIds[index] }
    return source.copy(completedMainMissionIds = main, completedSecondaryMissionIds = secondary)
}

internal fun CryosIxEngine.applied(state: CryosIxState, reason: String, referenceId: GameId?): CryosCommandResult.Applied {
    val sequence = Math.addExact(state.transactionSequence, 1L)
    return CryosCommandResult.Applied(
        state.copy(transactionSequence = sequence),
        CryosTransaction(sequence, reason, referenceId),
    )
}

internal fun CryosIxEngine.reject(state: CryosIxState, code: String) = CryosCommandResult.Rejected(state, code)

internal fun CryosIxEngine.discover(current: Set<GameId>, sourceId: GameId): Set<GameId> {
    val exact = GameId.of("codex_${sourceId.value}")
    return if (exact in definitions.codexEntryIds) current + exact else current
}

internal fun CryosIxEngine.componentIds(): Set<GameId> = definitions.recipes.values
    .mapNotNullTo(linkedSetOf()) { it.outputId.takeIf { id -> id.value.startsWith("component_") } }

internal fun CryosIxEngine.consumeRefined(source: Map<GameId, Long>, quantity: Long): Map<GameId, Long> {
    var remaining = quantity
    val inventory = source.toMutableMap()
    definitions.refinedMaterialIds.sortedBy { it.value }.forEach { id ->
        if (remaining <= 0L) return@forEach
        val available = inventory[id] ?: 0L
        val used = minOf(available, remaining)
        inventory[id] = available - used
        remaining -= used
    }
    require(remaining == 0L)
    return inventory
}

