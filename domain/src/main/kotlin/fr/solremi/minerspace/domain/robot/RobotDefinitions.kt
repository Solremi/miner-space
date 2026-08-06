package fr.solremi.minerspace.domain.robot

import fr.solremi.minerspace.shared.GameId

enum class RobotFamily { EXTRACTOR, REFINER, ASSEMBLER, LOGISTICS }
enum class RobotTrait { PRECISE, ENDURING, FAST, STABLE, PROSPECTOR }
enum class MasteryTier { NOVICE, EXPERIENCED, EXPERT, VETERAN }
enum class AutomationPriority { BALANCED, MISSION, STORAGE_RELIEF, RARE_RESOURCE, PROFIT }
enum class RenderQuality { LOW, MEDIUM, HIGH }

data class RobotFamilyDefinition(
    val family: RobotFamily,
    val nameKey: String,
    val defaultName: String,
    val serialPrefix: String,
    val defaultTrait: RobotTrait,
    val maxLevel: Int,
    val baseLogisticsPerSecond: Long,
    val upgradeCostsSpaceDollars: List<Long>,
) {
    init {
        require(nameKey.isNotBlank())
        require(defaultName.isNotBlank())
        require(serialPrefix.matches(Regex("[A-Z0-9-]{2,12}")))
        require(maxLevel in 1..10)
        require(baseLogisticsPerSecond >= 0L)
        require(upgradeCostsSpaceDollars.size == maxLevel)
        require(upgradeCostsSpaceDollars.first() == 0L)
        require(upgradeCostsSpaceDollars.zipWithNext().all { (left, right) -> right >= left })
    }
}

data class RobotDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val families: Map<RobotFamily, RobotFamilyDefinition>,
    val masteryThresholds: Map<MasteryTier, Long>,
    val visibleUnitsByQuality: Map<RenderQuality, Int>,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(families.keys == RobotFamily.entries.toSet())
        require(masteryThresholds.keys == MasteryTier.entries.toSet())
        require(masteryThresholds.getValue(MasteryTier.NOVICE) == 0L)
        val thresholds = MasteryTier.entries.map(masteryThresholds::getValue)
        require(thresholds.zipWithNext().all { (left, right) -> right > left })
        require(visibleUnitsByQuality.keys == RenderQuality.entries.toSet())
        require(visibleUnitsByQuality.values.all { it in 1..50 })
        require(visibleUnitsByQuality.getValue(RenderQuality.HIGH) == 50)
    }
}
