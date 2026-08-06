package fr.solremi.minerspace.domain.refining

import fr.solremi.minerspace.shared.GameId

const val BASIS_POINTS_SCALE: Long = 10_000L

data class RecipeDefinition(
    val id: GameId,
    val nameKey: String,
    val inputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val durationSeconds: Long,
) {
    init {
        require(nameKey.isNotBlank())
        require(inputs.isNotEmpty())
        require(inputs.values.all { it > 0L })
        require(outputQuantity > 0L)
        require(durationSeconds > 0L)
    }
}

data class RefinerRobotDefinition(
    val id: GameId,
    val nameKey: String,
    val queueCapacity: Int,
) {
    init {
        require(nameKey.isNotBlank())
        require(queueCapacity > 0)
    }
}

data class RefiningDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val robot: RefinerRobotDefinition,
    val recipes: Map<GameId, RecipeDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(recipes.isNotEmpty())
    }
}
