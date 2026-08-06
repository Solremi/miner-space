package fr.solremi.minerspace.game.scene

import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.shared.GameId

enum class FerrumColonyStage(val label: String, val rank: Int) {
    OUTPOST("AVANT-POSTE", 0),
    INDUSTRIAL("COMPLEXE INDUSTRIEL", 1),
    NETWORKED("RÉSEAU AUTOMATISÉ", 2),
    ORBITAL("CHANTIER ORBITAL", 3),
    ;

    fun includes(other: FerrumColonyStage): Boolean = rank >= other.rank
}

data class FerrumColonyDevelopment(
    val stage: FerrumColonyStage,
    val refinedMaterials: Long,
    val components: Long,
    val technologies: Int,
) {
    val summary: String
        get() = "${stage.label} · $refinedMaterials raffinés · $components composants · $technologies technologies"

    companion object {
        fun from(state: ManufacturingGameState): FerrumColonyDevelopment {
            val refined = state.stock(REFINED_IRON) + state.stock(REFINED_COPPER)
            val components = state.stock(POWER_CELL) + state.stock(SENSOR_ARRAY)
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

        private val REFINED_IRON = GameId.of("refined_iron_ingot")
        private val REFINED_COPPER = GameId.of("refined_copper_plate")
        private val POWER_CELL = GameId.of("component_power_cell")
        private val SENSOR_ARRAY = GameId.of("component_sensor_array")
    }
}
