package fr.solremi.minerspace.data.narrative

import fr.solremi.minerspace.data.save.NarrativeStateCodec
import fr.solremi.minerspace.domain.narrative.NarrativeEngine
import fr.solremi.minerspace.domain.narrative.PendingNarrativeGrant
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NarrativeContentAndCodecTest {
    @Test
    fun `loader parses four ordered chapters and veteran target`() {
        val definitions = NarrativeContentLoader().parse(CONTENT)
        assertEquals("0.11.0", definitions.contentVersion)
        assertEquals(4, definitions.chapters.size)
        assertEquals(GameId.of("robot_extractor_01"), definitions.veteranRobotId)
        assertEquals(6000L, definitions.veteranMasteryPoints)
    }

    @Test
    fun `loader rejects decimal values`() {
        assertThrows(IllegalArgumentException::class.java) {
            NarrativeContentLoader().parse(CONTENT.replace("6000", "6000.5"))
        }
    }

    @Test
    fun `codec round trips pending grant`() {
        val definitions = NarrativeContentLoader().parse(CONTENT)
        val base = NarrativeEngine(definitions).initialState()
        val state = base.copy(
            readTransmissionIds = setOf(GameId.of("nova_first_contact")),
            anomalyAttempts = mapOf(GameId.of("nova_first_contact") to 1),
            pendingGrant = PendingNarrativeGrant("grant|safe", GameId.of("nova_first_contact"), null, 0, null, 0),
            transactionSequence = 3,
        )
        val codec = NarrativeStateCodec()
        val payload = codec.encode(state, definitions.contentVersion, 1000)
        assertEquals(state, codec.decode(payload))
    }

    private companion object {
        val CONTENT = javaClass.classLoader.getResource("data/narrative.json")!!.readText()
    }
}
