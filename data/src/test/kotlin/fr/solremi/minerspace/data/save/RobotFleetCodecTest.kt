package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.domain.robot.AutomationPriority
import fr.solremi.minerspace.domain.robot.RenderQuality
import fr.solremi.minerspace.domain.robot.RobotAutomationEngine
import fr.solremi.minerspace.domain.robot.RobotCommandResult
import fr.solremi.minerspace.domain.robot.RobotFamily
import fr.solremi.minerspace.domain.services.ContentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RobotFleetCodecTest {
    @Test
    fun `robot identity levels priorities and statistics survive round trip`() {
        val text = requireNotNull(javaClass.classLoader.getResource("data/robots.json")).readText()
        val definitions = RobotContentLoader().load(object : ContentRepository {
            override fun readText(path: String): String? = text
        })
        val engine = RobotAutomationEngine(definitions)
        var state = engine.initialState(1_000L).copy(renderQuality = RenderQuality.HIGH)
        val logisticsId = state.robots.values.first { it.family == RobotFamily.LOGISTICS }.id
        state = (engine.upgrade(state, logisticsId, 10_000L) as RobotCommandResult.Applied).state
        state = (engine.cyclePriority(state, logisticsId) as RobotCommandResult.Applied).state
        state = engine.recordWork(state, logisticsId, 450L, 300L)
        val codec = RobotFleetCodec()
        val restored = codec.decode(codec.encode(state, definitions.contentVersion, 2_000L))
        assertEquals(state, restored)
        assertEquals(AutomationPriority.RARE_RESOURCE, restored.robots.getValue(logisticsId).priority)
    }
}
