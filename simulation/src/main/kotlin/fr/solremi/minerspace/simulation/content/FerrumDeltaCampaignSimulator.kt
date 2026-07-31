package fr.solremi.minerspace.simulation.content

import fr.solremi.minerspace.domain.content.FerrumDeltaContent
import fr.solremi.minerspace.domain.content.PlayerProfileDefinition
import kotlin.math.ceil

data class CampaignSimulationResult(
    val profileId: String,
    val advertisingEnabled: Boolean,
    val completionDays: Int,
    val unlockedSectorIds: Set<String>,
    val blockedSectorIds: Set<String>,
    val mandatoryResourceSourceIds: Set<String>,
)

class FerrumDeltaCampaignSimulator(private val content: FerrumDeltaContent) {
    fun simulate(profileId: String, advertisingEnabled: Boolean = false): CampaignSimulationResult {
        val profile = content.playerProfiles.firstOrNull { it.id == profileId }
            ?: error("Unknown profile: $profileId")
        val dailyProgress = dailyProgress(profile, advertisingEnabled)
        val unlocked = linkedSetOf<String>()
        var points = 0
        var days = 0

        while (points < content.completionProgressPoints) {
            days++
            points = Math.addExact(points, dailyProgress)
            var changed: Boolean
            do {
                changed = false
                content.sectors
                    .sortedBy { it.targetDayRegular }
                    .filter { it.id !in unlocked }
                    .forEach { sector ->
                        val threshold = Math.multiplyExact(sector.targetDayRegular, REGULAR_DAILY_PROGRESS)
                        if (points >= threshold && unlocked.containsAll(sector.requiredSectorIds)) {
                            unlocked += sector.id
                            changed = true
                        }
                    }
            } while (changed)
            require(days <= 365) { "Campaign simulation failed to converge" }
        }

        val blocked = content.sectors
            .filter { it.id !in unlocked || !unlocked.containsAll(it.requiredSectorIds) }
            .mapTo(linkedSetOf()) { it.id }
        val mandatorySources = content.resources
            .filter { it.mandatory }
            .mapTo(linkedSetOf()) { it.guaranteedSourceId }

        return CampaignSimulationResult(
            profileId = profile.id,
            advertisingEnabled = advertisingEnabled,
            completionDays = days,
            unlockedSectorIds = unlocked,
            blockedSectorIds = blocked,
            mandatoryResourceSourceIds = mandatorySources,
        )
    }

    fun expectedDays(profileId: String, advertisingEnabled: Boolean = false): Int {
        val profile = content.playerProfiles.firstOrNull { it.id == profileId }
            ?: error("Unknown profile: $profileId")
        return ceil(content.completionProgressPoints.toDouble() / dailyProgress(profile, advertisingEnabled)).toInt()
    }

    fun isWithinPublishedRange(result: CampaignSimulationResult): Boolean {
        val profile = content.playerProfiles.first { it.id == result.profileId }
        return result.completionDays in profile.expectedMinDays..profile.expectedMaxDays
    }

    private fun dailyProgress(profile: PlayerProfileDefinition, advertisingEnabled: Boolean): Int {
        val percent = if (advertisingEnabled) 100 + profile.advertisingBonusPercent else 100
        return Math.multiplyExact(profile.dailyProgressPoints, percent) / 100
    }

    private companion object {
        const val REGULAR_DAILY_PROGRESS = 1_000
    }
}
