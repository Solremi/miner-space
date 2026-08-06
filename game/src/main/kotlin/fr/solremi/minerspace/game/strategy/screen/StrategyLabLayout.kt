package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Rectangle
import kotlin.math.max

data class StrategyLabLayout(
    val top: Rectangle,
    val specializationPanel: Rectangle,
    val modulePanel: Rectangle,
    val comparisonPanel: Rectangle,
    val specializationButtons: List<Rectangle>,
    val moduleButtons: List<Rectangle>,
    val robotButtons: List<Rectangle>,
    val choose: Rectangle,
    val craft: Rectangle,
    val equip: Rectangle,
    val upgrade: Rectangle,
    val dismantle: Rectangle,
    val back: Rectangle,
)

object StrategyLabLayoutCalculator {
    fun calculate(width: Float, height: Float): StrategyLabLayout {
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1)
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 50f, right - left, 50f)
        val contentTop = topBar.y - 6f
        val actionsHeight = 48f
        val contentBottom = bottom + actionsHeight + 6f
        val safeWidth = right - left
        val specializationWidth = (safeWidth * .27f).coerceAtLeast(170f)
        val moduleWidth = (safeWidth * .40f).coerceAtLeast(260f)
        val comparisonWidth = safeWidth - specializationWidth - moduleWidth - 12f
        val specializationPanel = Rectangle(left, contentBottom, specializationWidth, contentTop - contentBottom)
        val modulePanel = Rectangle(
            specializationPanel.x + specializationPanel.width + 6f,
            contentBottom,
            moduleWidth,
            contentTop - contentBottom,
        )
        val comparisonPanel = Rectangle(
            modulePanel.x + modulePanel.width + 6f,
            contentBottom,
            comparisonWidth,
            contentTop - contentBottom,
        )
        val specializationButtons = gridButtons(specializationPanel, 2, 2, 58f, 38f, 34f)
        val moduleButtons = gridButtons(modulePanel, 4, 2, 60f, 36f, 34f)
        val robotButtons = gridButtons(comparisonPanel, 4, 1, 52f, 34f, 34f)
        val buttonWidth = (safeWidth - 5 * 6f) / 6f
        val actions = List(6) { index ->
            Rectangle(left + index * (buttonWidth + 6f), bottom, buttonWidth, actionsHeight)
        }
        return StrategyLabLayout(
            topBar,
            specializationPanel,
            modulePanel,
            comparisonPanel,
            specializationButtons,
            moduleButtons,
            robotButtons,
            actions[0],
            actions[1],
            actions[2],
            actions[3],
            actions[4],
            actions[5],
        )
    }

    private fun gridButtons(
        panel: Rectangle,
        columns: Int,
        rows: Int,
        cellWidth: Float,
        cellHeight: Float,
        topInset: Float,
    ): List<Rectangle> {
        val gap = 5f
        return List(columns * rows) { index ->
            val column = index % columns
            val row = index / columns
            Rectangle(
                panel.x + 8f + column * (cellWidth + gap),
                panel.y + panel.height - topInset - (row + 1) * cellHeight - row * gap,
                cellWidth,
                cellHeight,
            )
        }
    }
}
