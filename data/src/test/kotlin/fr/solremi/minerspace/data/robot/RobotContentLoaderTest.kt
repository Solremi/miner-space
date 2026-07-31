package fr.solremi.minerspace.data.robot

import fr.solremi.minerspace.domain.robot.RenderQuality
import fr.solremi.minerspace.domain.robot.RobotFamily
import fr.solremi.minerspace.domain.services.ContentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RobotContentLoaderTest {
    @Test
    fun `versioned robot content defines four families and fifty high quality units`() {
        val text = requireNotNull(javaClass.classLoader.getResource("data/robots.json")).readText()
        val definitions = RobotContentLoader().load(object : ContentRepository {
            override fun readText(path: String): String? = text
        })
        assertEquals("0.8.0", definitions.contentVersion)
        assertEquals(RobotFamily.entries.toSet(), definitions.families.keys)
        assertEquals(50, definitions.visibleUnitsByQuality.getValue(RenderQuality.HIGH))
        assertEquals(5, definitions.families.getValue(RobotFamily.LOGISTICS).maxLevel)
    }
}
