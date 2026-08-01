package fr.solremi.minerspace.game.ui

import com.badlogic.gdx.math.Rectangle

data class UiInsets(
    val left: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val top: Float = 0f,
) {
    init { require(listOf(left, right, bottom, top).none { it < 0f }) }
}

data class UiSafeArea(
    val left: Float,
    val right: Float,
    val bottom: Float,
    val top: Float,
) {
    init { require(right > left && top > bottom) }
    val width: Float get() = right - left
    val height: Float get() = top - bottom
}

data class FerrumHudLayout(
    val safeArea: UiSafeArea,
    val top: Rectangle,
    val navigation: List<Rectangle>,
    val info: Rectangle,
    val recipe: Rectangle,
    val action: Rectangle,
    val task: Rectangle,
    val center: Rectangle,
) {
    val interactive: List<Rectangle> get() = navigation + listOf(recipe, action, task, center)
}

object FerrumHudLayoutCalculator {
    const val MIN_TOUCH_SIZE = 48f

    fun calculate(
        width: Float,
        height: Float,
        insets: UiInsets = UiInsets(),
        margin: Float = 8f,
    ): FerrumHudLayout {
        require(width > 0f && height > 0f && margin >= 0f)
        val safe = UiSafeArea(
            left = insets.left + margin,
            right = (width - insets.right - margin).coerceAtLeast(insets.left + margin + 1f),
            bottom = insets.bottom + margin,
            top = (height - insets.top - margin).coerceAtLeast(insets.bottom + margin + 1f),
        )
        val topBar = Rectangle(safe.left, safe.top - 50f, safe.width, 50f)
        val navGap = 4f
        val navWidth = (safe.width - navGap * 7f) / 8f
        require(navWidth >= MIN_TOUCH_SIZE) { "Viewport is too narrow for Ferrum navigation" }
        val navigation = List(8) { index ->
            Rectangle(
                safe.left + index * (navWidth + navGap),
                topBar.y - MIN_TOUCH_SIZE,
                navWidth,
                MIN_TOUCH_SIZE,
            )
        }
        val controlGap = 5f
        val center = Rectangle(safe.right - 78f, safe.bottom, 78f, MIN_TOUCH_SIZE)
        val task = Rectangle(center.x - controlGap - 88f, safe.bottom, 88f, MIN_TOUCH_SIZE)
        val action = Rectangle(task.x - controlGap - 88f, safe.bottom, 88f, MIN_TOUCH_SIZE)
        val recipe = Rectangle(action.x - controlGap - 82f, safe.bottom, 82f, MIN_TOUCH_SIZE)
        val info = Rectangle(
            safe.left,
            safe.bottom,
            (recipe.x - controlGap - safe.left).coerceAtLeast(205f),
            MIN_TOUCH_SIZE,
        )
        val layout = FerrumHudLayout(safe, topBar, navigation, info, recipe, action, task, center)
        require(layout.interactive.all { it.width >= MIN_TOUCH_SIZE && it.height >= MIN_TOUCH_SIZE })
        return layout
    }
}

object UiText {
    fun ellipsis(value: String, maxCharacters: Int): String {
        require(maxCharacters >= 1)
        if (value.length <= maxCharacters) return value
        if (maxCharacters == 1) return "…"
        return value.take(maxCharacters - 1).trimEnd() + "…"
    }
}
