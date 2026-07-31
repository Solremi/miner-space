package fr.solremi.minerspace.data.frontier

import fr.solremi.minerspace.data.save.FrontierStateCodec
import fr.solremi.minerspace.domain.frontier.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FrontierContentAndCodecTest {
    @Test
    fun `factory exposes the complete frontier budget`() {
        val definitions = FrontierContentFactory.create()
        assertEquals("1.0.0", definitions.contentVersion)
        assertEquals(3, FrontierVisualFamily.entries.size)
        assertEquals(24, definitions.sectorTemplates.size)
        assertEquals(12, definitions.modifiers.size)
        assertEquals(6, definitions.objectives.size)
        FrontierVisualFamily.entries.forEach { family ->
            assertEquals(8, definitions.sectorTemplates.values.count { it.family == family })
        }
    }

    @Test
    fun `multi world save resumes the selected world exactly`() {
        val definitions = FrontierContentFactory.create()
        val engine = FrontierEngine(definitions)
        var state = engine.initialState(123_456L)
        FrontierDifficulty.entries.forEachIndexed { index, difficulty ->
            state = (engine.discoverWorld(state, difficulty, 1_000L + index) as FrontierCommandResult.Applied).state
        }
        state = (engine.performAction(state, 2_000L) as FrontierCommandResult.Applied).state
        val codec = FrontierStateCodec()
        val restored = codec.decode(codec.encode(state, 3_000L))
        assertEquals(state, restored)
        assertEquals(3, restored.worlds.size)
        assertNotNull(restored.activeWorldId)
        assertTrue(engine.validationErrors(restored).isEmpty())
    }

    @Test
    fun `generator persists its seed and never repeats the immediate signature`() {
        val definitions = FrontierContentFactory.create()
        val generator = FrontierWorldGenerator(definitions)
        var previous: String? = null
        repeat(500) { index ->
            val world = generator.generate(77L, index, FrontierDifficulty.entries[index % 3], previous)
            assertNotEquals(previous, world.signature)
            assertTrue(generator.validationErrors(world).isEmpty())
            previous = world.signature
        }
    }
}
