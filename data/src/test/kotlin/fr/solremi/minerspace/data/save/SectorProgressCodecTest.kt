package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.exploration.ExplorationState
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SectorProgressCodecTest {
    @Test
    fun `sector progress survives a snapshot round trip`() {
        val state = ExplorationState(
            revealedSectorIds = setOf(GameId.of("sector_core"), GameId.of("sector_deep")),
            unlockedSectorIds = setOf(GameId.of("sector_core")),
            discoveredRareDepositIds = setOf(GameId.of("rare_xenon")),
            spentSpaceDollars = 900L,
            spentComponents = mapOf(GameId.of("component_sensor_array") to 3L),
            activeMissionSectorId = GameId.of("sector_deep"),
            transactionSequence = 4L,
        )
        val codec = SectorProgressCodec()
        val restored = codec.decode(codec.encode(state, "0.6.0", savedAtEpochMillis = 123L))
        assertEquals(state, restored)
    }
}
