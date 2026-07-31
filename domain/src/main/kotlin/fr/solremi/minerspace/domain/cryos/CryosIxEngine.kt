package fr.solremi.minerspace.domain.cryos

import fr.solremi.minerspace.shared.GameId

class CryosIxEngine(val definitions: CryosIxDefinitions) {
    private val initialSector = definitions.sectors.values.single { it.requiredSectorIds.isEmpty() }.id

    fun initialState(veteranRobotId: GameId?): CryosIxState = CryosIxState(
        baseInstalled = false,
        energy = 0L,
        heat = 0L,
        coldExposure = 0L,
        thermalNodes = 0,
        inventory = (definitions.resources.keys + definitions.refinedMaterialIds + componentIds())
            .associateWith { 0L },
        installedTechnologyIds = emptySet(),
        craftedModuleIds = emptySet(),
        unlockedSectorIds = emptySet(),
        completedMainMissionIds = emptySet(),
        completedSecondaryMissionIds = emptySet(),
        resolvedEventIds = emptySet(),
        narrativeDiscoveryIds = emptySet(),
        discoveredCodexEntryIds = emptySet(),
        frontierUnlocked = false,
        veteranRobotId = veteranRobotId,
        transactionSequence = 0L,
    )

    fun normalize(source: CryosIxState): CryosIxState = source.copy(
        energy = source.energy.coerceIn(0L, MAX_ENERGY),
        heat = source.heat.coerceIn(0L, MAX_HEAT),
        coldExposure = source.coldExposure.coerceAtLeast(0L),
        thermalNodes = source.thermalNodes.coerceIn(0, definitions.sectors.size - 1),
        inventory = (definitions.resources.keys + definitions.refinedMaterialIds + componentIds())
            .associateWith { (source.inventory[it] ?: 0L).coerceAtLeast(0L) },
        installedTechnologyIds = source.installedTechnologyIds.filterTo(linkedSetOf(), definitions.technologies::containsKey),
        craftedModuleIds = source.craftedModuleIds.filterTo(linkedSetOf(), definitions.modules::containsKey),
        unlockedSectorIds = source.unlockedSectorIds.filterTo(linkedSetOf(), definitions.sectors::containsKey),
        resolvedEventIds = source.resolvedEventIds.filterTo(linkedSetOf()) { it in definitions.eventIds },
        narrativeDiscoveryIds = source.narrativeDiscoveryIds.filterTo(linkedSetOf()) { it in definitions.narrativeDiscoveryIds },
        discoveredCodexEntryIds = source.discoveredCodexEntryIds.filterTo(linkedSetOf()) { it in definitions.codexEntryIds },
        transactionSequence = source.transactionSequence.coerceAtLeast(0L),
    )

    fun installBase(state: CryosIxState): CryosCommandResult {
        if (state.baseInstalled) return reject(state, "base_already_installed")
        val next = state.copy(
            baseInstalled = true,
            energy = 140L,
            heat = 180L,
            unlockedSectorIds = setOf(initialSector),
        )
        return applied(progress(next), "install_cryos_base", initialSector)
    }

    fun generateEnergy(state: CryosIxState): CryosCommandResult {
        if (!state.baseInstalled) return reject(state, "base_required")
        val bonus = if (TECH_EFFICIENT_GRID in state.installedTechnologyIds) 25L else 0L
        val next = state.copy(
            energy = (state.energy + 70L + bonus).coerceAtMost(MAX_ENERGY),
            heat = (state.heat + 8L).coerceAtMost(MAX_HEAT),
        )
        return applied(progress(next), "generate_energy", null)
    }

    fun heatBase(state: CryosIxState): CryosCommandResult {
        if (!state.baseInstalled) return reject(state, "base_required")
        if (state.energy < 20L) return reject(state, "energy_insufficient")
        val bonus = if (TECH_THERMAL_RECOVERY in state.installedTechnologyIds) 25L else 0L
        val next = state.copy(
            energy = state.energy - 20L,
            heat = (state.heat + 90L + bonus).coerceAtMost(MAX_HEAT),
            coldExposure = (state.coldExposure - 15L).coerceAtLeast(0L),
        )
        return applied(progress(next), "heat_base", null)
    }

    fun extract(state: CryosIxState, resourceId: GameId): CryosCommandResult {
        val resource = definitions.resources[resourceId] ?: return reject(state, "unknown_resource")
        if (!state.baseInstalled) return reject(state, "base_required")
        if (resource.sourceSectorId !in state.unlockedSectorIds) return reject(state, "sector_locked")
        val sector = definitions.sectors.getValue(resource.sourceSectorId)
        if (state.heat < sector.minimumHeat) return reject(state, "heat_insufficient")
        if (state.energy < EXTRACTION_ENERGY_COST) return reject(state, "energy_insufficient")
        val technologyBonus = if (TECH_CRYO_DRILL in state.installedTechnologyIds) 4L else 0L
        val moduleBonus = if (state.craftedModuleIds.size >= 2) 2L else 0L
        val quantity = 8L + technologyBonus + moduleBonus
        val inventory = state.inventory.toMutableMap()
        inventory[resourceId] = Math.addExact(inventory[resourceId] ?: 0L, quantity)
        val next = state.copy(
            energy = state.energy - EXTRACTION_ENERGY_COST,
            heat = (state.heat - sector.coldDrainPerAction).coerceAtLeast(0L),
            coldExposure = Math.addExact(state.coldExposure, sector.coldDrainPerAction),
            inventory = inventory,
            discoveredCodexEntryIds = discover(state.discoveredCodexEntryIds, resourceId),
        )
        return applied(progress(next), "extract_cryos_resource", resourceId)
    }

    fun refine(state: CryosIxState, recipeId: GameId): CryosCommandResult {
        val recipe = definitions.recipes[recipeId] ?: return reject(state, "unknown_recipe")
        if (!state.baseInstalled) return reject(state, "base_required")
        if (state.energy < recipe.energyCost) return reject(state, "energy_insufficient")
        if (state.heat < recipe.heatCost) return reject(state, "heat_insufficient")
        if (recipe.inputs.any { (id, quantity) -> (state.inventory[id] ?: 0L) < quantity }) {
            return reject(state, "materials_insufficient")
        }
        val inventory = state.inventory.toMutableMap()
        recipe.inputs.forEach { (id, quantity) -> inventory[id] = inventory.getValue(id) - quantity }
        inventory[recipe.outputId] = Math.addExact(inventory[recipe.outputId] ?: 0L, recipe.outputQuantity)
        val next = state.copy(
            energy = state.energy - recipe.energyCost,
            heat = state.heat - recipe.heatCost,
            inventory = inventory,
            discoveredCodexEntryIds = discover(state.discoveredCodexEntryIds, recipe.outputId),
        )
        return applied(progress(next), "refine_cryos_material", recipeId)
    }

    fun buildThermalNode(state: CryosIxState): CryosCommandResult {
        if (!state.baseInstalled) return reject(state, "base_required")
        if (state.thermalNodes >= definitions.sectors.size - 1) return reject(state, "thermal_network_complete")
        val cost = 2L + state.thermalNodes
        if (state.energy < 45L || state.heat < 90L) return reject(state, "thermal_power_insufficient")
        if ((state.inventory[REFINED_CRYONITE] ?: 0L) < cost || (state.inventory[REFINED_GLASS] ?: 0L) < cost) {
            return reject(state, "thermal_materials_insufficient")
        }
        val inventory = state.inventory.toMutableMap()
        inventory[REFINED_CRYONITE] = inventory.getValue(REFINED_CRYONITE) - cost
        inventory[REFINED_GLASS] = inventory.getValue(REFINED_GLASS) - cost
        val next = state.copy(
            energy = state.energy - 45L,
            heat = state.heat - 30L,
            thermalNodes = state.thermalNodes + 1,
            inventory = inventory,
        )
        return applied(progress(next), "build_thermal_node", GameId.of("thermal_node_${state.thermalNodes + 1}"))
    }

    fun unlockNextSector(state: CryosIxState): CryosCommandResult {
        val sector = definitions.sectors.values
            .filter { it.id !in state.unlockedSectorIds }
            .firstOrNull { candidate ->
                state.unlockedSectorIds.containsAll(candidate.requiredSectorIds) &&
                    state.thermalNodes >= candidate.requiredThermalNodes
            } ?: return reject(state, "no_sector_available")
        if (state.heat < sector.minimumHeat) return reject(state, "heat_insufficient")
        if (state.energy < 35L) return reject(state, "energy_insufficient")
        val next = state.copy(
            energy = state.energy - 35L,
            heat = (state.heat - sector.coldDrainPerAction).coerceAtLeast(0L),
            unlockedSectorIds = state.unlockedSectorIds + sector.id,
            discoveredCodexEntryIds = discover(state.discoveredCodexEntryIds, sector.id),
        )
        return applied(progress(next), "unlock_cryos_sector", sector.id)
    }

    fun installNextTechnology(state: CryosIxState): CryosCommandResult {
        val technology = definitions.technologies.values.firstOrNull { candidate ->
            candidate.id !in state.installedTechnologyIds &&
                state.installedTechnologyIds.containsAll(candidate.requiredTechnologyIds)
        } ?: return reject(state, "technology_tree_complete")
        if (state.energy < technology.energyCost || state.heat < technology.heatCost) {
            return reject(state, "technology_power_insufficient")
        }
        val availableRefined = definitions.refinedMaterialIds.sumOf { state.inventory[it] ?: 0L }
        if (availableRefined < technology.refinedMaterialCost) return reject(state, "technology_materials_insufficient")
        val inventory = consumeRefined(state.inventory, technology.refinedMaterialCost)
        val next = state.copy(
            energy = state.energy - technology.energyCost,
            heat = state.heat - technology.heatCost,
            inventory = inventory,
            installedTechnologyIds = state.installedTechnologyIds + technology.id,
            discoveredCodexEntryIds = discover(state.discoveredCodexEntryIds, technology.id),
        )
        return applied(progress(next), "install_cryos_technology", technology.id)
    }

    fun craftNextModule(state: CryosIxState): CryosCommandResult {
        val module = definitions.modules.values.firstOrNull { candidate ->
            candidate.id !in state.craftedModuleIds &&
                state.installedTechnologyIds.containsAll(candidate.requiredTechnologyIds)
        } ?: return reject(state, "module_catalog_complete_or_locked")
        if (state.energy < module.energyCost) return reject(state, "energy_insufficient")
        val availableRefined = definitions.refinedMaterialIds.sumOf { state.inventory[it] ?: 0L }
        if (availableRefined < module.refinedMaterialCost) return reject(state, "module_materials_insufficient")
        val inventory = consumeRefined(state.inventory, module.refinedMaterialCost)
        val next = state.copy(
            energy = state.energy - module.energyCost,
            inventory = inventory,
            craftedModuleIds = state.craftedModuleIds + module.id,
            discoveredCodexEntryIds = discover(state.discoveredCodexEntryIds, module.id),
        )
        return applied(progress(next), "craft_cryogenic_module", module.id)
    }

    fun resolveNextEvent(state: CryosIxState): CryosCommandResult {
        val event = definitions.eventIds.firstOrNull { it !in state.resolvedEventIds }
            ?: return reject(state, "events_complete")
        val requiredSectors = definitions.eventIds.indexOf(event) + 2
        if (state.unlockedSectorIds.size < requiredSectors) return reject(state, "event_sector_locked")
        val next = state.copy(
            energy = (state.energy + 45L).coerceAtMost(MAX_ENERGY),
            heat = (state.heat + 35L).coerceAtMost(MAX_HEAT),
            resolvedEventIds = state.resolvedEventIds + event,
            discoveredCodexEntryIds = discover(state.discoveredCodexEntryIds, event),
        )
        return applied(progress(next), "resolve_cryos_event", event)
    }

    fun completePlanetaryObjective(state: CryosIxState): CryosCommandResult {
        if (state.frontierUnlocked) return reject(state, "frontier_already_unlocked")
        if (state.unlockedSectorIds.size < definitions.sectors.size) return reject(state, "all_sectors_required")
        if (state.thermalNodes < definitions.sectors.size - 1) return reject(state, "thermal_network_incomplete")
        if (state.installedTechnologyIds.size < 3) return reject(state, "technologies_required")
        if (state.craftedModuleIds.isEmpty()) return reject(state, "cryogenic_module_required")
        val next = state.copy(
            frontierUnlocked = true,
            narrativeDiscoveryIds = definitions.narrativeDiscoveryIds.toSet(),
            discoveredCodexEntryIds = state.discoveredCodexEntryIds + definitions.narrativeDiscoveryIds,
        )
        return applied(progress(next), "complete_cryos_major_objective", GameId.of("objective_cryos_frontier"))
    }

    companion object {
        const val MAX_ENERGY = 500L
        const val MAX_HEAT = 500L
        const val EXTRACTION_ENERGY_COST = 12L
        val REFINED_CRYONITE: GameId = GameId.of("refined_cryonite_plate")
        val REFINED_GLASS: GameId = GameId.of("refined_thermal_glass")
        val TECH_EFFICIENT_GRID: GameId = GameId.of("tech_cryos_efficient_grid")
        val TECH_THERMAL_RECOVERY: GameId = GameId.of("tech_cryos_thermal_recovery")
        val TECH_CRYO_DRILL: GameId = GameId.of("tech_cryos_deep_drill")
    }
}
