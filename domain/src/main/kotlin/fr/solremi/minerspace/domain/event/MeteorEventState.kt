package fr.solremi.minerspace.domain.event

import fr.solremi.minerspace.shared.GameId

data class MeteorFragment(
    val id: String,
    val kind: MeteorFragmentKind,
    val spawnXMillionths: Int,
    val spawnYMillionths: Int,
    val velocityXMillionthsPerSecond: Int,
    val velocityYMillionthsPerSecond: Int,
    val spawnedAtActiveMillis: Long,
) {
    init {
        require(id.isNotBlank())
        require(spawnXMillionths in -250_000..1_250_000)
        require(spawnYMillionths in -250_000..1_250_000)
        require(velocityXMillionthsPerSecond in -500_000..500_000)
        require(velocityYMillionthsPerSecond in -500_000..500_000)
        require(spawnedAtActiveMillis >= 0L)
    }
}

data class MeteorPoint(val xMillionths: Int, val yMillionths: Int)

data class MeteorEventState(
    val eventId: String,
    val seed: Long,
    val phase: MeteorEventPhase,
    val elapsedActiveMillis: Long,
    val nextSpawnIndex: Long,
    val rareSpawned: Boolean,
    val fragments: List<MeteorFragment>,
    val standardCollected: Long,
    val rareCollected: Long,
    val assistanceEnabled: Boolean,
    val lastAssistAtMillis: Long,
    val expectedStandardInventory: Long?,
    val expectedRareInventory: Long?,
    val codexEntryIds: Set<GameId>,
    val transactionSequence: Long,
) {
    init {
        require(eventId.isNotBlank())
        require(elapsedActiveMillis >= 0L)
        require(nextSpawnIndex >= 0L)
        require(standardCollected >= 0L)
        require(rareCollected >= 0L)
        require(lastAssistAtMillis >= 0L)
        require(transactionSequence >= 0L)
        require(fragments.map { it.id }.distinct().size == fragments.size)
        require(expectedStandardInventory == null || expectedStandardInventory >= 0L)
        require(expectedRareInventory == null || expectedRareInventory >= 0L)
        if (phase == MeteorEventPhase.COMMITTING || phase == MeteorEventPhase.COMMITTED) {
            require(expectedStandardInventory != null && expectedRareInventory != null)
        }
    }
}

data class MeteorCaptureResult(
    val state: MeteorEventState,
    val captured: MeteorFragmentKind?,
)
