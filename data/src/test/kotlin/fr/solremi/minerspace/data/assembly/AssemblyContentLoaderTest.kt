package fr.solremi.minerspace.data.assembly

import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AssemblyContentLoaderTest {
    @Test
    fun `loads component recipes technologies and prerequisites`() {
        val definitions = AssemblyContentLoader().parse(VALID_JSON)

        assertEquals("0.4.0-test", definitions.contentVersion)
        assertEquals(4, definitions.robot.queueCapacity)
        assertEquals(
            AssemblyOutputKind.TECHNOLOGY,
            definitions.recipes.getValue(GameId.of("assembly_tech_two")).outputKind,
        )
        assertEquals(
            setOf(GameId.of("tech_one")),
            definitions.technologies.getValue(GameId.of("tech_two")).requiredTechnologyIds,
        )
    }

    @Test
    fun `rejects decimal production values`() {
        assertThrows(IllegalArgumentException::class.java) {
            AssemblyContentLoader().parse(
                VALID_JSON.replace("\"durationSeconds\": 20", "\"durationSeconds\": 20.5"),
            )
        }
    }

    private companion object {
        val VALID_JSON = """
            {
              "schemaVersion": 1,
              "contentVersion": "0.4.0-test",
              "robot": {"id":"robot_as_01","nameKey":"robot.as","queueCapacity":4},
              "recipes": [
                {
                  "id":"assembly_tech_one",
                  "nameKey":"assembly.tech_one",
                  "inputs":{"component_power_cell":2},
                  "outputResourceId":"tech_one_item",
                  "outputQuantity":1,
                  "durationSeconds":20,
                  "outputKind":"TECHNOLOGY",
                  "requiredTechnologyIds":[]
                },
                {
                  "id":"assembly_tech_two",
                  "nameKey":"assembly.tech_two",
                  "inputs":{"component_power_cell":3},
                  "outputResourceId":"tech_two_item",
                  "outputQuantity":1,
                  "durationSeconds":30,
                  "outputKind":"TECHNOLOGY",
                  "requiredTechnologyIds":["tech_one"]
                }
              ],
              "technologies": [
                {"id":"tech_one","nameKey":"tech.one","itemResourceId":"tech_one_item","requiredTechnologyIds":[],"extractionBonusMillionths":200000},
                {"id":"tech_two","nameKey":"tech.two","itemResourceId":"tech_two_item","requiredTechnologyIds":["tech_one"],"extractionBonusMillionths":150000}
              ]
            }
        """.trimIndent()
    }
}
