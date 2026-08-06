package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.data.manufacturing.ManufacturingActionResult
import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.game.ferrum.model.FerrumIds
import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.game.text.GameplayText
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.text.FrenchGameText
import fr.solremi.minerspace.shared.text.GameTextKey

enum class FerrumFeedbackKind { NONE, IMPACT, SUCCESS, WARNING }

data class FerrumActionFeedback(
    val message: String,
    val kind: FerrumFeedbackKind = FerrumFeedbackKind.NONE,
)

class FerrumActionController(
    private val controller: ManufacturingCoordinator,
    private val clock: ClockService,
    private val text: FerrumTextCatalog,
) {
    private val refiningDefinitions = controller.refiningDefinitions
    private val assemblyDefinitions = controller.assemblyDefinitions
    private val refiningRecipeIds = FerrumIds.REFINING_RECIPES.filter(refiningDefinitions.recipes::containsKey)
    private val assemblyRecipeIds = FerrumIds.ASSEMBLY_RECIPES.filter(assemblyDefinitions.recipes::containsKey)
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

    fun cycleBatch(state: FerrumScreenState): FerrumActionFeedback {
        state.batchMode = state.batchMode.next()
        return FerrumActionFeedback(text.batchSelected(state.batchMode.label), FerrumFeedbackKind.IMPACT)
    }

    fun performAction(state: FerrumScreenState): FerrumActionFeedback = when (state.selected) {
        FerrumNodeId.REFINER -> launchBatch(state, selectedRefiningRecipeId(state), refining = true)
        FerrumNodeId.ASSEMBLER -> launchBatch(state, selectedAssemblyRecipeId(state), refining = false)
        FerrumNodeId.BASE -> applyResult(controller.sellAll())
        FerrumNodeId.IRON_DEPOSIT -> applyResult(controller.collectDeposit(FerrumIds.DEPOSIT_IRON))
        FerrumNodeId.COPPER_DEPOSIT -> applyResult(controller.collectDeposit(FerrumIds.DEPOSIT_COPPER))
        FerrumNodeId.CRYSTAL_DEPOSIT -> applyResult(controller.collectDeposit(FerrumIds.DEPOSIT_CRYSTAL))
        null -> FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
    }

    fun performTask(state: FerrumScreenState): FerrumActionFeedback {
        return when (state.selected) {
            FerrumNodeId.BASE -> collectAllAvailable()
            FerrumNodeId.REFINER -> {
                val ready = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }
                val job = ready ?: gameState.refining.jobs.firstOrNull()
                    ?: return FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
                applyResult(if (ready != null) controller.collectRefining(job.id) else controller.cancelRefining(job.id))
            }
            FerrumNodeId.ASSEMBLER -> {
                val ready = gameState.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }
                if (ready != null) return applyResult(controller.collectAssembly(ready.id))
                val technology = selectedAssemblyTechnology(state)
                if (technology != null && controller.canInstallTechnology(technology.id)) {
                    applyResult(controller.installTechnology(technology.id))
                } else {
                    FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
                }
            }
            else -> FerrumActionFeedback(text.noTask, FerrumFeedbackKind.WARNING)
        }
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
            if (result !is ManufacturingActionResult.Applied) return applyResult(result)
            launched++
        }
        return if (launched > 0) {
            FerrumActionFeedback(text.batchLaunched(launched), FerrumFeedbackKind.SUCCESS)
        } else {
            FerrumActionFeedback(
                if (refining) refiningDetails(state) else assemblyDetails(state),
                FerrumFeedbackKind.WARNING,
            )
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
                is ManufacturingActionResult.PersistenceFailed -> return applyResult(result)
                is ManufacturingActionResult.Rejected -> Unit
            }
        }
        val assemblyIds = gameState.assembly.jobs
            .filter { it.status == AssemblyJobStatus.READY_TO_COLLECT }
            .map { it.id }
        for (id in assemblyIds) {
            when (val result = controller.collectAssembly(id)) {
                is ManufacturingActionResult.Applied -> collected++
                is ManufacturingActionResult.PersistenceFailed -> return applyResult(result)
                is ManufacturingActionResult.Rejected -> Unit
            }
        }
        if (gameState.refining.refundBuffer.values.any { it > 0L }) {
            when (val result = controller.collectRefiningRefunds()) {
                is ManufacturingActionResult.Applied -> collected++
                is ManufacturingActionResult.PersistenceFailed -> return applyResult(result)
                is ManufacturingActionResult.Rejected -> Unit
            }
        }
        return FerrumActionFeedback(
            text.collectedLots(collected),
            if (collected > 0) FerrumFeedbackKind.SUCCESS else FerrumFeedbackKind.WARNING,
        )
    }

    private fun applyResult(result: ManufacturingActionResult): FerrumActionFeedback = when (result) {
        is ManufacturingActionResult.Applied -> FerrumActionFeedback(
            GameplayText.manufacturingSuccess(result.reason),
            FerrumFeedbackKind.SUCCESS,
        )
        is ManufacturingActionResult.Rejected -> FerrumActionFeedback(
            GameplayText.manufacturingError(result.code),
            FerrumFeedbackKind.WARNING,
        )
        is ManufacturingActionResult.PersistenceFailed -> FerrumActionFeedback(
            FrenchGameText.text(GameTextKey.ACTION_CANCELLED_SAVE_UNAVAILABLE),
            FerrumFeedbackKind.WARNING,
        )
    }

    private fun selectedRefiningRecipeId(state: FerrumScreenState): GameId? =
        refiningRecipeIds.getOrNull(state.refiningRecipeIndex)

    private fun selectedAssemblyRecipeId(state: FerrumScreenState): GameId? =
        assemblyRecipeIds.getOrNull(state.assemblyRecipeIndex)

    private fun selectedAssemblyRecipe(state: FerrumScreenState) =
        selectedAssemblyRecipeId(state)?.let(assemblyDefinitions.recipes::get)

    private fun selectedAssemblyTechnology(state: FerrumScreenState): TechnologyDefinition? {
        val output = selectedAssemblyRecipe(state)?.outputResourceId ?: return null
        return assemblyDefinitions.technologies.values.firstOrNull { it.itemResourceId == output }
    }

    private fun technologyInstallAvailable(state: FerrumScreenState): Boolean =
        selectedAssemblyTechnology(state)?.let { controller.canInstallTechnology(it.id) } == true

    private fun readyCount(): Int =
        gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT } +
            gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }

    private fun pending(id: GameId): Long = gameState.economy.deposits[id]?.pendingCollection ?: 0L

    private fun remainingSeconds(finishAtEpochMillis: Long): Long =
        ((finishAtEpochMillis - clock.nowEpochMillis()) / 1_000L).coerceAtLeast(0L)
}
