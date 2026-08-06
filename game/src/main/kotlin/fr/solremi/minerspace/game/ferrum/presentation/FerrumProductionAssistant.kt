package fr.solremi.minerspace.game.ferrum.presentation

import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.game.ferrum.model.FerrumIds
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.text.FerrumAdviceKey
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.game.ferrum.text.FrenchFerrumText
import fr.solremi.minerspace.shared.GameId

data class FerrumProductionAdvice(
    val phase: Int,
    val totalPhases: Int,
    val progressLabel: String,
    val title: String,
    val detail: String,
    val target: FerrumNodeId?,
)

object FerrumProductionAssistant {
    private const val TOTAL_PHASES = 7

    fun initial(text: FerrumTextCatalog = FrenchFerrumText): FerrumProductionAdvice = advice(
        phase = 1,
        title = text.advice(FerrumAdviceKey.INITIAL_TITLE),
        detail = text.advice(FerrumAdviceKey.INITIAL_DETAIL),
        target = FerrumNodeId.IRON_DEPOSIT,
        text = text,
    )

    fun evaluate(
        state: ManufacturingGameState,
        refining: RefiningDefinitions,
        assembly: AssemblyDefinitions,
        text: FerrumTextCatalog = FrenchFerrumText,
    ): FerrumProductionAdvice {
        state.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }?.let { job ->
            return advice(
                phase = phase(state),
                title = text.advice(FerrumAdviceKey.PRODUCTION_READY_TITLE),
                detail = text.advice(
                    FerrumAdviceKey.PRODUCTION_READY_DETAIL,
                    mapOf("name" to text.refiningRecipeName(job.recipeId)),
                ),
                target = FerrumNodeId.REFINER,
                text = text,
            )
        }
        state.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }?.let { job ->
            return advice(
                phase = phase(state),
                title = text.advice(FerrumAdviceKey.ASSEMBLY_READY_TITLE),
                detail = text.advice(
                    FerrumAdviceKey.ASSEMBLY_READY_DETAIL,
                    mapOf("name" to text.assemblyRecipeName(job.recipeId)),
                ),
                target = FerrumNodeId.ASSEMBLER,
                text = text,
            )
        }
        if (state.refining.refundBuffer.values.any { it > 0L }) {
            return advice(
                phase = phase(state),
                title = text.advice(FerrumAdviceKey.REFUND_TITLE),
                detail = text.advice(FerrumAdviceKey.REFUND_DETAIL),
                target = FerrumNodeId.BASE,
                text = text,
            )
        }

        val ironAvailable = stock(state, FerrumIds.RAW_IRON) + pending(state, FerrumIds.DEPOSIT_IRON)
        if (ironAvailable < 10L) {
            return advice(
                phase = 1,
                title = text.advice(FerrumAdviceKey.IRON_TITLE),
                detail = text.advice(FerrumAdviceKey.IRON_DETAIL),
                target = FerrumNodeId.IRON_DEPOSIT,
                text = text,
            )
        }

        val copperAvailable = stock(state, FerrumIds.RAW_COPPER) + pending(state, FerrumIds.DEPOSIT_COPPER)
        if (copperAvailable < 6L) {
            return advice(
                phase = 2,
                title = text.advice(FerrumAdviceKey.COPPER_TITLE),
                detail = text.advice(FerrumAdviceKey.COPPER_DETAIL),
                target = FerrumNodeId.COPPER_DEPOSIT,
                text = text,
            )
        }

        if (stock(state, FerrumIds.REFINED_IRON) < 3L) {
            val recipe = refining.recipes[FerrumIds.RECIPE_IRON]
            return advice(
                phase = 3,
                title = text.advice(FerrumAdviceKey.IRON_REFINING_TITLE),
                detail = missingOrReady(
                    state,
                    recipe?.inputs.orEmpty(),
                    text.advice(FerrumAdviceKey.IRON_REFINING_READY),
                    text,
                ),
                target = if (recipe != null && hasInputs(state, recipe.inputs)) {
                    FerrumNodeId.REFINER
                } else {
                    FerrumNodeId.IRON_DEPOSIT
                },
                text = text,
            )
        }

        if (stock(state, FerrumIds.REFINED_COPPER) < 2L) {
            val recipe = refining.recipes[FerrumIds.RECIPE_COPPER]
            return advice(
                phase = 3,
                title = text.advice(FerrumAdviceKey.COPPER_REFINING_TITLE),
                detail = missingOrReady(
                    state,
                    recipe?.inputs.orEmpty(),
                    text.advice(FerrumAdviceKey.COPPER_REFINING_READY),
                    text,
                ),
                target = if (recipe != null && hasInputs(state, recipe.inputs)) {
                    FerrumNodeId.REFINER
                } else {
                    FerrumNodeId.COPPER_DEPOSIT
                },
                text = text,
            )
        }

        if (stock(state, FerrumIds.POWER_CELL) < 1L) {
            val recipe = assembly.recipes[FerrumIds.ASSEMBLY_POWER_CELL]
            return advice(
                phase = 4,
                title = text.advice(FerrumAdviceKey.POWER_CELL_TITLE),
                detail = missingOrReady(
                    state,
                    recipe?.inputs.orEmpty(),
                    text.advice(FerrumAdviceKey.POWER_CELL_READY),
                    text,
                ),
                target = FerrumNodeId.ASSEMBLER,
                text = text,
            )
        }

        if (stock(state, FerrumIds.SENSOR_ARRAY) < 1L) {
            val recipe = assembly.recipes[FerrumIds.ASSEMBLY_SENSOR_ARRAY]
            val crystalAvailable = stock(state, FerrumIds.RAW_CRYSTAL) + pending(state, FerrumIds.DEPOSIT_CRYSTAL)
            val target = when {
                crystalAvailable < 4L -> FerrumNodeId.CRYSTAL_DEPOSIT
                recipe != null && hasInputs(state, recipe.inputs) -> FerrumNodeId.ASSEMBLER
                stock(state, FerrumIds.REFINED_COPPER) < 2L -> FerrumNodeId.REFINER
                else -> FerrumNodeId.ASSEMBLER
            }
            return advice(
                phase = 5,
                title = text.advice(FerrumAdviceKey.SENSOR_TITLE),
                detail = missingOrReady(
                    state,
                    recipe?.inputs.orEmpty(),
                    text.advice(FerrumAdviceKey.SENSOR_READY),
                    text,
                ),
                target = target,
                text = text,
            )
        }

        if (FerrumIds.TECH_EXTRACTION !in state.assembly.installedTechnologyIds) {
            if (stock(state, FerrumIds.TECH_EXTRACTION_ITEM) > 0L) {
                return advice(
                    phase = 7,
                    title = text.advice(FerrumAdviceKey.INSTALL_TECH_TITLE),
                    detail = text.advice(FerrumAdviceKey.INSTALL_TECH_DETAIL),
                    target = FerrumNodeId.ASSEMBLER,
                    text = text,
                )
            }
            val recipe = assembly.recipes[FerrumIds.ASSEMBLY_TECH_EXTRACTION]
            return advice(
                phase = 6,
                title = text.advice(FerrumAdviceKey.BUILD_TECH_TITLE),
                detail = missingOrReady(
                    state,
                    recipe?.inputs.orEmpty(),
                    text.advice(FerrumAdviceKey.BUILD_TECH_READY),
                    text,
                ),
                target = FerrumNodeId.ASSEMBLER,
                text = text,
            )
        }

        val running = state.refining.jobs.size + state.assembly.jobs.size
        return advice(
            phase = TOTAL_PHASES,
            title = text.advice(FerrumAdviceKey.COMPLETE_TITLE),
            detail = if (running > 0) {
                text.advice(FerrumAdviceKey.COMPLETE_RUNNING, mapOf("count" to running))
            } else {
                text.advice(FerrumAdviceKey.COMPLETE_IDLE)
            },
            target = null,
            text = text,
        )
    }

    private fun phase(state: ManufacturingGameState): Int = when {
        FerrumIds.TECH_EXTRACTION in state.assembly.installedTechnologyIds -> 7
        stock(state, FerrumIds.TECH_EXTRACTION_ITEM) > 0L -> 7
        stock(state, FerrumIds.POWER_CELL) >= 2L && stock(state, FerrumIds.SENSOR_ARRAY) >= 1L -> 6
        stock(state, FerrumIds.SENSOR_ARRAY) >= 1L -> 6
        stock(state, FerrumIds.POWER_CELL) >= 1L -> 5
        stock(state, FerrumIds.REFINED_IRON) >= 3L && stock(state, FerrumIds.REFINED_COPPER) >= 2L -> 4
        stock(state, FerrumIds.RAW_COPPER) + pending(state, FerrumIds.DEPOSIT_COPPER) >= 6L -> 3
        stock(state, FerrumIds.RAW_IRON) + pending(state, FerrumIds.DEPOSIT_IRON) >= 10L -> 2
        else -> 1
    }

    private fun advice(
        phase: Int,
        title: String,
        detail: String,
        target: FerrumNodeId?,
        text: FerrumTextCatalog,
    ): FerrumProductionAdvice {
        val normalized = phase.coerceIn(1, TOTAL_PHASES)
        return FerrumProductionAdvice(
            phase = normalized,
            totalPhases = TOTAL_PHASES,
            progressLabel = text.progressLabel(normalized, TOTAL_PHASES),
            title = title,
            detail = detail,
            target = target,
        )
    }

    private fun missingOrReady(
        state: ManufacturingGameState,
        inputs: Map<GameId, Long>,
        readyText: String,
        text: FerrumTextCatalog,
    ): String {
        val missing = inputs.mapNotNull { (id, required) ->
            val amount = (required - stock(state, id)).coerceAtLeast(0L)
            if (amount == 0L) null else "$amount ${text.resourceName(id)}"
        }
        return if (missing.isEmpty()) {
            readyText
        } else {
            text.advice(FerrumAdviceKey.MISSING_INPUTS, mapOf("items" to missing.joinToString(", ")))
        }
    }

    private fun hasInputs(state: ManufacturingGameState, inputs: Map<GameId, Long>): Boolean =
        inputs.all { (id, required) -> stock(state, id) >= required }

    private fun stock(state: ManufacturingGameState, id: GameId): Long =
        state.economy.inventory[id] ?: 0L

    private fun pending(state: ManufacturingGameState, id: GameId): Long =
        state.economy.deposits[id]?.pendingCollection ?: 0L
}
