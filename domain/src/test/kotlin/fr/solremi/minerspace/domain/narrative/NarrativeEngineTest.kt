package fr.solremi.minerspace.domain.narrative

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NarrativeEngineTest {
    @Test
    fun `narrative remains optional and archives read transmissions`() {
        val engine = NarrativeEngine(definitions())
        val snapshot = snapshot()
        val initial = engine.initialState()
        assertEquals(0, engine.visibleArchives(initial).size)
        assertEquals(1, engine.chapterViews(initial, snapshot).count { it.available })
        val read = engine.readTransmission(initial, id("c1"), snapshot) as NarrativeCommandResult.Applied
        assertEquals(1, engine.visibleArchives(read.state).size)
        assertTrue(id("c1") in read.state.readTransmissionIds)
    }

    @Test
    fun `pity threshold guarantees rare discoveries and veteran robot`() {
        val engine = NarrativeEngine(definitions())
        val snapshot = snapshot()
        var state = engine.initialState()
        val expectations = listOf(id("c1") to 1, id("c2") to 2, id("c3") to 3)
        expectations.forEach { (chapterId, expectedAttempts) ->
            state = (engine.readTransmission(state, chapterId, snapshot) as NarrativeCommandResult.Applied).state
            while (state.pendingGrant == null) {
                state = (engine.investigate(state, chapterId, snapshot) as NarrativeCommandResult.Applied).state
            }
            assertEquals(expectedAttempts, state.anomalyAttempts.getValue(chapterId))
            state = (engine.finalizePending(state) as NarrativeCommandResult.Applied).state
        }
        assertEquals(id("robot_extractor_01"), state.veteranRobotId)
        assertEquals(setOf(id("rare_prismatic_ferrite"), id("rare_xenon_crystal")), state.discoveredRareResourceIds)
    }

    @Test
    fun `pending grant only requests missing quantities`() {
        val engine = NarrativeEngine(definitions())
        val grant = PendingNarrativeGrant("grant", id("c3"), id("rare_xenon_crystal"), 3, id("robot_extractor_01"), 6000)
        val partial = snapshot().copy(
            inventory = mapOf(id("rare_xenon_crystal") to 2L),
            robotMasteryById = mapOf(id("robot_extractor_01") to 5500L),
        )
        assertEquals(1L, engine.missingRareQuantity(grant, partial))
        assertEquals(500L, engine.requiredVeteranMastery(grant, partial))
        val completed = partial.copy(
            inventory = mapOf(id("rare_xenon_crystal") to 3L),
            robotMasteryById = mapOf(id("robot_extractor_01") to 6000L),
        )
        assertEquals(0L, engine.missingRareQuantity(grant, completed))
        assertEquals(0L, engine.requiredVeteranMastery(grant, completed))
    }

    private fun definitions(): NarrativeDefinitions = NarrativeDefinitions(
        1, "0.11.0", 6000, id("robot_extractor_01"),
        listOf(
            NarrativeChapterDefinition(id("c1"), NarrativeChapterKind.CONTACT, "c1", "t1", "a1", 1, 0, emptySet(), 100, 1, 1, null, false),
            NarrativeChapterDefinition(id("c2"), NarrativeChapterKind.RUINS, "c2", "t2", "a2", 2, 0, setOf(id("c1")), 0, 2, 2, id("rare_prismatic_ferrite"), false),
            NarrativeChapterDefinition(id("c3"), NarrativeChapterKind.LEGACY, "c3", "t3", "a3", 3, 1, setOf(id("c2")), 0, 3, 3, id("rare_xenon_crystal"), true),
        ).associateBy { it.id },
    )

    private fun snapshot() = NarrativeSnapshot(4, 2, emptyMap(), mapOf(id("robot_extractor_01") to 0L))
    private fun id(value: String) = GameId.of(value)
}
