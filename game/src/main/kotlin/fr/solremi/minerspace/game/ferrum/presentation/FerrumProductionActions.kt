package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingActionResult
import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.shared.GameId

internal class FerrumProductionActions(
    private val controller: ManufacturingCoordinator,
    private val presenter: FerrumSelectionPresenter,
    private val text: FerrumTextCatalog,
    private val feedbackMapper: ManufacturingFeedbackMapper,
) {
    fun cycleBatch(state: FerrumScreenState): FerrumActionFeedback {
        state.batchMode = state.batchMode.next()
        return FerrumActionFeedback(text.batchSelected(state.batchMode.label), FerrumFeedbackKind.IMPACT)
    }

    fun launchSelected(state: FerrumScreenState): FerrumActionFeedback = when (state.selected) {
        FerrumNodeId.REFINER -> launchBatch(state, presenter.selectedRefiningRecipeId(state), refining = true)
        FerrumNodeId.ASSEMBLER -> launchBatch(state, presenter.selectedAssemblyRecipeId(state), refining = false)
        else -> FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
    }

    private fun launchBatch(
        state: FerrumScreenState,
        recipeId: GameId?,
        refining: Boolean,
    ): FerrumActionFeedback {
        recipeId ?: return FerrumActionFeedback(text.noRecipe, FerrumFeedbackKind.WARNING)
        var launched = 0
        while (launched < state.batchMode.limit) {
            val available = if (refining) controller.canLaunchRefining(recipeId) else controller.canLaunchAssembly(recipeId)
            if (!available) break
            val result = if (refining) controller.launchRefining(recipeId) else controller.launchAssembly(recipeId)
            if (result !is ManufacturingActionResult.Applied) return feedbackMapper.map(result)
            launched++
        }
        return if (launched > 0) {
            FerrumActionFeedback(text.batchLaunched(launched), FerrumFeedbackKind.SUCCESS)
        } else {
            FerrumActionFeedback(
                if (refining) presenter.refiningDetails(state) else presenter.assemblyDetails(state),
                FerrumFeedbackKind.WARNING,
            )
        }
    }
}
