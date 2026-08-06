package fr.solremi.minerspace.domain.content

class FerrumDeltaContentValidator {
    fun validationErrors(content: FerrumDeltaContent): List<String> {
        val errors = mutableListOf<String>()
        fun check(value: Boolean, code: String) { if (!value) errors += code }
        fun <T> unique(values: List<T>, id: (T) -> String, code: String) {
            check(values.map(id).distinct().size == values.size, code)
        }

        check(content.schemaVersion > 0, "schema_version")
        check(content.contentVersion == "1.0.0", "content_version")
        check(content.completionProgressPoints > 0, "completion_points")
        check(content.timing.firstExtractionSeconds <= 30, "first_extraction_too_late")
        check(content.timing.firstRefiningSeconds <= 180, "first_refining_too_late")
        check(content.timing.firstRobotSeconds <= 600, "first_robot_too_late")
        check(content.timing.firstSectorSeconds <= 1_200, "first_sector_too_late")
        check(content.timing.firstVisualTransformationSeconds <= 3_600, "first_visual_change_too_late")

        unique(content.resources, CatalogResource::id, "duplicate_resource")
        unique(content.recipes, CatalogRecipe::id, "duplicate_recipe")
        unique(content.technologies, CatalogTechnology::id, "duplicate_technology")
        unique(content.modules, CatalogModule::id, "duplicate_module")
        unique(content.sectors, CatalogSector::id, "duplicate_sector")
        unique(content.deposits, CatalogDeposit::id, "duplicate_deposit")
        unique(content.buildings, CatalogBuilding::id, "duplicate_building")
        unique(content.robots, CatalogRobot::family, "duplicate_robot_family")
        unique(content.missions, CatalogMission::id, "duplicate_mission")
        unique(content.achievements, CatalogAchievement::id, "duplicate_achievement")
        unique(content.contracts, CatalogContract::id, "duplicate_contract")
        unique(content.events, CatalogEvent::id, "duplicate_event")
        unique(content.codexEntries, CatalogCodexEntry::id, "duplicate_codex")
        unique(content.collections, CatalogCollection::id, "duplicate_collection")
        unique(content.narrativeMilestones, NarrativeMilestone::id, "duplicate_narrative_milestone")
        unique(content.transmissions, CatalogTransmission::id, "duplicate_transmission")
        unique(content.playerProfiles, PlayerProfileDefinition::id, "duplicate_player_profile")

        val resourceCounts = content.resources.groupingBy { it.category }.eachCount()
        check(resourceCounts[ResourceCategory.RAW] == 9, "raw_resource_budget")
        check(resourceCounts[ResourceCategory.REFINED] == 9, "refined_resource_budget")
        check(resourceCounts[ResourceCategory.COMPONENT] == 24, "component_budget")
        check(resourceCounts[ResourceCategory.RARE] == 5, "rare_resource_budget")

        val recipeCounts = content.recipes.groupingBy { it.kind }.eachCount()
        check(recipeCounts[RecipeKind.REFINING] == 9, "refining_recipe_budget")
        check(recipeCounts[RecipeKind.COMPONENT] == 24, "component_recipe_budget")
        check(recipeCounts[RecipeKind.TECHNOLOGY] == 14, "technology_recipe_budget")
        check(recipeCounts[RecipeKind.FINAL] == 1, "final_recipe_budget")
        check(content.technologies.size == 14, "technology_budget")

        val moduleCounts = content.modules.groupingBy { it.rarity }.eachCount()
        check(content.modules.size == 24, "module_budget")
        check(moduleCounts[ModuleRarity.STANDARD] == 10, "standard_module_budget")
        check(moduleCounts[ModuleRarity.IMPROVED] == 8, "improved_module_budget")
        check(moduleCounts[ModuleRarity.ADVANCED] == 4, "advanced_module_budget")
        check(moduleCounts[ModuleRarity.EXCEPTIONAL] == 2, "exceptional_module_budget")
        check(content.modules.count { it.setId == "FORGE" } >= 3, "forge_set_incomplete")
        check(content.modules.count { it.setId == "SURVEY" } >= 3, "survey_set_incomplete")

        val sectorCounts = content.sectors.groupingBy { it.tier }.eachCount()
        check(content.sectors.size == 14, "sector_budget")
        check(sectorCounts[SectorTier.INITIAL] == 1, "initial_sector_budget")
        check(sectorCounts[SectorTier.STANDARD] == 10, "standard_sector_budget")
        check(sectorCounts[SectorTier.DEEP] == 2, "deep_sector_budget")
        check(sectorCounts[SectorTier.FINAL] == 1, "final_sector_budget")
        check(content.sectors.all { it.strategicNovelty.isNotBlank() }, "sector_novelty_missing")
        check(content.deposits.size in 30..36, "deposit_budget")

        val rawIds = content.resources
            .filter { it.category == ResourceCategory.RAW }
            .mapTo(linkedSetOf(), CatalogResource::id)
        rawIds.forEach { rawId ->
            check(content.deposits.count { it.resourceId == rawId } >= 2, "deposit_source_missing:$rawId")
            check(content.deposits.any { it.resourceId == rawId && it.guaranteed }, "guaranteed_deposit_missing:$rawId")
        }

        check(content.robots.size == 4, "robot_family_budget")
        check(content.robots.all { it.levels == 5 }, "robot_level_budget")
        check(content.robots.all { it.specializations.size == 2 }, "robot_specialization_budget")
        check(content.traits.size >= 5, "robot_trait_budget")
        check(content.masteryTiers.size == 4, "mastery_tier_budget")
        check(content.buildings.count { it.tier == "INITIAL_INTERMEDIATE" } == 9, "initial_building_budget")
        check(content.buildings.count { it.tier == "ADVANCED_FINAL" } == 6, "advanced_building_budget")
        check(content.buildings.all { it.visualTiers in 3..5 }, "building_visual_tiers")

        val missionCounts = content.missions.groupingBy { it.group }.eachCount()
        check(missionCounts[MissionGroup.MAIN] == 42, "main_mission_budget")
        check(missionCounts[MissionGroup.SECONDARY] == 36, "secondary_mission_budget")
        check(missionCounts[MissionGroup.MASTERY_COLLECTION] == 20, "mastery_collection_budget")
        check(content.achievements.size == 8, "achievement_budget")
        check(content.contracts.size == 12, "contract_budget")
        check(content.contracts.groupBy { it.tier }.keys == ContractTier.entries.toSet(), "contract_tiers")

        val eventCounts = content.events.groupingBy { it.kind }.eachCount()
        check(eventCounts["METEOR_SHOWER"] == 4, "meteor_event_budget")
        check(eventCounts["CRYSTAL_ANOMALY"] == 2, "crystal_event_budget")
        check(eventCounts["ABANDONED_ROBOT"] == 2, "abandoned_robot_event_budget")
        check(eventCounts["NON_PUNITIVE_FAILURE"] == 2, "failure_event_budget")
        check(eventCounts["ORBITAL_MERCHANT"] == 1, "merchant_event_budget")
        check(eventCounts["TEMPORARY_DEPOSIT"] == 1, "temporary_deposit_event_budget")
        check(content.events.none { it.mandatory }, "mandatory_event")

        check(content.codexEntries.size in 100..130, "codex_budget")
        check(content.codexEntries.count { it.analysisLevels > 1 } == 15, "multilevel_codex_budget")
        check(content.collections.size == 10, "collection_budget")
        check(content.narrativeMilestones.size == 5, "narrative_milestone_budget")
        check(content.transmissions.size == 12, "transmission_budget")

        val sectorIds = content.sectors.mapTo(linkedSetOf(), CatalogSector::id)
        content.deposits.forEach {
            check(it.resourceId in rawIds, "deposit_unknown_resource:${it.id}")
            check(it.sectorId in sectorIds, "deposit_unknown_sector:${it.id}")
        }

        val resourceIds = content.resources.mapTo(linkedSetOf(), CatalogResource::id)
        content.recipes.forEach { recipe ->
            check(recipe.inputs.all(resourceIds::contains), "recipe_unknown_input:${recipe.id}")
        }
        val technologyIds = content.technologies.mapTo(linkedSetOf(), CatalogTechnology::id)
        content.technologies.forEach {
            check(it.requiredTechnologyIds.all(technologyIds::contains), "technology_unknown_requirement:${it.id}")
        }
        content.sectors.forEach {
            check(it.requiredSectorIds.all(sectorIds::contains), "sector_unknown_requirement:${it.id}")
        }
        val missionIds = content.missions.mapTo(linkedSetOf(), CatalogMission::id)
        content.missions.forEach {
            check(it.requiredMissionIds.all(missionIds::contains), "mission_unknown_requirement:${it.id}")
        }
        content.achievements.forEach {
            check(it.requiredMissionIds.all(missionIds::contains), "achievement_unknown_requirement:${it.id}")
        }

        val collectionIds = content.collections.mapTo(linkedSetOf(), CatalogCollection::id)
        val codexIds = content.codexEntries.mapTo(linkedSetOf(), CatalogCodexEntry::id)
        content.codexEntries.forEach {
            check(it.collectionId in collectionIds, "codex_unknown_collection:${it.id}")
        }
        content.collections.forEach {
            check(it.entryIds.all(codexIds::contains), "collection_unknown_entry:${it.id}")
        }

        val transmissionIds = content.transmissions.mapTo(linkedSetOf(), CatalogTransmission::id)
        content.narrativeMilestones.forEach {
            check(it.transmissionIds.isNotEmpty(), "empty_narrative_milestone:${it.id}")
            check(it.transmissionIds.all(transmissionIds::contains), "unknown_transmission:${it.id}")
        }
        check(
            content.narrativeMilestones.flatMap { it.transmissionIds }.toSet() == transmissionIds,
            "transmission_not_archived",
        )

        val sourceIds = buildSet {
            addAll(content.deposits.map(CatalogDeposit::id))
            addAll(content.recipes.map(CatalogRecipe::id))
            addAll(content.sectors.map(CatalogSector::id))
            addAll(content.missions.map(CatalogMission::id))
            addAll(content.events.map(CatalogEvent::id))
        }
        content.resources.filter(CatalogResource::mandatory).forEach {
            check(it.guaranteedSourceId in sourceIds, "mandatory_resource_not_guaranteed:${it.id}")
        }

        check(isAcyclic(content.sectors.associate { it.id to it.requiredSectorIds }), "sector_dependency_cycle")
        check(isAcyclic(content.technologies.associate { it.id to it.requiredTechnologyIds }), "technology_dependency_cycle")
        check(isAcyclic(content.missions.associate { it.id to it.requiredMissionIds }), "mission_dependency_cycle")

        val profiles = content.playerProfiles.associateBy { it.id }
        check(profiles.keys == setOf("VERY_ACTIVE", "REGULAR", "CASUAL"), "player_profile_budget")
        content.playerProfiles.forEach {
            check(it.dailyProgressPoints > 0, "profile_daily_progress:${it.id}")
            check(it.advertisingBonusPercent in 0..30, "profile_ad_bonus:${it.id}")
            check(
                it.expectedMinDays > 0 && it.expectedMaxDays >= it.expectedMinDays,
                "profile_day_range:${it.id}",
            )
        }
        return errors
    }

    fun requireValid(content: FerrumDeltaContent) {
        val errors = validationErrors(content)
        require(errors.isEmpty()) { errors.joinToString() }
    }

    private fun isAcyclic(graph: Map<String, Set<String>>): Boolean {
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String): Boolean {
            if (id in visited) return true
            if (!visiting.add(id)) return false
            val valid = graph[id].orEmpty().all(::visit)
            visiting.remove(id)
            visited += id
            return valid
        }
        return graph.keys.all(::visit)
    }
}
