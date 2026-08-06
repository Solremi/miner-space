package fr.solremi.minerspace.domain.ads

import fr.solremi.minerspace.shared.GameId

enum class RewardType {
    TIME_RELAY,
    OFFLINE_DOUBLE,
    STANDARD_MATERIALS,
    PREMIUM_CONTRACT,
    ANALYSIS,
    METEOR_RECOVERY,
    METEOR_EXTENSION,
    ORBITAL_BOOST,
}

enum class RewardScope { DAILY, RETURN, EVENT }
enum class PendingRewardStatus { PREPARED, SDK_REWARDED }

data class RewardedOfferDefinition(
    val id: GameId,
    val title: String,
    val rewardDescription: String,
    val rewardType: RewardType,
    val rewardValue: Long,
    val rewardDurationMillis: Long,
    val dailyLimit: Int,
    val cooldownMillis: Long,
    val scope: RewardScope,
) {
    init {
        require(title.isNotBlank() && rewardDescription.isNotBlank())
        require(rewardValue > 0L && rewardDurationMillis >= 0L)
        require(dailyLimit > 0 && cooldownMillis >= 0L)
    }
}

data class RewardedAdvertisingDefinitions(
    val schemaVersion: Int,
    val contentVersion: String,
    val globalDailyLimit: Int,
    val offers: Map<GameId, RewardedOfferDefinition>,
) {
    init {
        require(schemaVersion > 0 && contentVersion.isNotBlank())
        require(globalDailyLimit > 0 && offers.isNotEmpty())
        require(offers.values.map { it.rewardType }.distinct().size == offers.size)
        require(offers.values.none { it.dailyLimit > globalDailyLimit })
    }
}
