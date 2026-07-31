package fr.solremi.minerspace.data.content

import fr.solremi.minerspace.domain.content.*

object FerrumDeltaContentFactory {
    fun create(): FerrumDeltaContent {
        val raw = listOf("iron", "copper", "crystal", "nickel", "titanium", "silica", "cobalt", "iridium", "xenon_ore")
        val refined = listOf("iron_ingot", "copper_plate", "crystal_lens", "nickel_alloy", "titanium_beam", "silica_glass", "cobalt_coil", "iridium_mesh", "xenon_matrix")
        val components = listOf(
            "power_cell", "sensor_array", "servo_core", "heat_sink", "logic_board", "magnetic_coupler",
            "pressure_valve", "optic_bus", "titanium_frame", "storage_matrix", "drone_chassis", "reactor_shell",
            "navigation_core", "quantum_relay", "archive_decoder", "plasma_injector", "gravity_anchor",
            "fusion_lattice", "launch_clamp", "orbital_beacon", "stellar_compressor", "nova_resonator",
            "transfer_core", "interplanetary_bus",
        )
        val technologyIds = listOf(
            "tech_extraction_protocol", "tech_quantum_sorting", "tech_logistics_mesh", "tech_deep_scanner",
            "tech_thermal_recovery", "tech_robot_parallelism", "tech_storage_lattice", "tech_crystal_analysis",
            "tech_iridium_forging", "tech_xenon_stabilization", "tech_archive_decoding", "tech_orbital_assembly",
            "tech_nova_navigation", "tech_interplanetary_launch",
        )
        val sectorIds = listOf(
            "sector_core_delta", "sector_copper_ridge", "sector_crystal_flats", "sector_logistics_pass",
            "sector_nickel_basin", "sector_silica_dunes", "sector_cobalt_trench", "sector_titanium_shelf",
            "sector_iridium_fault", "sector_xenon_depths", "sector_archive_ruins", "sector_quantum_abyss",
            "sector_nova_vault", "sector_launch_shipyard",
        )
        val sectorDays = listOf(1, 2, 3, 5, 7, 9, 12, 15, 19, 23, 27, 29, 31, 32)
        val sectorReasons = listOf(
            "Base et première chaîne", "Plaques conductrices", "Optique cristalline", "Raccourci logistique",
            "Alliages robustes", "Verre industriel", "Bobines et moteurs", "Structures avancées",
            "Blindages d’iridium", "Xénon garanti", "Archives garanties", "Technologies profondes",
            "Navigation NOVA", "Départ interplanétaire",
        )

        val resources = buildList {
            raw.forEach { add(CatalogResource("raw_$it", ResourceCategory.RAW, true, "deposit_${it}_01")) }
            refined.forEach { add(CatalogResource("refined_$it", ResourceCategory.REFINED, true, "recipe_$it")) }
            components.forEachIndexed { index, id ->
                add(CatalogResource("component_$id", ResourceCategory.COMPONENT, index < 18, "assembly_$id"))
            }
            add(CatalogResource("rare_prismatic_ferrite", ResourceCategory.RARE, false, "mastery_collection_01"))
            add(CatalogResource("rare_xenon_crystal", ResourceCategory.RARE, false, "sector_xenon_depths"))
            add(CatalogResource("rare_archive_fragment", ResourceCategory.RARE, false, "sector_archive_ruins"))
            add(CatalogResource("rare_meteor_core", ResourceCategory.RARE, false, "event_meteor_1"))
            add(CatalogResource("rare_nova_core", ResourceCategory.RARE, true, "main_42"))
        }
        val recipes = buildList {
            raw.indices.forEach { index ->
                add(CatalogRecipe("recipe_${refined[index]}", RecipeKind.REFINING, setOf("raw_${raw[index]}"), "refined_${refined[index]}"))
            }
            components.forEachIndexed { index, id ->
                add(CatalogRecipe(
                    "assembly_$id",
                    RecipeKind.COMPONENT,
                    setOf("refined_${refined[index % 9]}", "refined_${refined[(index + 3) % 9]}"),
                    "component_$id",
                ))
            }
            technologyIds.forEachIndexed { index, id ->
                add(CatalogRecipe(
                    "assembly_${id.removePrefix("tech_")}",
                    RecipeKind.TECHNOLOGY,
                    setOf("component_${components[index * 2 % 24]}", "component_${components[(index * 2 + 1) % 24]}"),
                    "${id}_item",
                ))
            }
            add(CatalogRecipe(
                "assembly_interplanetary_departure_array",
                RecipeKind.FINAL,
                setOf("component_stellar_compressor", "component_nova_resonator", "component_transfer_core", "component_interplanetary_bus", "rare_nova_core"),
                "final_interplanetary_departure_array",
            ))
        }
        val technologies = technologyIds.mapIndexed { index, id ->
            CatalogTechnology(id, if (index == 0) emptySet() else setOf(technologyIds[index - 1]), 2 + index * 2)
        }
        val modules = MODULE_NAMES.mapIndexed { index, id ->
            val rarity = when {
                index < 10 -> ModuleRarity.STANDARD
                index < 18 -> ModuleRarity.IMPROVED
                index < 22 -> ModuleRarity.ADVANCED
                else -> ModuleRarity.EXCEPTIONAL
            }
            val setId = when (index % 3) { 0 -> "FORGE"; 1 -> "SURVEY"; else -> "NONE" }
            CatalogModule("module_$id", rarity, setId)
        }
        val sectors = sectorIds.mapIndexed { index, id ->
            CatalogSector(
                id = id,
                tier = when (index) {
                    0 -> SectorTier.INITIAL
                    in 1..10 -> SectorTier.STANDARD
                    in 11..12 -> SectorTier.DEEP
                    else -> SectorTier.FINAL
                },
                requiredSectorIds = if (index == 0) emptySet() else setOf(sectorIds[index - 1]),
                targetDayRegular = sectorDays[index],
                strategicNovelty = sectorReasons[index],
            )
        }
        val depositCounts = listOf(4, 4, 4, 4, 4, 4, 3, 3, 4)
        val deposits = buildList {
            var cursor = 0
            raw.forEachIndexed { resourceIndex, resource ->
                repeat(depositCounts[resourceIndex]) { depositIndex ->
                    add(CatalogDeposit(
                        id = "deposit_${resource}_${(depositIndex + 1).toString().padStart(2, '0')}",
                        resourceId = "raw_$resource",
                        sectorId = sectorIds[cursor++ % sectorIds.size],
                        guaranteed = depositIndex == 0,
                    ))
                }
            }
        }
        val buildings = BUILDINGS.mapIndexed { index, id ->
            CatalogBuilding("building_$id", if (index < 9) "INITIAL_INTERMEDIATE" else "ADVANCED_FINAL", 3 + index % 3)
        }
        val robots = mapOf(
            "EXTRACTOR" to setOf("FOREUR", "PROSPECTEUR"),
            "REFINER" to setOf("METALLURGISTE", "THERMICIEN"),
            "ASSEMBLER" to setOf("MICROFABRICANT", "ARCHITECTE"),
            "LOGISTICS" to setOf("CONVOYEUR", "REGULATEUR"),
        ).map { (family, specializations) -> CatalogRobot(family, 5, specializations) }

        val missions = buildList {
            var previous: String? = null
            repeat(42) { index ->
                val id = "main_${(index + 1).toString().padStart(2, '0')}"
                add(CatalogMission(id, MissionGroup.MAIN, previous?.let(::setOf).orEmpty(), 150L + index * 75L))
                previous = id
            }
            repeat(36) { index ->
                add(CatalogMission(
                    "secondary_${(index + 1).toString().padStart(2, '0')}",
                    MissionGroup.SECONDARY,
                    setOf("main_${(index.coerceAtMost(41) + 1).toString().padStart(2, '0')}"),
                    200L + index * 45L,
                ))
            }
            repeat(20) { index ->
                add(CatalogMission(
                    "mastery_collection_${(index + 1).toString().padStart(2, '0')}",
                    MissionGroup.MASTERY_COLLECTION,
                    setOf("main_${(index + 6).toString().padStart(2, '0')}"),
                    350L + index * 60L,
                ))
            }
        }
        val achievements = List(8) { index ->
            CatalogAchievement("achievement_${index + 1}", setOf("main_${(10 + index * 4).coerceAtMost(42).toString().padStart(2, '0')}"))
        }
        val contracts = List(12) { index ->
            CatalogContract(
                "contract_template_${(index + 1).toString().padStart(2, '0')}",
                ContractTier.entries[index % 3],
                listOf(ResourceCategory.RAW, ResourceCategory.REFINED, ResourceCategory.COMPONENT)[index % 3],
            )
        }
        val events = buildList {
            repeat(4) { add(CatalogEvent("event_meteor_${it + 1}", "METEOR_SHOWER", false)) }
            repeat(2) { add(CatalogEvent("event_crystal_anomaly_${it + 1}", "CRYSTAL_ANOMALY", false)) }
            repeat(2) { add(CatalogEvent("event_abandoned_robot_${it + 1}", "ABANDONED_ROBOT", false)) }
            repeat(2) { add(CatalogEvent("event_non_punitive_failure_${it + 1}", "NON_PUNITIVE_FAILURE", false)) }
            add(CatalogEvent("event_orbital_merchant", "ORBITAL_MERCHANT", false))
            add(CatalogEvent("event_temporary_exceptional_deposit", "TEMPORARY_DEPOSIT", false))
        }
        val collections = List(10) { collectionIndex ->
            val first = collectionIndex * 12 + 1
            CatalogCollection(
                "collection_${(collectionIndex + 1).toString().padStart(2, '0')}",
                (first until first + 12).mapTo(linkedSetOf()) { "codex_ferrum_${it.toString().padStart(3, '0')}" },
            )
        }
        val codex = List(120) { index ->
            CatalogCodexEntry(
                id = "codex_ferrum_${(index + 1).toString().padStart(3, '0')}",
                category = listOf("RESOURCE", "INDUSTRY", "EXPLORATION", "ROBOT", "STRATEGY")[index % 5],
                analysisLevels = if (index < 15) 3 else 1,
                collectionId = collections[index / 12].id,
            )
        }
        val transmissions = List(12) { CatalogTransmission("nova_transmission_${(it + 1).toString().padStart(2, '0')}") }
        val milestones = listOf(
            NarrativeMilestone("narrative_milestone_1", transmissions.subList(0, 3).mapTo(linkedSetOf()) { it.id }),
            NarrativeMilestone("narrative_milestone_2", transmissions.subList(3, 5).mapTo(linkedSetOf()) { it.id }),
            NarrativeMilestone("narrative_milestone_3", transmissions.subList(5, 7).mapTo(linkedSetOf()) { it.id }),
            NarrativeMilestone("narrative_milestone_4", transmissions.subList(7, 9).mapTo(linkedSetOf()) { it.id }),
            NarrativeMilestone("narrative_milestone_5", transmissions.subList(9, 12).mapTo(linkedSetOf()) { it.id }),
        )

        return FerrumDeltaContent(
            schemaVersion = 1,
            contentVersion = "1.0.0",
            completionProgressPoints = 32_000,
            timing = CampaignTiming(25, 165, 540, 1_140, 3_300),
            resources = resources,
            recipes = recipes,
            technologies = technologies,
            modules = modules,
            sectors = sectors,
            deposits = deposits,
            buildings = buildings,
            robots = robots,
            traits = setOf("PRECISE", "ENDURING", "FAST", "STABLE", "PROSPECTOR"),
            masteryTiers = setOf("NOVICE", "EXPERIENCED", "EXPERT", "VETERAN"),
            missions = missions,
            achievements = achievements,
            contracts = contracts,
            events = events,
            codexEntries = codex,
            collections = collections,
            narrativeMilestones = milestones,
            transmissions = transmissions,
            playerProfiles = listOf(
                PlayerProfileDefinition("VERY_ACTIVE", 1_455, 25, 18, 25),
                PlayerProfileDefinition("REGULAR", 1_000, 20, 25, 40),
                PlayerProfileDefinition("CASUAL", 640, 15, 40, 60),
            ),
        ).also(FerrumDeltaContentValidator()::requireValid)
    }

    private val MODULE_NAMES = listOf(
        "forge_drill", "forge_thermal", "forge_chassis", "survey_optics", "survey_quantum", "survey_archive",
        "storage_capsule", "compact_battery", "servo_accelerator", "magnetic_sifter", "thermal_regulator",
        "optic_predictor", "cargo_weave", "crystal_resonator", "titanium_bracing", "cobalt_driver",
        "iridium_filter", "xenon_compass", "quantum_harvester", "archive_coprocessor", "gravity_stabilizer",
        "orbital_matrix", "nova_heart", "stellar_crown",
    )

    private val BUILDINGS = listOf(
        "base_delta", "refinery", "assembler", "robot_bay", "storage_hub", "scanner_array", "logistics_port",
        "research_lab", "meteor_lab", "deep_foundry", "quantum_foundry", "archive_vault", "orbital_beacon",
        "launch_shipyard", "interplanetary_array",
    )
}
