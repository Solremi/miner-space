package fr.solremi.minerspace.data.refining

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RefiningContentLoaderTest {
    @Test
    fun `loads robot and integer recipe definitions`() {
        val definitions = RefiningContentLoader().parse(
            """
            {
              "schemaVersion": 1,
              "contentVersion": "test",
              "robot": {"id":"robot_rf_01","nameKey":"robot.rf","queueCapacity":4},
              "recipes": [
                {
                  "id":"recipe_iron_ingot",
                  "nameKey":"recipe.iron",
                  "inputs":{"raw_iron":12},
                  "outputResourceId":"refined_iron_ingot",
                  "outputQuantity":4,
                  "durationSeconds":12
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(4, definitions.robot.queueCapacity)
        assertEquals(
            12L,
            definitions.recipes.getValue(GameId.of("recipe_iron_ingot"))
                .inputs.getValue(GameId.of("raw_iron")),
        )
    }

    @Test
    fun `rejects decimal economy values`() {
        assertThrows(IllegalArgumentException::class.java) {
            RefiningContentLoader().parse(
                """
                {
                  "schemaVersion": 1,
                  "contentVersion": "test",
                  "robot": {"id":"robot_rf_01","nameKey":"robot.rf","queueCapacity":4},
                  "recipes": [
                    {
                      "id":"recipe_iron_ingot",
                      "nameKey":"recipe.iron",
                      "inputs":{"raw_iron":12.5},
                      "outputResourceId":"refined_iron_ingot",
                      "outputQuantity":4,
                      "durationSeconds":12
                    }
                  ]
                }
                """.trimIndent(),
            )
        }
    }
}
