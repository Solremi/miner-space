package fr.solremi.minerspace.shared.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameTextCatalogTest {
    @Test
    fun `all keys have a french value`() {
        GameTextKey.entries.forEach { key ->
            if (key == GameTextKey.EXPLORATION_SECTOR_OPENED) {
                assertEquals(
                    "Secteur ouvert · ressources rares",
                    FrenchGameText.text(key, mapOf("reason" to "ressources rares")),
                )
            } else {
                val value = FrenchGameText.text(key)
                require(value.isNotBlank())
            }
        }
    }

    @Test
    fun `template arguments are exact`() {
        assertThrows(IllegalArgumentException::class.java) {
            FrenchGameText.text(GameTextKey.EXPLORATION_SECTOR_OPENED)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FrenchGameText.text(
                GameTextKey.EXPLORATION_SECTOR_OPENED,
                mapOf("reason" to "test", "extra" to 1),
            )
        }
    }
}
