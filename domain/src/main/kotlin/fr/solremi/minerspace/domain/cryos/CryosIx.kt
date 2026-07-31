package fr.solremi.minerspace.domain.cryos

import fr.solremi.minerspace.shared.GameId

data class CryosResourceDefinition(
    val id: GameId,
    val name: String,
    val sourceSectorId: GameId,
    val mandatory: Boolean,
) {
    init { require(name.isNotBlank()) }
}

data class CryosSectorDefinition(
    val id: GameId,
    val name: String,
    val requiredSectorIds: Set<GameId>,
    val requiredThermalNodes: Int,
    val minimumHeat: Long,
    val coldDrainPerAction: Long,
) {
    init {
        require(name.isNotBlank())
        require(requiredThermalNodes >= 0 && minimumHeat >= 0L && coldDrainPerAction > 0L)
        require(id !in requiredSectorIds)
    }
}

data class CryosRecipeDefinition(
    val id: GameId,
    val inputs: Map<GameId, Long>,
    val outputId: GameId,
    val outputQuantity: Long,
    val energyCost: Long,
    val heatCost: Long,
) {
    init {
        require(inputs.isNotEmpty() && inputs.values.all { it > 0L })
        require(outputQuantity > 0L && energyCost >= 0L && heatCost >= 0L)
    }
}

data class CryosTechnologyDefinition(
    val id: GameId,
    val requiredTechnologyIds: Set<GameId>,
    val energyCost: Long,
    val heatCost: Long,
    val refinedMaterialCost: Long,
) {
    init {
        require(id !in requiredTechnologyIds)
        require(energyCost > 0L && heatCost >= 0L && refinedMaterialCost > 0L)
    }
}

data class CryosModuleDefinition(
    val id: GameId,
    val setId: GameId,
    val requiredTechnologyIds: Set<GameId>,
    val refinedMaterialCost: Long,
    val energyCost: Long,
) {
    init { require(refinedMaterialCost > 0L && energyCost > 0L) }
}

data class CryosIxDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val sectors: Map<GameId, CryosSectorDefinition>,
    val resources: Map<GameId, CryosResourceDefinition>,
    val refinedMaterialIds: Set<GameId>,
    val recipes: Map<GameId, CryosRecipeDefinition>,
    val technologies: Map<GameId, CryosTechnologyDefinition>,
    val modules: Map<GameId, CryosModuleDefinition>,
    val mainMissionIds: List<GameId>,
    val secondaryMissionIds: List<GameId>,
    val eventIds: List<GameId>,
    val narrativeDiscoveryIds: List<GameId>,
    val codexEntryIds: List<GameId>,
    val thermalSetId: GameId,
) {
    init {
        require(schemaVersion > 0 && contentVersion.isNotBlank())
        require(sectors.size == 6)
        require(resources.size == 4)
        require(refinedMaterialIds.size == 4)
        require(recipes.size == 8)
        require(technologies.size == 5)
        require(modules.size == 8)
        require(mainMissionIds.size == 12)
        require(secondaryMissionIds.size == 10)
        require(eventIds.size == 3)
        require(narrativeDiscoveryIds.size == 2)
        require(codexEntryIds.size in 25..40)
        require(sectors.values.count { it.requiredSectorIds.isEmpty() } == 1)
        require(resources.values.all { sectors.containsKey(it.sourceSectorId) })
        require(resources.values.filter { it.mandatory }.all { it.sourceSectorId in sectors })
        require(recipes.values.all { recipe -> recipe.inputs.keys.all { it in resources || it in refinedMaterialIds } })
        require(recipes.values.all { it.outputId in refinedMaterialIds || it.outputId.value.startsWith("component_") })
        require(technologies.values.all { tech -> tech.requiredTechnologyIds.all(technologies::containsKey) })
        require(modules.values.all { module -> module.requiredTechnologyIds.all(technologies::containsKey) })
        require(modules.values.all { it.setId == thermalSetId })
        require(noSectorCycle() && noTechnologyCycle())
    }

    private fun noSectorCycle(): Boolean = acyclic(sectors.mapValues { it.value.requiredSectorIds })
    private fun noTechnologyCycle(): Boolean = acyclic(technologies.mapValues { it.value.requiredTechnologyIds })

    private fun acyclic(graph: Map<GameId, Set<GameId>>): Boolean {
        val visiting = mutableSetOf<GameId>()
        val visited = mutableSetOf<GameId>()
        fun visit(id: GameId): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            val valid = graph.getValue(id).all { graph.containsKey(it) && visit(it) }
            visiting.remove(id)
            visited += id
            return valid
        }
        return graph.keys.all(::visit)
    }
}

data class CryosIxState(
    val baseInstalled: Boolean,
    val energy: Long,
    val heat: Long,
    val coldExposure: Long,
    val thermalNodes: Int,
    val inventory: Map<GameId, Long>,
    val installedTechnologyIds: Set<GameId>,
    val craftedModuleIds: Set<GameId>,
    val unlockedSectorIds: Set<GameId>,
    val completedMainMissionIds: Set<GameId>,
    val completedSecondaryMissionIds: Set<GameId>,
    val resolvedEventIds: Set<GameId>,
    val narrativeDiscoveryIds: Set<GameId>,
    val discoveredCodexEntryIds: Set<GameId>,
    val frontierUnlocked: Boolean,
    val veteranRobotId: GameId?,
    val transactionSequence: Long,
) {
    init {
        require(energy >= 0L && heat >= 0L && coldExposure >= 0L && thermalNodes >= 0)
        require(inventory.values.none { it < 0L })
        require(transactionSequence >= 0L)
    }
}

data class CryosTransaction(
    val sequence: Long,
    val reason: String,
    val referenceId: GameId?,
)

sealed interface CryosCommandResult {
    val state: CryosIxState

    data class Applied(
        override val state: CryosIxState,
        val transaction: CryosTransaction,
    ) : CryosCommandResult

    data class Rejected(
        override val state: CryosIxState,
        val code: String,
    ) : CryosCommandResult
}

