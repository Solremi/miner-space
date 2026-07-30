package fr.solremi.minerspace.data.economy

import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CoreEconomyContentLoaderTest {
    @Test
    fun `loads integer economy definitions from versioned json`() {
        val definitions = CoreEconomyContentLoader().load(
            repository = object : ContentRepository {
                override fun readText(path: String): String = VALID_JSON
            },
        )

        assertEquals(1, definitions.schemaVersion)
        assertEquals("0.2.0-test", definitions.contentVersion)
        assertEquals(2L, definitions.resources.getValue(GameId.of("raw_iron")).unitSalePrice)
        assertEquals(3L, definitions.deposits.getValue(GameId.of("deposit_iron_alpha")).extractionPerSecond)
    }

    @Test
    fun `rejects decimal economy values`() {
        assertThrows(IllegalArgumentException::class.java) {
            CoreEconomyContentLoader().parse(VALID_JSON.replace("\"unitSalePrice\": 2", "\"unitSalePrice\": 2.5"))
        }
    }

    private companion object {
        val VALID_JSON = """
            {
              "schemaVersion": 1,
              "contentVersion": "0.2.0-test",
              "items": [
                {
                  "type": "resource",
                  "id": "raw_iron",
                  "nameKey": "resource.raw_iron",
                  "unitSalePrice": 2,
                  "storageCapacity": 1000,
                  "sellable": true
                },
                {
                  "type": "deposit",
                  "id": "deposit_iron_alpha",
                  "resourceId": "raw_iron",
                  "initialReserve": 10000,
                  "extractionPerSecond": 3,
                  "transportCapacity": 300
                }
              ]
            }
        """.trimIndent()
    }
}
