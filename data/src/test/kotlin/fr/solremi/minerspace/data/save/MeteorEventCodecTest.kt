package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.event.MeteorEventEngine
import fr.solremi.minerspace.domain.event.MeteorEventPhase
import fr.solremi.minerspace.domain.event.MeteorEventState
import fr.solremi.minerspace.domain.event.MeteorFragment
import fr.solremi.minerspace.domain.event.MeteorFragmentKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MeteorEventCodecTest {
    @Test
    fun `round trip keeps interruption and commit preparation state`() {
        val state = MeteorEventState(
            eventId = "meteor_42",
            seed = 42L,
            phase = MeteorEventPhase.COMMITTING,
            elapsedActiveMillis = 31_250L,
            nextSpawnIndex = 23L,
            rareSpawned = true,
            fragments = listOf(
                MeteorFragment("standard_22", MeteorFragmentKind.STANDARD, 400_000, 900_000, 10_000, -150_000, 30_800L),
            ),
            standardCollected = 9L,
            rareCollected = 1L,
            assistanceEnabled = true,
            lastAssistAtMillis = 30_000L,
            expectedStandardInventory = 27L,
            expectedRareInventory = 2L,
            codexEntryIds = setOf(MeteorEventEngine.CODEX_EVENT, MeteorEventEngine.CODEX_RARE),
            transactionSequence = 14L,
        )
        val codec = MeteorEventCodec()
        val decoded = codec.decode(codec.encode(state, "0.7.0", 5_000L))
        assertEquals(state, decoded)
    }
}
