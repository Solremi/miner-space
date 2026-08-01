package fr.solremi.minerspace.game.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameUiTest {
    @Test
    fun `Ferrum controls remain touchable on compact landscape formats`() {
        listOf(640f to 320f, 844f to 390f).forEach { (width, height) ->
            val layout = FerrumHudLayoutCalculator.calculate(width, height)
            assertEquals(8, layout.navigation.size)
            assertTrue(layout.interactive.all {
                it.width >= FerrumHudLayoutCalculator.MIN_TOUCH_SIZE &&
                    it.height >= FerrumHudLayoutCalculator.MIN_TOUCH_SIZE
            })
            assertTrue(layout.navigation.zipWithNext().all { (left, right) ->
                left.x + left.width <= right.x
            })
            assertTrue(layout.info.x + layout.info.width <= layout.recipe.x)
        }
    }

    @Test
    fun `safe insets are preserved`() {
        val layout = FerrumHudLayoutCalculator.calculate(
            844f,
            390f,
            UiInsets(left = 24f, right = 12f, bottom = 16f, top = 10f),
        )
        assertEquals(32f, layout.safeArea.left)
        assertEquals(824f, layout.safeArea.right)
        assertEquals(24f, layout.safeArea.bottom)
        assertEquals(372f, layout.safeArea.top)
    }

    @Test
    fun `long labels receive one ellipsis`() {
        assertEquals("Vertical…", UiText.ellipsis("Vertical slice Ferrum", 9))
    }
}
