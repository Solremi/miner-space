package fr.solremi.minerspace.data.ads

import fr.solremi.minerspace.domain.ads.*
import fr.solremi.minerspace.shared.GameId

object RewardedAdvertisingContentFactory {
    fun create(): RewardedAdvertisingDefinitions {
        val offers = listOf(
            offer("ad_time_relay", "Relais temporel", "-25 % sur une tâche, maximum 2 h", RewardType.TIME_RELAY, 1, 0, 5, 10, RewardScope.DAILY),
            offer("ad_offline_double", "Production hors ligne", "Double jusqu’à 8 h de production standard", RewardType.OFFLINE_DOUBLE, 1, 0, 1, 0, RewardScope.RETURN),
            offer("ad_supply_capsule", "Capsule sponsorisée", "10 min de matériaux standards", RewardType.STANDARD_MATERIALS, 10, 0, 3, 20, RewardScope.DAILY),
            offer("ad_premium_contract", "Contrat premium", "Un contrat au coefficient maximal 1,75", RewardType.PREMIUM_CONTRACT, 1, 0, 2, 30, RewardScope.DAILY),
            offer("ad_analysis_beacon", "Balise d’analyse", "Une analyse standard gratuite", RewardType.ANALYSIS, 1, 0, 2, 30, RewardScope.DAILY),
            offer("ad_meteor_drone", "Drone météoritique", "Récupère jusqu’à 25 % des fragments standards manqués", RewardType.METEOR_RECOVERY, 25, 0, 2, 0, RewardScope.EVENT),
            offer("ad_meteor_extension", "Prolongation météoritique", "+15 secondes, une fois par événement", RewardType.METEOR_EXTENSION, 15, 0, 1, 0, RewardScope.EVENT),
            offer("ad_orbital_boost", "Boost orbital", "+25 % sur une catégorie pendant 15 min", RewardType.ORBITAL_BOOST, 25, 15 * 60_000L, 2, 60, RewardScope.DAILY),
        ).associateBy { it.id }
        return RewardedAdvertisingDefinitions(
            schemaVersion = 1,
            contentVersion = "1.0.0",
            globalDailyLimit = 10,
            offers = offers,
        )
    }

    private fun offer(
        id: String,
        title: String,
        reward: String,
        type: RewardType,
        value: Long,
        durationMillis: Long,
        limit: Int,
        cooldownMinutes: Long,
        scope: RewardScope,
    ) = RewardedOfferDefinition(
        id = GameId.of(id),
        title = title,
        rewardDescription = reward,
        rewardType = type,
        rewardValue = value,
        rewardDurationMillis = durationMillis,
        dailyLimit = limit,
        cooldownMillis = cooldownMinutes * 60_000L,
        scope = scope,
    )
}
