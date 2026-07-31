package fr.solremi.minerspace.data.cryos

import fr.solremi.minerspace.domain.cryos.*
import fr.solremi.minerspace.shared.GameId

object CryosIxContentFactory {
    fun create(): CryosIxDefinitions {
        val landing = id("sector_cryos_landing")
        val glass = id("sector_cryos_glass_fields")
        val brine = id("sector_cryos_brine_rift")
        val aurora = id("sector_cryos_aurora_shelf")
        val abyss = id("sector_cryos_thermal_abyss")
        val gate = id("sector_cryos_frontier_gate")
        val sectors = listOf(
            CryosSectorDefinition(landing, "Zone d’atterrissage", emptySet(), 0, 80, 18),
            CryosSectorDefinition(glass, "Champs de verre", setOf(landing), 1, 120, 22),
            CryosSectorDefinition(brine, "Faille saline", setOf(glass), 2, 150, 26),
            CryosSectorDefinition(aurora, "Plateau auroral", setOf(brine), 3, 180, 30),
            CryosSectorDefinition(abyss, "Abîme thermique", setOf(aurora), 4, 220, 36),
            CryosSectorDefinition(gate, "Porte de frontière", setOf(abyss), 5, 260, 42),
        ).associateBy { it.id }

        val cryonite = id("raw_cryonite")
        val silicate = id("raw_ice_silicate")
        val brineSalt = id("raw_thermal_brine")
        val auroraCrystal = id("raw_aurora_crystal")
        val resources = listOf(
            CryosResourceDefinition(cryonite, "Cryonite", landing, true),
            CryosResourceDefinition(silicate, "Silicate glacé", landing, true),
            CryosResourceDefinition(brineSalt, "Saumure thermique", brine, true),
            CryosResourceDefinition(auroraCrystal, "Cristal auroral", aurora, true),
        ).associateBy { it.id }

        val refinedCryonite = CryosIxEngine.REFINED_CRYONITE
        val refinedGlass = CryosIxEngine.REFINED_GLASS
        val refinedSalt = id("refined_thermal_salt")
        val refinedCrystal = id("refined_aurora_lens")
        val refined = linkedSetOf(refinedCryonite, refinedGlass, refinedSalt, refinedCrystal)

        val recipes = listOf(
            recipe("recipe_cryonite_plate", mapOf(cryonite to 6L), refinedCryonite, 2, 18, 15),
            recipe("recipe_thermal_glass", mapOf(silicate to 6L), refinedGlass, 2, 18, 15),
            recipe("recipe_thermal_salt", mapOf(brineSalt to 5L), refinedSalt, 2, 22, 20),
            recipe("recipe_aurora_lens", mapOf(auroraCrystal to 5L), refinedCrystal, 2, 24, 22),
            recipe("recipe_heat_cell", mapOf(refinedCryonite to 2L, refinedSalt to 1L), id("component_heat_cell"), 1, 28, 25),
            recipe("recipe_thermal_conduit", mapOf(refinedGlass to 2L, refinedCryonite to 1L), id("component_thermal_conduit"), 1, 30, 25),
            recipe("recipe_aurora_sensor", mapOf(refinedCrystal to 2L, refinedGlass to 1L), id("component_aurora_sensor"), 1, 32, 28),
            recipe("recipe_frontier_coupler", mapOf(refinedSalt to 2L, refinedCrystal to 2L), id("component_frontier_coupler"), 1, 36, 30),
        ).associateBy { it.id }

        val tech1 = CryosIxEngine.TECH_EFFICIENT_GRID
        val tech2 = CryosIxEngine.TECH_THERMAL_RECOVERY
        val tech3 = CryosIxEngine.TECH_CRYO_DRILL
        val tech4 = id("tech_cryos_insulated_logistics")
        val tech5 = id("tech_cryos_frontier_stabilizer")
        val technologies = listOf(
            technology(tech1, emptySet(), 70, 60, 4),
            technology(tech2, setOf(tech1), 85, 75, 6),
            technology(tech3, setOf(tech1), 95, 85, 8),
            technology(tech4, setOf(tech2), 115, 100, 10),
            technology(tech5, setOf(tech3, tech4), 140, 125, 12),
        ).associateBy { it.id }

        val setId = id("set_cryos_thermal")
        val modules = (1..8).map { index ->
            val requirements = when {
                index <= 2 -> emptySet()
                index <= 4 -> setOf(tech1)
                index <= 6 -> setOf(tech2)
                else -> setOf(tech3)
            }
            CryosModuleDefinition(
                id = id("module_cryos_${index.toString().padStart(2, '0')}"),
                setId = setId,
                requiredTechnologyIds = requirements,
                refinedMaterialCost = 4L + index,
                energyCost = 35L + index * 5L,
            )
        }.associateBy { it.id }

        val main = ids("mission_cryos_main", 12)
        val secondary = ids("mission_cryos_secondary", 10)
        val events = listOf(id("event_cryos_whiteout"), id("event_cryos_thermal_vent"), id("event_cryos_aurora_storm"))
        val narrative = listOf(id("narrative_cryos_first_signal"), id("narrative_cryos_frontier_memory"))

        val codexSources = buildList {
            addAll(resources.keys)
            addAll(refined)
            addAll(recipes.values.map { it.outputId })
            addAll(technologies.keys)
            addAll(modules.keys)
            addAll(sectors.keys)
            addAll(events)
            addAll(narrative)
        }
        val codex = codexSources.distinct().take(30).map { id("codex_${it.value}") }
        require(codex.size == 30)

        return CryosIxDefinitions(
            schemaVersion = 1,
            contentVersion = "1.0.0",
            sectors = sectors,
            resources = resources,
            refinedMaterialIds = refined,
            recipes = recipes,
            technologies = technologies,
            modules = modules,
            mainMissionIds = main,
            secondaryMissionIds = secondary,
            eventIds = events,
            narrativeDiscoveryIds = narrative,
            codexEntryIds = codex,
            thermalSetId = setId,
        )
    }

    private fun recipe(
        value: String,
        inputs: Map<GameId, Long>,
        output: GameId,
        quantity: Long,
        energy: Long,
        heat: Long,
    ) = CryosRecipeDefinition(id(value), inputs, output, quantity, energy, heat)

    private fun technology(id: GameId, required: Set<GameId>, energy: Long, heat: Long, refined: Long) =
        CryosTechnologyDefinition(id, required, energy, heat, refined)

    private fun ids(prefix: String, count: Int): List<GameId> =
        (1..count).map { id("${prefix}_${it.toString().padStart(2, '0')}") }

    private fun id(value: String): GameId = GameId.of(value)
}
