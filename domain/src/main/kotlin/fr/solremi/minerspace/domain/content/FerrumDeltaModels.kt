package fr.solremi.minerspace.domain.content

enum class ResourceCategory { RAW, REFINED, COMPONENT, RARE }
enum class RecipeKind { REFINING, COMPONENT, TECHNOLOGY, FINAL }
enum class SectorTier { INITIAL, STANDARD, DEEP, FINAL }
enum class ModuleRarity { STANDARD, IMPROVED, ADVANCED, EXCEPTIONAL }
enum class MissionGroup { MAIN, SECONDARY, MASTERY_COLLECTION }
enum class ContractTier { SIMPLE, PROFITABLE, AMBITIOUS }

data class CampaignTiming(
    val firstExtractionSeconds: Int,
    val firstRefiningSeconds: Int,
    val firstRobotSeconds: Int,
    val firstSectorSeconds: Int,
    val firstVisualTransformationSeconds: Int,
)

data class CatalogResource(val id: String, val category: ResourceCategory, val mandatory: Boolean, val guaranteedSourceId: String)
data class CatalogRecipe(val id: String, val kind: RecipeKind, val inputs: Set<String>, val outputId: String)
data class CatalogTechnology(val id: String, val requiredTechnologyIds: Set<String>, val unlockDayRegular: Int)
data class CatalogModule(val id: String, val rarity: ModuleRarity, val setId: String)
data class CatalogSector(val id: String, val tier: SectorTier, val requiredSectorIds: Set<String>, val targetDayRegular: Int, val strategicNovelty: String)
data class CatalogDeposit(val id: String, val resourceId: String, val sectorId: String, val guaranteed: Boolean)
data class CatalogBuilding(val id: String, val tier: String, val visualTiers: Int)
data class CatalogRobot(val family: String, val levels: Int, val specializations: Set<String>)
data class CatalogMission(val id: String, val group: MissionGroup, val requiredMissionIds: Set<String>, val rewardSpaceDollars: Long)
data class CatalogAchievement(val id: String, val requiredMissionIds: Set<String>)
data class CatalogContract(val id: String, val tier: ContractTier, val resourceCategory: ResourceCategory)
data class CatalogEvent(val id: String, val kind: String, val mandatory: Boolean)
data class CatalogCodexEntry(val id: String, val category: String, val analysisLevels: Int, val collectionId: String)
data class CatalogCollection(val id: String, val entryIds: Set<String>)
data class NarrativeMilestone(val id: String, val transmissionIds: Set<String>)
data class CatalogTransmission(val id: String)
data class PlayerProfileDefinition(val id: String, val dailyProgressPoints: Int, val advertisingBonusPercent: Int, val expectedMinDays: Int, val expectedMaxDays: Int)

data class FerrumDeltaContent(
    val schemaVersion: Int,
    val contentVersion: String,
    val completionProgressPoints: Int,
    val timing: CampaignTiming,
    val resources: List<CatalogResource>,
    val recipes: List<CatalogRecipe>,
    val technologies: List<CatalogTechnology>,
    val modules: List<CatalogModule>,
    val sectors: List<CatalogSector>,
    val deposits: List<CatalogDeposit>,
    val buildings: List<CatalogBuilding>,
    val robots: List<CatalogRobot>,
    val traits: Set<String>,
    val masteryTiers: Set<String>,
    val missions: List<CatalogMission>,
    val achievements: List<CatalogAchievement>,
    val contracts: List<CatalogContract>,
    val events: List<CatalogEvent>,
    val codexEntries: List<CatalogCodexEntry>,
    val collections: List<CatalogCollection>,
    val narrativeMilestones: List<NarrativeMilestone>,
    val transmissions: List<CatalogTransmission>,
    val playerProfiles: List<PlayerProfileDefinition>,
)
