package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.game.ferrum.model.FerrumIds
import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.shared.GameId

internal class FerrumSelectionPresenter(
    private val controller: ManufacturingCoordinator,
    private val clock: ClockService,
    private val text: FerrumTextCatalog,
) {
    private val refiningDefinitions = controller.refiningDefinitions
    private val assemblyDefinitions = controller.assemblyDefinitions
    val refiningRecipeIds: List<GameId> = FerrumIds.REFINING_RECIPES.filter(refiningDefinitions.recipes::containsKey)
    val assemblyRecipeIds: List<GameId> = FerrumIds.ASSEMBLY_RECIPES.filter(assemblyDefinitions.recipes::containsKey)
    private val gameState get() = controller.state

    fun recipeAvailable(state: FerrumScreenState): Boolean =
        state.selected == FerrumNodeId.REFINER || state.selected == FerrumNodeId.ASSEMBLER

    fun actionAvailable(state: FerrumScreenState): Boolean = when (state.selected) {
        FerrumNodeId.BASE -> controller.economyDefinitions.resources.any { (id, definition) ->
            definition.sellable && controller.stock(id) > 0L
        }
        FerrumNodeId.REFINER -> selectedRefiningRecipeId(state)?.let(controller::canLaunchRefining) == true
        FerrumNodeId.ASSEMBLER -> selectedAssemblyRecipeId(state)?.let(controller::canLaunchAssembly) == true
        FerrumNodeId.IRON_DEPOSIT -> pending(FerrumIds.DEPOSIT_IRON) > 0L
        FerrumNodeId.COPPER_DEPOSIT -> pending(FerrumIds.DEPOSIT_COPPER) > 0L
        FerrumNodeId.CRYSTAL_DEPOSIT -> pending(FerrumIds.DEPOSIT_CRYSTAL) > 0L
        null -> false
    }

    fun taskAvailable(state: FerrumScreenState): Boolean = when (state.selected) {
        FerrumNodeId.BASE -> readyCount() > 0 || gameState.refining.refundBuffer.values.any { it > 0L }
        FerrumNodeId.REFINER -> gameState.refining.jobs.isNotEmpty()
        FerrumNodeId.ASSEMBLER ->
            gameState.assembly.jobs.any { it.status == AssemblyJobStatus.READY_TO_COLLECT } ||
                technologyInstallAvailable(state)
        else -> false
    }

    fun recipeLabel(state: FerrumScreenState): String = when (state.selected) {
        FerrumNodeId.REFINER -> text.recipeLabel(selectedRefiningRecipeId(state))
        FerrumNodeId.ASSEMBLER -> text.recipeLabel(selectedAssemblyRecipeId(state))
        else -> text.recipeLabel(null)
    }

    fun actionLabel(state: FerrumScreenState): String = text.actionLabel(state.selected)

    fun taskLabel(state: FerrumScreenState): String {
        val ready = when (state.selected) {
            FerrumNodeId.REFINER -> gameState.refining.jobs.any { it.status == RefiningJobStatus.READY_TO_COLLECT }
            FerrumNodeId.ASSEMBLER -> gameState.assembly.jobs.any { it.status == AssemblyJobStatus.READY_TO_COLLECT }
            else -> readyCount() > 0
        }
        return text.taskLabel(
            node = state.selected,
            ready = ready,
            cancellable = state.selected == FerrumNodeId.REFINER && gameState.refining.jobs.isNotEmpty(),
            installable = technologyInstallAvailable(state),
        )
    }

    fun utilityLabel(state: FerrumScreenState): String = text.utilityLabel(
        productionNode = state.selected == FerrumNodeId.REFINER || state.selected == FerrumNodeId.ASSEMBLER,
        batchLabel = state.batchMode.label,
    )

    fun selectionTitle(state: FerrumScreenState): String = text.selectionTitle(state.selected)

    fun economyLine(): String = text.economyLine(
        spaceDollars = gameState.economy.spaceDollars,
        iron = controller.stock(FerrumIds.RAW_IRON),
        copper = controller.stock(FerrumIds.RAW_COPPER),
        crystal = controller.stock(FerrumIds.RAW_CRYSTAL),
    )

    fun cycleRecipe(state: FerrumScreenState): FerrumActionFeedback {
        when (state.selected) {
            FerrumNodeId.REFINER -> if (refiningRecipeIds.isNotEmpty()) {
                state.refiningRecipeIndex = (state.refiningRecipeIndex + 1) % refiningRecipeIds.size
            }
            FerrumNodeId.ASSEMBLER -> if (assemblyRecipeIds.isNotEmpty()) {
                state.assemblyRecipeIndex = (state.assemblyRecipeIndex + 1) % assemblyRecipeIds.size
            }
            else -> return FerrumActionFeedback(state.message)
        }
        return FerrumActionFeedback(recipeLabel(state), FerrumFeedbackKind.IMPACT)
    }

    fun refiningDetails(state: FerrumScreenState): String {
        val ready = gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val running = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING }
        return when {
            ready > 0 -> text.refiningReady(ready)
            running != null -> text.refiningRunning(
                text.refiningRecipeName(running.recipeId),
                remainingSeconds(running.finishesAtEpochMillis),
            )
            else -> text.queue(
                selectedRefiningRecipeId(state)?.let(text::refiningRecipeName) ?: text.noRecipe,
                gameState.refining.jobs.size,
                refiningDefinitions.robot.queueCapacity,
            )
        }
    }

    fun assemblyDetails(state: FerrumScreenState): String {
        val recipe = selectedAssemblyRecipe(state) ?: return text.noRecipe
        val name = text.assemblyRecipeName(recipe.id)
        if (!gameState.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)) {
            return text.technologyRequired(name)
        }
        val ready = gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }
        if (ready > 0) return text.refiningReady(ready)
        return text.queue(name, gameState.assembly.jobs.size, assemblyDefinitions.robot.queueCapacity)
    }

    fun selectedRefiningRecipeId(state: FerrumScreenState): GameId? =
        refiningRecipeIds.getOrNull(state.refiningRecipeIndex)

    fun selectedAssemblyRecipeId(state: FerrumScreenState): GameId? =
        assemblyRecipeIds.getOrNull(state.assemblyRecipeIndex)

    fun selectedAssemblyTechnology(state: FerrumScreenState): TechnologyDefinition? {
        val output = selectedAssemblyRecipe(state)?.outputResourceId ?: return null
        return assemblyDefinitions.technologies.values.firstOrNull { it.itemResourceId == output }
    }

    fun technologyInstallAvailable(state: FerrumScreenState): Boolean =
        selectedAssemblyTechnology(state)?.let { controller.canInstallTechnology(it.id) } == true

    private fun selectedAssemblyRecipe(state: FerrumScreenState) =
        selectedAssemblyRecipeId(state)?.let(assemblyDefinitions.recipes::get)

    private fun readyCount(): Int =
        gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT } +
            gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }

    private fun pending(id: GameId): Long = gameState.economy.deposits[id]?.pendingCollection ?: 0L

    private fun remainingSeconds(finishAtEpochMillis: Long): Long =
        ((finishAtEpochMillis - clock.nowEpochMillis()) / 1_000L).coerceAtLeast(0L)
}
