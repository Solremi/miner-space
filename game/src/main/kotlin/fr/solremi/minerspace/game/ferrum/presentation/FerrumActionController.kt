package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.game.ferrum.model.FerrumIds
import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog

class FerrumActionController(
    controller: ManufacturingCoordinator,
    clock: ClockService,
    private val text: FerrumTextCatalog,
) {
    private val feedbackMapper = ManufacturingFeedbackMapper()
    private val presenter = FerrumSelectionPresenter(controller, clock, text)
    private val production = FerrumProductionActions(controller, presenter, text, feedbackMapper)
    private val collection = FerrumCollectionActions(controller, presenter, text, feedbackMapper)
    private val trade = FerrumTradeActions(controller, feedbackMapper)

    fun recipeAvailable(state: FerrumScreenState): Boolean = presenter.recipeAvailable(state)
    fun actionAvailable(state: FerrumScreenState): Boolean = presenter.actionAvailable(state)
    fun taskAvailable(state: FerrumScreenState): Boolean = presenter.taskAvailable(state)
    fun recipeLabel(state: FerrumScreenState): String = presenter.recipeLabel(state)
    fun actionLabel(state: FerrumScreenState): String = presenter.actionLabel(state)
    fun taskLabel(state: FerrumScreenState): String = presenter.taskLabel(state)
    fun utilityLabel(state: FerrumScreenState): String = presenter.utilityLabel(state)
    fun selectionTitle(state: FerrumScreenState): String = presenter.selectionTitle(state)
    fun economyLine(): String = presenter.economyLine()
    fun cycleRecipe(state: FerrumScreenState): FerrumActionFeedback = presenter.cycleRecipe(state)
    fun cycleBatch(state: FerrumScreenState): FerrumActionFeedback = production.cycleBatch(state)
    fun refiningDetails(state: FerrumScreenState): String = presenter.refiningDetails(state)
    fun assemblyDetails(state: FerrumScreenState): String = presenter.assemblyDetails(state)

    fun performAction(state: FerrumScreenState): FerrumActionFeedback = when (state.selected) {
        FerrumNodeId.REFINER, FerrumNodeId.ASSEMBLER -> production.launchSelected(state)
        FerrumNodeId.BASE -> trade.sellAll()
        FerrumNodeId.IRON_DEPOSIT -> collection.collectDeposit(FerrumIds.DEPOSIT_IRON)
        FerrumNodeId.COPPER_DEPOSIT -> collection.collectDeposit(FerrumIds.DEPOSIT_COPPER)
        FerrumNodeId.CRYSTAL_DEPOSIT -> collection.collectDeposit(FerrumIds.DEPOSIT_CRYSTAL)
        null -> FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
    }

    fun performTask(state: FerrumScreenState): FerrumActionFeedback = collection.performTask(state)
}
