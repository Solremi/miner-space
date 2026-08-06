package fr.solremi.minerspace.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FerrumPlayerHudTest {
    @Test
    fun `primary navigation contains four readable destinations`() {
        val layout = FerrumPlayerHudLayoutCalculator.calculate(640f, 320f)

        assertEquals(4, layout.primaryNavigation.size)
        assertEquals(
            listOf("EXPLORER", "FLOTTE", "MISSIONS", "MENU"),
            FerrumPrimaryDestination.entries.map { it.label },
        )
        assertTrue(layout.primaryNavigation.all { it.width >= 48f && it.height >= 48f })
    }

    @Test
    fun `secondary menu stays inside the 640 by 320 safe area`() {
        val layout = FerrumPlayerHudLayoutCalculator.calculate(
            width = 640f,
            height = 320f,
            insets = UiInsets(left = 8f, right = 12f, bottom = 6f, top = 10f),
            menuOpen = true,
        )

        assertEquals(FerrumSecondaryDestination.entries.size, layout.secondaryMenu.size)
        assertTrue(layout.secondaryMenu.all { it.width >= 48f && it.height >= 48f })
        assertTrue(layout.secondaryMenu.all { it.x >= layout.safeArea.left })
        assertTrue(layout.secondaryMenu.all { it.x + it.width <= layout.safeArea.right })
        assertTrue(layout.secondaryMenu.all { it.y >= layout.safeArea.bottom })
        assertTrue(layout.secondaryMenu.all { it.y + it.height <= layout.safeArea.top })
    }
}
