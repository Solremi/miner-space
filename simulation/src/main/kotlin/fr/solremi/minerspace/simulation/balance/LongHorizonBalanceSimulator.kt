package fr.solremi.minerspace.simulation.balance

import kotlin.math.pow

enum class IndustrialStrategy { EXTRACTION, LOGISTICS, RESEARCH }

data class PlayerPattern(
    val name: String,
    val sessionsPerDay: Int,
    val activeMinutesPerSession: Int,
    val rewardedUsageRatio: Double,
) {
    init {
        require(name.isNotBlank())
        require(sessionsPerDay in 1..12)
        require(activeMinutesPerSession in 1..240)
        require(rewardedUsageRatio in 0.0..1.0)
    }

    companion object {
        val CASUAL = PlayerPattern("casual", 1, 10, 0.0)
        val REGULAR = PlayerPattern("regular", 2, 15, 0.15)
        val ACTIVE = PlayerPattern("active", 4, 20, 0.30)
        val REGULAR_NO_ADS = REGULAR.copy(name = "regular_no_ads", rewardedUsageRatio = 0.0)
    }
}

data class FerrumBalanceScenario(
    val targetCampaignValue: Double = 250_000.0,
    val maximumDays: Int = 60,
    val offlineCapMinutesPerSession: Int = 480,
    val baseExtractionPerMinute: Double = 4.0,
    val initialUpgradeCost: Double = 800.0,
    val upgradeCostGrowth: Double = 1.65,
    val maximumUpgradeLevel: Int = 12,
) {
    init {
        require(targetCampaignValue > 0.0)
        require(maximumDays in 1..365)
        require(offlineCapMinutesPerSession > 0)
        require(baseExtractionPerMinute > 0.0)
        require(initialUpgradeCost > 0.0 && upgradeCostGrowth > 1.0)
        require(maximumUpgradeLevel >= 1)
    }
}

data class BalanceDay(
    val day: Int,
    val campaignValue: Double,
    val generatedValue: Double,
    val upgradeLevel: Int,
    val bankedSpaceDollars: Double,
)

data class BalanceSimulationResult(
    val pattern: PlayerPattern,
    val strategy: IndustrialStrategy,
    val completionDay: Int?,
    val finalCampaignValue: Double,
    val finalUpgradeLevel: Int,
    val stalledDays: Int,
    val days: List<BalanceDay>,
) {
    val completed: Boolean get() = completionDay != null
}

class LongHorizonBalanceSimulator(
    private val scenario: FerrumBalanceScenario = FerrumBalanceScenario(),
) {
    fun simulate(
        pattern: PlayerPattern,
        strategy: IndustrialStrategy,
    ): BalanceSimulationResult {
        val extractionMultiplier = if (strategy == IndustrialStrategy.EXTRACTION) 1.16 else 1.0
        val offlineEfficiency = if (strategy == IndustrialStrategy.LOGISTICS) 0.37 else 0.30
        val researchCostMultiplier = if (strategy == IndustrialStrategy.RESEARCH) 1.22 else 1.0
        val researchProgressMultiplier = if (strategy == IndustrialStrategy.RESEARCH) 1.12 else 1.0

        var campaignValue = 0.0
        var bankedSpaceDollars = 0.0
        var upgradeLevel = 1
        var stalledDays = 0
        var completionDay: Int? = null
        val days = mutableListOf<BalanceDay>()

        for (day in 1..scenario.maximumDays) {
            val activeMinutes = pattern.sessionsPerDay * pattern.activeMinutesPerSession
            val offlineMinutes = minOf(
                24 * 60 - activeMinutes,
                scenario.offlineCapMinutesPerSession * pattern.sessionsPerDay,
            ).coerceAtLeast(0)
            val extractionRate = scenario.baseExtractionPerMinute *
                extractionMultiplier *
                (1.0 + 0.16 * (upgradeLevel - 1))
            val rawProduced = extractionRate *
                (activeMinutes + offlineMinutes * offlineEfficiency)
            val refinedProduced = rawProduced * 0.45 *
                (1.0 + 0.05 * (upgradeLevel - 1))
            val rewardedMultiplier = 1.0 + pattern.rewardedUsageRatio * 0.25
            val generatedValue = (rawProduced * 0.4 + refinedProduced * 1.5) * rewardedMultiplier
            bankedSpaceDollars += generatedValue

            while (upgradeLevel < scenario.maximumUpgradeLevel) {
                val nextCost = scenario.initialUpgradeCost *
                    scenario.upgradeCostGrowth.pow(upgradeLevel - 1) /
                    researchCostMultiplier
                if (bankedSpaceDollars < nextCost) break
                bankedSpaceDollars -= nextCost
                upgradeLevel++
            }

            campaignValue += generatedValue *
                (1.0 + 0.08 * (upgradeLevel - 1)) *
                researchProgressMultiplier
            if (generatedValue < 1_000.0) stalledDays++
            days += BalanceDay(day, campaignValue, generatedValue, upgradeLevel, bankedSpaceDollars)
            if (campaignValue >= scenario.targetCampaignValue) {
                completionDay = day
                break
            }
        }

        return BalanceSimulationResult(
            pattern = pattern,
            strategy = strategy,
            completionDay = completionDay,
            finalCampaignValue = campaignValue,
            finalUpgradeLevel = upgradeLevel,
            stalledDays = stalledDays,
            days = days,
        )
    }

    fun matrix(patterns: List<PlayerPattern>): List<BalanceSimulationResult> =
        patterns.flatMap { pattern ->
            IndustrialStrategy.entries.map { strategy -> simulate(pattern, strategy) }
        }
}
