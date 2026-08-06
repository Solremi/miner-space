package fr.solremi.minerspace.game.ferrum.model

import fr.solremi.minerspace.shared.GameId

object FerrumIds {
    val RAW_IRON = GameId.of("raw_iron")
    val RAW_COPPER = GameId.of("raw_copper")
    val RAW_CRYSTAL = GameId.of("raw_crystal")
    val REFINED_IRON = GameId.of("refined_iron_ingot")
    val REFINED_COPPER = GameId.of("refined_copper_plate")
    val POWER_CELL = GameId.of("component_power_cell")
    val SENSOR_ARRAY = GameId.of("component_sensor_array")
    val TECH_EXTRACTION_ITEM = GameId.of("tech_extraction_protocol_item")
    val TECH_EXTRACTION = GameId.of("tech_extraction_protocol")

    val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
    val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
    val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")

    val RECIPE_IRON = GameId.of("recipe_iron_ingot")
    val RECIPE_COPPER = GameId.of("recipe_copper_plate")

    val ASSEMBLY_POWER_CELL = GameId.of("assembly_power_cell")
    val ASSEMBLY_SENSOR_ARRAY = GameId.of("assembly_sensor_array")
    val ASSEMBLY_TECH_EXTRACTION = GameId.of("assembly_tech_extraction_protocol")
    val ASSEMBLY_TECH_SORTING = GameId.of("assembly_tech_quantum_sorting")

    val REFINING_RECIPES = listOf(RECIPE_IRON, RECIPE_COPPER)
    val ASSEMBLY_RECIPES = listOf(
        ASSEMBLY_POWER_CELL,
        ASSEMBLY_SENSOR_ARRAY,
        ASSEMBLY_TECH_EXTRACTION,
        ASSEMBLY_TECH_SORTING,
    )
}
