package fr.solremi.minerspace.game.ferrum.model

import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.shared.GameId

enum class FerrumColonyStage(val rank: Int) {
    OUTPOST(0),
    INDUSTRIAL(1),
    NETWORKED(2),
    ORBITAL(3),
    ;

    fun includes(other: FerrumColonyStage): Boolean = rank >= other.rank
}

data class FerrumColonyDevelopment(
    val stage: FerrumColonyStage,
    val refinedMaterials: Long,
    val components: Long,
    val technologies: Int,
) {
    companion object {
        fun from(state: ManufacturingGameState): FerrumColonyDevelopment {
            val refined = state.stock(FerrumIds.REFINED_IRON) + state.stock(FerrumIds.REFINED_COPPER)
            val components = state.stock(FerrumIds.POWER_CELL) + state.stock(FerrumIds.SENSOR_ARRAY)
            val technologies = state.assembly.installedTechnologyIds.size
            val stage = when {
                technologies >= 2 -> FerrumColonyStage.ORBITAL
                technologies >= 1 || components >= 3L -> FerrumColonyStage.NETWORKED
                components >= 1L || refined >= 5L -> FerrumColonyStage.INDUSTRIAL
                else -> FerrumColonyStage.OUTPOST
            }
            return FerrumColonyDevelopment(stage, refined, components, technologies)
        }

        private fun ManufacturingGameState.stock(id: GameId): Long = economy.inventory[id] ?: 0L
    }
}
