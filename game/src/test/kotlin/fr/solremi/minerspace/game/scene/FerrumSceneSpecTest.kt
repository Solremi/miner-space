package fr.solremi.minerspace.game.scene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FerrumSceneSpecTest {
    @Test
    fun `vertical slice exposes one selectable node per gameplay installation`() {
        assertEquals(FerrumNodeId.entries.toSet(), FerrumSceneSpec.nodes.map { it.id }.toSet())
        assertEquals(FerrumNodeId.entries.size, FerrumSceneSpec.nodes.size)
    }

    @Test
    fun `each picking volume contains its declared world position`() {
        FerrumSceneSpec.nodes.forEach { node ->
            assertTrue(node.pickingBounds.contains(node.position), "Bounds do not contain ${node.id}")
        }
    }
}
