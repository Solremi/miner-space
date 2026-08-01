package fr.solremi.minerspace.game.layout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LandscapeLayoutPolicyTest {
    @Test
    fun `compact 640 by 320 keeps controls visible and touchable`() {
        val layout = LandscapeLayoutPolicy.calculate(
            worldWidth = 640f,
            worldHeight = 320f,
            screenWidth = 640,
            screenHeight = 320,
        )

        assertTrue(layout.compact)
        assertTrue(layout.content.height > 0f)
        assertEquals(4, layout.controls.size)
        layout.controls.forEach { control ->
            assertTrue(control.width >= LandscapeLayoutPolicy.MIN_TOUCH_TARGET)
            assertTrue(control.height >= LandscapeLayoutPolicy.MIN_TOUCH_TARGET)
            assertTrue(control.x >= layout.safeArea.x)
            assertTrue(control.right <= layout.safeArea.right + .01f)
        }
    }

    @Test
    fun `wide 844 by 390 preserves content between bars`() {
        val layout = LandscapeLayoutPolicy.calculate(
            worldWidth = 844f,
            worldHeight = 390f,
            screenWidth = 844,
            screenHeight = 390,
            insets = ScreenInsets(left = 16, right = 24, bottom = 8),
        )

        assertFalse(layout.compact)
        assertTrue(layout.content.y >= layout.bottomBar.top)
        assertTrue(layout.content.top <= layout.topBar.y)
        assertTrue(layout.safeArea.x > 8f)
        assertTrue(layout.safeArea.right < 836f)
    }

    @Test
    fun `extreme insets never create negative rectangles`() {
        val layout = LandscapeLayoutPolicy.calculate(
            worldWidth = 640f,
            worldHeight = 320f,
            screenWidth = 320,
            screenHeight = 640,
            insets = ScreenInsets(left = 80, top = 120, right = 80, bottom = 120),
            controlCount = 3,
        )

        assertTrue(layout.safeArea.width >= 0f)
        assertTrue(layout.safeArea.height >= 0f)
        assertTrue(layout.content.width >= 0f)
        assertTrue(layout.content.height >= 0f)
    }
}
