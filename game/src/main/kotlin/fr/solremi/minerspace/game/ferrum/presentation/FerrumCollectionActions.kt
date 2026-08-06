package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingActionResult
import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.shared.GameId

internal class FerrumCollectionActions(
    private val controller: ManufacturingCoordinator,
    private val presenter: FerrumSelectionPresenter,
    private val text: FerrumTextCatalog,
    private val feedbackMapper: ManufacturingFeedbackMapper,
) {
    private val gameState get() = controller.state

    fun collectDeposit(depositId: GameId): FerrumActionFeedback =
        feedbackMapper.map(controller.collectDeposit(depositId))

    fun performTask(state: FerrumScreenState): FerrumActionFeedback = when (state.selected) {
        FerrumNodeId.BASE -> collectAllAvailable()
        FerrumNodeId.REFINER -> collectOrCancelRefining()
        FerrumNodeId.ASSEMBLER -> collectOrInstallAssembly(state)
        else -> FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
    }

    private fun collectOrCancelRefining(): FerrumActionFeedback {
        val ready = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val job = ready ?: gameState.refining.jobs.firstOrNull()
            ?: return FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
        return feedbackMapper.map(
            if (ready != null) controller.collectRefining(job.id) else controller.cancelRefining(job.id),
        )
    }

    private fun collectOrInstallAssembly(state: FerrumScreenState): FerrumActionFeedback {
        val ready = gameState.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }
        if (ready != null) return feedbackMapper.map(controller.collectAssembly(ready.id))
        val technology = presenter.selectedAssemblyTechnology(state)
        return if (technology != null && controller.canInstallTechnology(technology.id)) {
            feedbackMapper.map(controller.installTechnology(technology.id))
        } else {
            FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
        }
    }

    private fun collectAllAvailable(): FerrumActionFeedback {
        var collected = 0
        val refiningIds = gameState.refining.jobs
            .filter { it.status == RefiningJobStatus.READY_TO_COLLECT }
            .map { it.id }
        for (id in refiningIds) {
            when (val result = controller.collectRefining(id)) {
                is ManufacturingActionResult.Applied -> collected++
                is ManufacturingActionResult.PersistenceFailed -> return feedbackMapper.map(result)
                is ManufacturingActionResult.Rejected -> Unit
            }
        }
        val assemblyIds = gameState.assembly.jobs
            .filter { it.status == AssemblyJobStatus.READY_TO_COLLECT }
            .map { it.id }
        for (id in assemblyIds) {
            when (val result = controller.collectAssembly(id)) {
                is ManufacturingActionResult.Applied -> collected++
                is ManufacturingActionResult.PersistenceFailed -> return feedbackMapper.map(result)
                is ManufacturingActionResult.Rejected -> Unit
            }
        }
        if (gameState.refining.refundBuffer.values.any { it > 0L }) {
            when (val result = controller.collectRefiningRefunds()) {
                is ManufacturingActionResult.Applied -> collected++
                is ManufacturingActionResult.PersistenceFailed -> return feedbackMapper.map(result)
                is ManufacturingActionResult.Rejected -> Unit
            }
        }
        return FerrumActionFeedback(
            text.collectedLots(collected),
            if (collected > 0) FerrumFeedbackKind.SUCCESS else FerrumFeedbackKind.WARNING,
        )
    }
}
