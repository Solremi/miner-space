package fr.solremi.minerspace.domain.assembly

import fr.solremi.minerspace.domain.economy.MULTIPLIER_SCALE
import fr.solremi.minerspace.shared.GameId

enum class AssemblyOutputKind { COMPONENT, TECHNOLOGY }

data class AssemblyRecipeDefinition(
    val id: GameId,
    val nameKey: String,
    val inputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val durationSeconds: Long,
    val outputKind: AssemblyOutputKind,
    val requiredTechnologyIds: Set<GameId> = emptySet(),
) {
    init {
        require(nameKey.isNotBlank())
        require(inputs.isNotEmpty())
        require(inputs.values.all { it > 0L })
        require(outputQuantity > 0L)
        require(durationSeconds > 0L)
    }
}

data class TechnologyDefinition(
    val id: GameId,
    val nameKey: String,
    val itemResourceId: GameId,
    val requiredTechnologyIds: Set<GameId>,
    val extractionBonusMillionths: Long,
) {
    init {
        require(nameKey.isNotBlank())
        require(extractionBonusMillionths in 0L..MULTIPLIER_SCALE)
        require(id !in requiredTechnologyIds)
    }
}

data class AssemblerRobotDefinition(
    val id: GameId,
    val nameKey: String,
    val queueCapacity: Int,
) {
    init {
        require(nameKey.isNotBlank())
        require(queueCapacity > 0)
    }
}

data class AssemblyDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val robot: AssemblerRobotDefinition,
    val recipes: Map<GameId, AssemblyRecipeDefinition>,
    val technologies: Map<GameId, TechnologyDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(recipes.isNotEmpty())
        require(technologies.isNotEmpty())
        recipes.values.forEach { recipe ->
            recipe.requiredTechnologyIds.forEach { require(technologies.containsKey(it)) }
        }
        technologies.values.forEach { technology ->
            technology.requiredTechnologyIds.forEach { require(technologies.containsKey(it)) }
            require(recipes.values.any { it.outputResourceId == technology.itemResourceId }) {
                "Technology ${technology.id} has no assembly recipe."
            }
        }
    }
}
