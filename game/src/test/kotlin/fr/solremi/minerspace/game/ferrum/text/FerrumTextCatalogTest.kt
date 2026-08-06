package fr.solremi.minerspace.game.ferrum.text

import fr.solremi.minerspace.game.ferrum.model.FerrumColonyStage
import fr.solremi.minerspace.game.ui.FerrumPrimaryDestination
import fr.solremi.minerspace.game.ui.FerrumSecondaryDestination
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FerrumTextCatalogTest {
    @Test
    fun `all navigation and colony labels are defined`() {
        assertTrue(FerrumPrimaryDestination.entries.all { FrenchFerrumText.primaryDestination(it).isNotBlank() })
        assertTrue(FerrumSecondaryDestination.entries.all { FrenchFerrumText.secondaryDestination(it).isNotBlank() })
        assertTrue(FerrumColonyStage.entries.all { FrenchFerrumText.stageName(it).isNotBlank() })
    }

    @Test
    fun `save freshness is formatted consistently`() {
        assertEquals("NON SAUVEGARDÉ", FrenchFerrumText.saveStatus(null))
        assertEquals("SAUVEGARDÉ", FrenchFerrumText.saveStatus(2L))
        assertEquals("SAUV. 42s", FrenchFerrumText.saveStatus(42L))
        assertEquals("SAUV. 2min", FrenchFerrumText.saveStatus(125L))
    }

    @Test
    fun `advice templates reject missing or unexpected arguments`() {
        assertThrows(IllegalArgumentException::class.java) {
            FrenchFerrumText.advice(FerrumAdviceKey.PRODUCTION_READY_DETAIL)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FrenchFerrumText.advice(
                FerrumAdviceKey.INITIAL_TITLE,
                mapOf("unexpected" to "value"),
            )
        }
    }

    @Test
    fun `advice template replaces declared arguments`() {
        assertEquals(
            "Lingots prête dans le raffineur. Touchez ici pour vous y rendre.",
            FrenchFerrumText.advice(
                FerrumAdviceKey.PRODUCTION_READY_DETAIL,
                mapOf("name" to "Lingots"),
            ),
        )
    }
}
