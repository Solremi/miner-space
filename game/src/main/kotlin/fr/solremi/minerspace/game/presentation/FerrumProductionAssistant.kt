package fr.solremi.minerspace.game.presentation

import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.game.scene.FerrumColonyDevelopment
import fr.solremi.minerspace.game.scene.FerrumColonyVisualState
import fr.solremi.minerspace.game.scene.FerrumNodeId
import fr.solremi.minerspace.shared.GameId

data class FerrumProductionAdvice(
    val phase: Int,
    val totalPhases: Int,
    val title: String,
    val detail: String,
    val target: FerrumNodeId?,
) {
    val progressLabel: String get() = "DÉMARRAGE $phase/$totalPhases"
}

object FerrumProductionAssistant {
    private const val TOTAL_PHASES = 7

    fun evaluate(
        state: ManufacturingGameState,
        refining: RefiningDefinitions,
        assembly: AssemblyDefinitions,
    ): FerrumProductionAdvice {
        FerrumColonyVisualState.update(FerrumColonyDevelopment.from(state))

        state.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }?.let { job ->
            return advice(
                phase = phase(state),
                title = "Production terminée",
                detail = "${refiningName(job.recipeId)} prête dans le raffineur. Touchez ici pour vous y rendre.",
                target = FerrumNodeId.REFINER,
            )
        }
        state.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }?.let { job ->
            return advice(
                phase = phase(state),
                title = "Assemblage terminé",
                detail = "${assemblyName(job.recipeId)} prêt dans l’assembleur.",
                target = FerrumNodeId.ASSEMBLER,
            )
        }
        if (state.refining.refundBuffer.values.any { it > 0L }) {
            return advice(
                phase = phase(state),
                title = "Ressources remboursées",
                detail = "La base conserve des matériaux issus d’une production annulée.",
                target = FerrumNodeId.BASE,
            )
        }

        val ironAvailable = stock(state, RAW_IRON) + pending(state, DEPOSIT_IRON)
        if (ironAvailable < 10L) {
            return advice(
                phase = 1,
                title = "Réveiller Aster",
                detail = "Collectez 10 unités de fer pour relancer la première chaîne de Ferrum Delta.",
                target = FerrumNodeId.IRON_DEPOSIT,
            )
        }

        val copperAvailable = stock(state, RAW_COPPER) + pending(state, DEPOSIT_COPPER)
        if (copperAvailable < 6L) {
            return advice(
                phase = 2,
                title = "Rétablir le circuit cuivre",
                detail = "Collectez 6 unités de cuivre. Rhea en a besoin pour stabiliser la raffinerie.",
                target = FerrumNodeId.COPPER_DEPOSIT,
            )
        }

        if (stock(state, REFINED_IRON) < 3L) {
            val recipe = refining.recipes[RECIPE_IRON]
            return advice(
                phase = 3,
                title = "Produire les premiers lingots",
                detail = missingOrReady(state, recipe?.inputs.orEmpty(), "Lancez un raffinage de fer."),
                target = if (recipe != null && hasInputs(state, recipe.inputs)) FerrumNodeId.REFINER else FerrumNodeId.IRON_DEPOSIT,
            )
        }

        if (stock(state, REFINED_COPPER) < 2L) {
            val recipe = refining.recipes[RECIPE_COPPER]
            return advice(
                phase = 3,
                title = "Préparer les plaques conductrices",
                detail = missingOrReady(state, recipe?.inputs.orEmpty(), "Lancez un raffinage de cuivre."),
                target = if (recipe != null && hasInputs(state, recipe.inputs)) FerrumNodeId.REFINER else FerrumNodeId.COPPER_DEPOSIT,
            )
        }

        if (stock(state, POWER_CELL) < 1L) {
            val recipe = assembly.recipes[ASSEMBLY_POWER_CELL]
            return advice(
                phase = 4,
                title = "Assembler une pile énergétique",
                detail = missingOrReady(state, recipe?.inputs.orEmpty(), "Kestrel peut assembler la première pile."),
                target = FerrumNodeId.ASSEMBLER,
            )
        }

        if (stock(state, SENSOR_ARRAY) < 1L) {
            val recipe = assembly.recipes[ASSEMBLY_SENSOR_ARRAY]
            val crystalAvailable = stock(state, RAW_CRYSTAL) + pending(state, DEPOSIT_CRYSTAL)
            val target = when {
                crystalAvailable < 4L -> FerrumNodeId.CRYSTAL_DEPOSIT
                recipe != null && hasInputs(state, recipe.inputs) -> FerrumNodeId.ASSEMBLER
                stock(state, REFINED_COPPER) < 2L -> FerrumNodeId.REFINER
                else -> FerrumNodeId.ASSEMBLER
            }
            return advice(
                phase = 5,
                title = "Donner des yeux à la base",
                detail = missingOrReady(state, recipe?.inputs.orEmpty(), "Assemblez le premier réseau de capteurs."),
                target = target,
            )
        }

        if (TECH_EXTRACTION !in state.assembly.installedTechnologyIds) {
            if (stock(state, TECH_EXTRACTION_ITEM) > 0L) {
                return advice(
                    phase = 7,
                    title = "Installer le protocole d’extraction",
                    detail = "Le module est prêt. Installez-le pour accélérer toutes les foreuses.",
                    target = FerrumNodeId.ASSEMBLER,
                )
            }
            val recipe = assembly.recipes[ASSEMBLY_TECH_EXTRACTION]
            return advice(
                phase = 6,
                title = "Compiler le protocole d’extraction",
                detail = missingOrReady(state, recipe?.inputs.orEmpty(), "Fabriquez le module technologique puis récupérez-le."),
                target = FerrumNodeId.ASSEMBLER,
            )
        }

        val running = state.refining.jobs.size + state.assembly.jobs.size
        return advice(
            phase = TOTAL_PHASES,
            title = "Chaîne initiale opérationnelle",
            detail = if (running > 0) {
                "$running tâche(s) en cours. Le prochain objectif se trouve dans Missions."
            } else {
                "Aster, Rhea et Kestrel attendent vos ordres. Développez maintenant la colonie."
            },
            target = null,
        )
    }

    private fun phase(state: ManufacturingGameState): Int = when {
        TECH_EXTRACTION in state.assembly.installedTechnologyIds -> 7
        stock(state, TECH_EXTRACTION_ITEM) > 0L -> 7
        stock(state, POWER_CELL) >= 2L && stock(state, SENSOR_ARRAY) >= 1L -> 6
        stock(state, SENSOR_ARRAY) >= 1L -> 6
        stock(state, POWER_CELL) >= 1L -> 5
        stock(state, REFINED_IRON) >= 3L && stock(state, REFINED_COPPER) >= 2L -> 4
        stock(state, RAW_COPPER) + pending(state, DEPOSIT_COPPER) >= 6L -> 3
        stock(state, RAW_IRON) + pending(state, DEPOSIT_IRON) >= 10L -> 2
        else -> 1
    }

    private fun advice(phase: Int, title: String, detail: String, target: FerrumNodeId?) =
        FerrumProductionAdvice(phase.coerceIn(1, TOTAL_PHASES), TOTAL_PHASES, title, detail, target)

    private fun missingOrReady(
        state: ManufacturingGameState,
        inputs: Map<GameId, Long>,
        readyText: String,
    ): String {
        val missing = inputs.mapNotNull { (id, required) ->
            val amount = (required - stock(state, id)).coerceAtLeast(0L)
            if (amount == 0L) null else "$amount ${resourceName(id)}"
        }
        return if (missing.isEmpty()) readyText else "Il manque ${missing.joinToString(", ")}."
    }

    private fun hasInputs(state: ManufacturingGameState, inputs: Map<GameId, Long>): Boolean =
        inputs.all { (id, required) -> stock(state, id) >= required }

    private fun stock(state: ManufacturingGameState, id: GameId): Long =
        state.economy.inventory[id] ?: 0L

    private fun pending(state: ManufacturingGameState, id: GameId): Long =
        state.economy.deposits[id]?.pendingCollection ?: 0L

    private fun resourceName(id: GameId): String = when (id) {
        RAW_IRON -> "fer"
        RAW_COPPER -> "cuivre"
        RAW_CRYSTAL -> "cristal"
        REFINED_IRON -> "lingot(s) de fer"
        REFINED_COPPER -> "plaque(s) de cuivre"
        POWER_CELL -> "pile(s) énergétique(s)"
        SENSOR_ARRAY -> "réseau(x) de capteurs"
        else -> id.value.replace('_', ' ')
    }

    private fun refiningName(id: GameId): String = when (id) {
        RECIPE_IRON -> "Les lingots de fer"
        RECIPE_COPPER -> "Les plaques de cuivre"
        else -> "La production"
    }

    private fun assemblyName(id: GameId): String = when (id) {
        ASSEMBLY_POWER_CELL -> "La pile énergétique"
        ASSEMBLY_SENSOR_ARRAY -> "Le réseau de capteurs"
        ASSEMBLY_TECH_EXTRACTION -> "Le protocole d’extraction"
        else -> "Le composant"
    }

    private val RAW_IRON = GameId.of("raw_iron")
    private val RAW_COPPER = GameId.of("raw_copper")
    private val RAW_CRYSTAL = GameId.of("raw_crystal")
    private val REFINED_IRON = GameId.of("refined_iron_ingot")
    private val REFINED_COPPER = GameId.of("refined_copper_plate")
    private val POWER_CELL = GameId.of("component_power_cell")
    private val SENSOR_ARRAY = GameId.of("component_sensor_array")
    private val TECH_EXTRACTION_ITEM = GameId.of("tech_extraction_protocol_item")
    private val TECH_EXTRACTION = GameId.of("tech_extraction_protocol")
    private val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
    private val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
    private val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")
    private val RECIPE_IRON = GameId.of("recipe_iron_ingot")
    private val RECIPE_COPPER = GameId.of("recipe_copper_plate")
    private val ASSEMBLY_POWER_CELL = GameId.of("assembly_power_cell")
    private val ASSEMBLY_SENSOR_ARRAY = GameId.of("assembly_sensor_array")
    private val ASSEMBLY_TECH_EXTRACTION = GameId.of("assembly_tech_extraction_protocol")
}
