package fr.solremi.minerspace.domain.event

import fr.solremi.minerspace.shared.GameId

enum class MeteorFragmentKind { STANDARD, RARE }
enum class MeteorEventPhase { ACTIVE, SUMMARY, COMMITTING, COMMITTED }

data class MeteorEventDefinition(
    val schemaVersion: Int,
    val contentVersion: String,
    val durationMillis: Long,
    val spawnIntervalMillis: Long,
    val maxActiveFragments: Int,
    val fragmentLifetimeMillis: Long,
    val rareSpawnAtMillis: Long,
    val standardResourceId: GameId,
    val rareResourceId: GameId,
    val standardRewardPerFragment: Long,
    val rareRewardQuantity: Long,
    val captureRadiusMillionths: Int,
    val assistedCaptureRadiusMillionths: Int,
    val assistAutoCollectIntervalMillis: Long,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(durationMillis in 45_000L..90_000L)
        require(spawnIntervalMillis in 250L..10_000L)
        require(maxActiveFragments in 1..64)
        require(fragmentLifetimeMillis in 1_500L..20_000L)
        require(rareSpawnAtMillis in 1L until durationMillis)
        require(standardRewardPerFragment > 0L)
        require(rareRewardQuantity > 0L)
        require(captureRadiusMillionths in 10_000..250_000)
        require(assistedCaptureRadiusMillionths >= captureRadiusMillionths)
        require(assistAutoCollectIntervalMillis in 500L..20_000L)
    }
}
