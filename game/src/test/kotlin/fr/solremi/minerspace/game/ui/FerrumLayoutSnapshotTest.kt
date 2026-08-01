package fr.solremi.minerspace.game.ui

import com.badlogic.gdx.math.Rectangle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class FerrumLayoutSnapshotTest {
    @Test
    fun `compact landscape snapshot remains stable`() {
        assertEquals(
            "compact|top=8.0,262.0,624.0,50.0|nav0=8.0,214.0,74.5,48.0|" +
                "nav7=557.5,214.0,74.5,48.0|info=8.0,8.0,268.0,48.0|" +
                "recipe=281.0,8.0,82.0,48.0|action=368.0,8.0,88.0,48.0|" +
                "task=461.0,8.0,88.0,48.0|center=554.0,8.0,78.0,48.0",
            snapshot("compact", FerrumHudLayoutCalculator.calculate(640f, 320f)),
        )
    }

    @Test
    fun `wide and notch snapshots remain stable`() {
        assertEquals(
            "wide|top=8.0,332.0,828.0,50.0|nav0=8.0,284.0,100.0,48.0|" +
                "nav7=736.0,284.0,100.0,48.0|info=8.0,8.0,472.0,48.0|" +
                "recipe=485.0,8.0,82.0,48.0|action=572.0,8.0,88.0,48.0|" +
                "task=665.0,8.0,88.0,48.0|center=758.0,8.0,78.0,48.0",
            snapshot("wide", FerrumHudLayoutCalculator.calculate(844f, 390f)),
        )
        assertEquals(
            "notch|top=32.0,322.0,792.0,50.0|nav0=32.0,274.0,95.5,48.0|" +
                "nav7=728.5,274.0,95.5,48.0|info=32.0,24.0,436.0,48.0|" +
                "recipe=473.0,24.0,82.0,48.0|action=560.0,24.0,88.0,48.0|" +
                "task=653.0,24.0,88.0,48.0|center=746.0,24.0,78.0,48.0",
            snapshot(
                "notch",
                FerrumHudLayoutCalculator.calculate(
                    844f,
                    390f,
                    UiInsets(left = 24f, right = 12f, bottom = 16f, top = 10f),
                ),
            ),
        )
    }

    private fun snapshot(name: String, layout: FerrumHudLayout): String = buildString {
        append(name)
        append("|top=").append(layout.top.value())
        append("|nav0=").append(layout.navigation.first().value())
        append("|nav7=").append(layout.navigation.last().value())
        append("|info=").append(layout.info.value())
        append("|recipe=").append(layout.recipe.value())
        append("|action=").append(layout.action.value())
        append("|task=").append(layout.task.value())
        append("|center=").append(layout.center.value())
    }

    private fun Rectangle.value(): String =
        listOf(x, y, width, height).joinToString(",") {
            String.format(Locale.ROOT, "%.1f", it)
        }
}
