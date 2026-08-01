package fr.solremi.minerspace.game.text

import fr.solremi.minerspace.shared.text.FrenchGameText
import fr.solremi.minerspace.shared.text.GameTextKey

object GameplayText {
    fun manufacturingSuccess(reason: String): String = FrenchGameText.text(
        when {
            reason == "sell_all" -> GameTextKey.MANUFACTURING_STOCK_SOLD
            reason == "collect" -> GameTextKey.MANUFACTURING_COLLECTION_TRANSFERRED
            reason == "launch_refining" -> GameTextKey.MANUFACTURING_REFINING_STARTED
            reason == "collect_refining" -> GameTextKey.MANUFACTURING_REFINING_COLLECTED
            reason.startsWith("cancel_refining") -> GameTextKey.MANUFACTURING_REFINING_CANCELLED
            reason == "launch_assembly" -> GameTextKey.MANUFACTURING_ASSEMBLY_STARTED
            reason == "collect_assembly" -> GameTextKey.MANUFACTURING_ASSEMBLY_COLLECTED
            reason == "install_technology" -> GameTextKey.MANUFACTURING_TECHNOLOGY_INSTALLED
            else -> GameTextKey.ACTION_SAVED
        },
    )

    fun manufacturingError(code: String): String = FrenchGameText.text(
        when {
            code == "technology_prerequisite_missing" -> GameTextKey.TECHNOLOGY_LOCKED
            code == "technology_item_missing" -> GameTextKey.TECHNOLOGY_ITEM_REQUIRED
            code == "output_storage_full" -> GameTextKey.STORAGE_FULL
            code.startsWith("missing_input") -> GameTextKey.MATERIALS_INSUFFICIENT
            else -> return code
        },
    )

    fun explorationError(code: String): String = FrenchGameText.text(
        when (code) {
            "scanner_level_low" -> GameTextKey.EXPLORATION_SCANNER_LOW
            "sector_path_locked" -> GameTextKey.EXPLORATION_PATH_LOCKED
            "technology_prerequisite_missing" -> GameTextKey.EXPLORATION_TECHNOLOGY_REQUIRED
            "insufficient_space_dollars" -> GameTextKey.EXPLORATION_MONEY_LOW
            "missing_sector_component" -> GameTextKey.EXPLORATION_COMPONENTS_LOW
            "sector_not_scanned" -> GameTextKey.EXPLORATION_SCAN_REQUIRED
            else -> return code
        },
    )

    fun sectorOpened(reason: String): String = FrenchGameText.text(
        GameTextKey.EXPLORATION_SECTOR_OPENED,
        mapOf("reason" to reason),
    )

    fun meteorError(code: String): String = FrenchGameText.text(
        when (code) {
            "meteor_reward_storage_full" -> GameTextKey.METEOR_REWARD_STORAGE_FULL
            else -> return code
        },
    )
}
