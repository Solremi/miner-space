package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator

internal class FerrumTradeActions(
    private val controller: ManufacturingCoordinator,
    private val feedbackMapper: ManufacturingFeedbackMapper,
) {
    fun sellAll(): FerrumActionFeedback = feedbackMapper.map(controller.sellAll())
}
