package fr.solremi.minerspace.domain.assembly

import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.economy.FixedPointMath
import fr.solremi.minerspace.domain.economy.MULTIPLIER_SCALE
import fr.solremi.minerspace.domain.economy.ProductionFormula
import fr.solremi.minerspace.domain.economy.ProductionMultipliers
import fr.solremi.minerspace.domain.refining.RefiningGameState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.shared.GameId

enum class AssemblyOutputKind { COMPONENT, TECHNOLOGY }

data class AssemblyRecipeDefinition(
    val id: GameId,
    val nameKey: String,
    val inputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val durationSeconds: Long,
    val outputKind: AssemblyOutputKind,
    val requiredTechnologyIds: Set<GameId> = emptySet(),
) {
    init {
        require(nameKey.isNotBlank())
        require(inputs.isNotEmpty())
        require(inputs.values.all { it > 0L })
        require(outputQuantity > 0L)
        require(durationSeconds > 0L)
    }
}

data class TechnologyDefinition(
    val id: GameId,
    val nameKey: String,
    val itemResourceId: GameId,
    val requiredTechnologyIds: Set<GameId>,
    val extractionBonusMillionths: Long,
) {
    init {
        require(nameKey.isNotBlank())
        require(extractionBonusMillionths in 0L..MULTIPLIER_SCALE)
        require(id !in requiredTechnologyIds)
    }
}

data class AssemblerRobotDefinition(
    val id: GameId,
    val nameKey: String,
    val queueCapacity: Int,
) {
    init {
        require(nameKey.isNotBlank())
        require(queueCapacity > 0)
    }
}

data class AssemblyDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val robot: AssemblerRobotDefinition,
    val recipes: Map<GameId, AssemblyRecipeDefinition>,
    val technologies: Map<GameId, TechnologyDefinition>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(recipes.isNotEmpty())
        require(technologies.isNotEmpty())
        recipes.values.forEach { recipe ->
            recipe.requiredTechnologyIds.forEach { require(technologies.containsKey(it)) }
        }
        technologies.values.forEach { technology ->
            technology.requiredTechnologyIds.forEach { require(technologies.containsKey(it)) }
            require(recipes.values.any { it.outputResourceId == technology.itemResourceId }) {
                "Technology ${technology.id} has no assembly recipe."
            }
        }
    }
}

enum class AssemblyJobStatus { QUEUED, RUNNING, READY_TO_COLLECT }

data class AssemblyJob(
    val id: String,
    val recipeId: GameId,
    val queuedAtEpochMillis: Long,
    val startsAtEpochMillis: Long,
    val finishesAtEpochMillis: Long,
    val reservedInputs: Map<GameId, Long>,
    val outputResourceId: GameId,
    val outputQuantity: Long,
    val outputKind: AssemblyOutputKind,
    val status: AssemblyJobStatus,
) {
    init {
        require(id.isNotBlank())
        require(queuedAtEpochMillis >= 0L)
        require(startsAtEpochMillis >= queuedAtEpochMillis)
        require(finishesAtEpochMillis > startsAtEpochMillis)
        require(reservedInputs.isNotEmpty())
        require(reservedInputs.values.all { it > 0L })
        require(outputQuantity > 0L)
    }
}

data class AssemblyState(
    val jobs: List<AssemblyJob>,
    val installedTechnologyIds: Set<GameId>,
    val nextJobSequence: Long,
) {
    init {
        require(nextJobSequence >= 1L)
        require(jobs.map { it.id }.distinct().size == jobs.size)
    }

    companion object {
        fun empty(): AssemblyState = AssemblyState(emptyList(), emptySet(), 1L)
    }
}

data class ManufacturingGameState(
    val economy: EconomyState,
    val refining: RefiningState,
    val assembly: AssemblyState,
) {
    fun refiningView(): RefiningGameState = RefiningGameState(economy, refining)

    fun withRefining(state: RefiningGameState): ManufacturingGameState = copy(
        economy = state.economy,
        refining = state.refining,
    )
}

data class AssemblyTransaction(
    val reason: String,
    val jobId: String? = null,
    val inventoryDeltas: Map<GameId, Long> = emptyMap(),
    val technologyId: GameId? = null,
)

sealed interface AssemblyCommandResult {
    val state: ManufacturingGameState

    data class Applied(
        override val state: ManufacturingGameState,
        val transaction: AssemblyTransaction,
    ) : AssemblyCommandResult

    data class Rejected(
        override val state: ManufacturingGameState,
        val code: String,
    ) : AssemblyCommandResult
}

data class ProductionComparison(
    val basePerMinute: Long,
    val currentPerMinute: Long,
    val projectedPerMinute: Long,
)

class AssemblyEngine(
    val definitions: AssemblyDefinitions,
    private val storageCapacities: Map<GameId, Long>,
) {
    fun reconcile(state: ManufacturingGameState, nowEpochMillis: Long): ManufacturingGameState {
        require(nowEpochMillis >= 0L)
        val jobs = state.assembly.jobs.map { job ->
            val status = when {
                nowEpochMillis >= job.finishesAtEpochMillis -> AssemblyJobStatus.READY_TO_COLLECT
                nowEpochMillis >= job.startsAtEpochMillis -> AssemblyJobStatus.RUNNING
                else -> AssemblyJobStatus.QUEUED
            }
            if (status == job.status) job else job.copy(status = status)
        }
        return if (jobs == state.assembly.jobs) state else state.copy(
            assembly = state.assembly.copy(jobs = jobs),
        )
    }

    fun launch(
        state: ManufacturingGameState,
        recipeId: GameId,
        nowEpochMillis: Long,
    ): AssemblyCommandResult {
        val current = reconcile(state, nowEpochMillis)
        val recipe = definitions.recipes[recipeId]
            ?: return AssemblyCommandResult.Rejected(current, "unknown_recipe")
        if (!current.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)) {
            return AssemblyCommandResult.Rejected(current, "technology_prerequisite_missing")
        }
        if (current.assembly.jobs.size >= definitions.robot.queueCapacity) {
            return AssemblyCommandResult.Rejected(current, "assembly_queue_full")
        }
        recipe.inputs.forEach { (resourceId, quantity) ->
            if ((current.economy.inventory[resourceId] ?: 0L) < quantity) {
                return AssemblyCommandResult.Rejected(current, "missing_input:${resourceId.value}")
            }
        }

        val inventory = current.economy.inventory.toMutableMap()
        recipe.inputs.forEach { (resourceId, quantity) ->
            inventory[resourceId] = inventory.getValue(resourceId) - quantity
        }
        val startsAt = maxOf(
            nowEpochMillis,
            current.assembly.jobs.maxOfOrNull { it.finishesAtEpochMillis } ?: nowEpochMillis,
        )
        val finishesAt = Math.addExact(
            startsAt,
            Math.multiplyExact(recipe.durationSeconds, 1_000L),
        )
        val sequence = current.assembly.nextJobSequence
        val job = AssemblyJob(
            id = "as_job_$sequence",
            recipeId = recipe.id,
            queuedAtEpochMillis = nowEpochMillis,
            startsAtEpochMillis = startsAt,
            finishesAtEpochMillis = finishesAt,
            reservedInputs = recipe.inputs,
            outputResourceId = recipe.outputResourceId,
            outputQuantity = recipe.outputQuantity,
            outputKind = recipe.outputKind,
            status = if (startsAt <= nowEpochMillis) AssemblyJobStatus.RUNNING else AssemblyJobStatus.QUEUED,
        )
        val next = current.copy(
            economy = current.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(current.economy.transactionSequence, 1L),
            ),
            assembly = current.assembly.copy(
                jobs = current.assembly.jobs + job,
                nextJobSequence = FixedPointMath.addExact(sequence, 1L),
            ),
        )
        return AssemblyCommandResult.Applied(
            next,
            AssemblyTransaction(
                reason = "launch_assembly",
                jobId = job.id,
                inventoryDeltas = recipe.inputs.mapValues { -it.value },
            ),
        )
    }

    fun collect(
        state: ManufacturingGameState,
        jobId: String,
        nowEpochMillis: Long,
    ): AssemblyCommandResult {
        val current = reconcile(state, nowEpochMillis)
        val job = current.assembly.jobs.firstOrNull { it.id == jobId }
            ?: return AssemblyCommandResult.Rejected(current, "unknown_assembly_job")
        if (job.status != AssemblyJobStatus.READY_TO_COLLECT) {
            return AssemblyCommandResult.Rejected(current, "assembly_job_not_completed")
        }
        val capacity = storageCapacities[job.outputResourceId]
            ?: return AssemblyCommandResult.Rejected(current, "unknown_output_storage")
        val stored = current.economy.inventory[job.outputResourceId] ?: 0L
        if (capacity - stored < job.outputQuantity) {
            return AssemblyCommandResult.Rejected(current, "output_storage_full")
        }
        val inventory = current.economy.inventory.toMutableMap().apply {
            this[job.outputResourceId] = FixedPointMath.addExact(stored, job.outputQuantity)
        }
        val next = current.copy(
            economy = current.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(current.economy.transactionSequence, 1L),
            ),
            assembly = current.assembly.copy(
                jobs = current.assembly.jobs.filterNot { it.id == jobId },
            ),
        )
        return AssemblyCommandResult.Applied(
            next,
            AssemblyTransaction(
                reason = "collect_assembly",
                jobId = job.id,
                inventoryDeltas = mapOf(job.outputResourceId to job.outputQuantity),
            ),
        )
    }

    fun installTechnology(
        state: ManufacturingGameState,
        technologyId: GameId,
    ): AssemblyCommandResult {
        val technology = definitions.technologies[technologyId]
            ?: return AssemblyCommandResult.Rejected(state, "unknown_technology")
        if (technologyId in state.assembly.installedTechnologyIds) {
            return AssemblyCommandResult.Rejected(state, "technology_already_installed")
        }
        if (!state.assembly.installedTechnologyIds.containsAll(technology.requiredTechnologyIds)) {
            return AssemblyCommandResult.Rejected(state, "technology_prerequisite_missing")
        }
        val stored = state.economy.inventory[technology.itemResourceId] ?: 0L
        if (stored <= 0L) return AssemblyCommandResult.Rejected(state, "technology_item_missing")

        val inventory = state.economy.inventory.toMutableMap().apply {
            this[technology.itemResourceId] = stored - 1L
        }
        val next = state.copy(
            economy = state.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(state.economy.transactionSequence, 1L),
            ),
            assembly = state.assembly.copy(
                installedTechnologyIds = state.assembly.installedTechnologyIds + technologyId,
            ),
        )
        return AssemblyCommandResult.Applied(
            next,
            AssemblyTransaction(
                reason = "install_technology",
                inventoryDeltas = mapOf(technology.itemResourceId to -1L),
                technologyId = technologyId,
            ),
        )
    }

    fun productionMultipliers(state: AssemblyState): ProductionMultipliers {
        val technologyBonus = state.installedTechnologyIds.sumOf { technologyId ->
            definitions.technologies[technologyId]?.extractionBonusMillionths ?: 0L
        }
        return ProductionMultipliers(
            technologies = FixedPointMath.addExact(MULTIPLIER_SCALE, technologyBonus),
        )
    }

    fun compareExtraction(
        basePerSecond: Long,
        state: AssemblyState,
        previewTechnologyId: GameId? = null,
    ): ProductionComparison {
        require(basePerSecond >= 0L)
        val basePerMinute = Math.multiplyExact(basePerSecond, 60L)
        val current = ProductionFormula.floor(basePerMinute, productionMultipliers(state))
        val projectedState = if (previewTechnologyId == null || previewTechnologyId in state.installedTechnologyIds) {
            state
        } else {
            val technology = definitions.technologies[previewTechnologyId]
            if (technology == null || !state.installedTechnologyIds.containsAll(technology.requiredTechnologyIds)) {
                state
            } else {
                state.copy(installedTechnologyIds = state.installedTechnologyIds + previewTechnologyId)
            }
        }
        val projected = ProductionFormula.floor(basePerMinute, productionMultipliers(projectedState))
        return ProductionComparison(basePerMinute, current, projected)
    }
}
