package fr.solremi.minerspace.game.ferrum.ui

import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.presentation.FerrumActionController
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.game.ui.FerrumPrimaryDestination
import fr.solremi.minerspace.game.ui.FerrumSecondaryDestination

class FerrumHudPresenter(
    private val actions: FerrumActionController,
    private val text: FerrumTextCatalog,
) {
    fun present(state: FerrumScreenState, secondsSinceSave: Long?): FerrumHudModel = FerrumHudModel(
        title = text.title,
        subtitle = "${actions.selectionTitle(state)} · ${actions.economyLine()}",
        adviceHeading = "${state.advice.progressLabel} · ${state.advice.title}",
        adviceDetail = state.advice.detail,
        footer = "${text.stageName(state.development.stage)} · ${text.saveStatus(secondsSinceSave)} · ${state.message}",
        primaryLabels = FerrumPrimaryDestination.entries.map(text::primaryDestination),
        secondaryLabels = FerrumSecondaryDestination.entries.map(text::secondaryDestination),
        recipeLabel = actions.recipeLabel(state),
        actionLabel = actions.actionLabel(state),
        taskLabel = actions.taskLabel(state),
        utilityLabel = actions.utilityLabel(state),
        menuOpen = state.menuOpen,
        recipeEnabled = actions.recipeAvailable(state),
        actionEnabled = actions.actionAvailable(state),
        taskEnabled = actions.taskAvailable(state),
    )
}
