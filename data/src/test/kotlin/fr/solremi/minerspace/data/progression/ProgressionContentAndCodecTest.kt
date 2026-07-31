package fr.solremi.minerspace.data.progression

import fr.solremi.minerspace.data.save.ProgressionStateCodec
import fr.solremi.minerspace.domain.progression.ContractTier
import fr.solremi.minerspace.domain.progression.ProgressionEngine
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ProgressionContentAndCodecTest {
    @Test fun `versioned content exposes guided week parallel contracts and permanent codex`() {
        val content = Files.readString(Path.of("../assets/data/progression.json"))
        val definitions = ProgressionContentLoader().parse(content)
        assertEquals("0.10.0", definitions.contentVersion)
        assertEquals(7, definitions.tutorialSteps.size)
        assertEquals(ContractTier.entries.toSet(), definitions.contracts.map { it.tier }.toSet())
        assertTrue(definitions.codexEntries.size >= 10)
    }

    @Test fun `progression save restores exact tutorial and selections`() {
        val content = Files.readString(Path.of("../assets/data/progression.json"))
        val definitions = ProgressionContentLoader().parse(content)
        val state = ProgressionEngine(definitions).initialState().copy(
            tutorialStepIndex = 3,
            completedTutorialIds = definitions.tutorialSteps.take(3).mapTo(linkedSetOf()) { it.id },
            claimedMissionIds = setOf(GameId.of("main_foundation")),
            selectedObjectiveId = GameId.of("main_refining"),
            transactionSequence = 4,
        )
        val codec = ProgressionStateCodec()
        val restored = codec.decode(codec.encode(state, definitions.contentVersion, 123L))
        assertEquals(state, restored)
    }
}
