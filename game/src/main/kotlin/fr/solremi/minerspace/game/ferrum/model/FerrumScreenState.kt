package fr.solremi.minerspace.game.ferrum.model

import fr.solremi.minerspace.game.ferrum.presentation.FerrumProductionAdvice
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId

enum class FerrumBatchMode(val label: String, val limit: Int) {
    ONE("X1", 1),
    FIVE("X5", 5),
    MAX("MAX", 32),
    ;

    fun next(): FerrumBatchMode = entries[(ordinal + 1) % entries.size]
}

data class FerrumScreenState(
    var selected: FerrumNodeId? = FerrumNodeId.BASE,
    var refiningRecipeIndex: Int = 0,
    var assemblyRecipeIndex: Int = 0,
    var batchMode: FerrumBatchMode = FerrumBatchMode.ONE,
    var menuOpen: Boolean = false,
    var message: String,
    var advice: FerrumProductionAdvice,
    var development: FerrumColonyDevelopment,
    var lastAnnouncedStage: FerrumColonyStage = development.stage,
)
