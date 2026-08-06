package fr.solremi.minerspace.game.ui

import com.badlogic.gdx.math.Rectangle

enum class FerrumPrimaryDestination(val label: String) {
    EXPLORATION("EXPLORER"),
    FLEET("FLOTTE"),
    MISSIONS("MISSIONS"),
    MENU("MENU"),
}

enum class FerrumSecondaryDestination(val label: String) {
    STRATEGY("STRATÉGIE"),
    ARCHIVES("ARCHIVES"),
    SETTINGS("RÉGLAGES"),
    TRANSFER("DÉPART"),
    BONUS("BONUS"),
}

data class FerrumPlayerHudLayout(
    val safeArea: UiSafeArea,
    val top: Rectangle,
    val primaryNavigation: List<Rectangle>,
    val status: Rectangle,
    val recipe: Rectangle,
    val action: Rectangle,
    val task: Rectangle,
    val utility: Rectangle,
    val secondaryMenu: List<Rectangle>,
) {
    val interactive: List<Rectangle> get() =
        primaryNavigation + listOf(status, recipe, action, task, utility) + secondaryMenu
}

object FerrumPlayerHudLayoutCalculator {
    const val MIN_TOUCH_SIZE = 48f

    fun calculate(
        width: Float,
        height: Float,
        insets: UiInsets = UiInsets(),
        menuOpen: Boolean = false,
        margin: Float = 8f,
    ): FerrumPlayerHudLayout {
        require(width > 0f && height > 0f)
        val safe = UiSafeArea(
            left = insets.left + margin,
            right = (width - insets.right - margin).coerceAtLeast(insets.left + margin + 1f),
            bottom = insets.bottom + margin,
            top = (height - insets.top - margin).coerceAtLeast(insets.bottom + margin + 1f),
        )

        val topBar = Rectangle(safe.left, safe.top - 56f, safe.width, 56f)
        val navGap = 4f
        val navWidth = ((safe.width * 0.58f) - navGap * 3f) / 4f
        val navStart = safe.right - (navWidth * 4f + navGap * 3f)
        val primary = List(FerrumPrimaryDestination.entries.size) { index ->
            Rectangle(navStart + index * (navWidth + navGap), topBar.y + 4f, navWidth, MIN_TOUCH_SIZE)
        }

        val controlsY = safe.bottom
        val controlGap = 5f
        val controlWidth = 82f
        val utility = Rectangle(safe.right - controlWidth, controlsY, controlWidth, MIN_TOUCH_SIZE)
        val task = Rectangle(utility.x - controlGap - controlWidth, controlsY, controlWidth, MIN_TOUCH_SIZE)
        val action = Rectangle(task.x - controlGap - controlWidth, controlsY, controlWidth, MIN_TOUCH_SIZE)
        val recipe = Rectangle(action.x - controlGap - controlWidth, controlsY, controlWidth, MIN_TOUCH_SIZE)
        val status = Rectangle(
            safe.left,
            controlsY,
            (recipe.x - controlGap - safe.left).coerceAtLeast(210f),
            MIN_TOUCH_SIZE,
        )

        val secondary = if (menuOpen) {
            val widthMenu = 132f
            val gap = 4f
            val columns = 2
            val totalWidth = widthMenu * columns + gap
            val startX = safe.right - totalWidth
            FerrumSecondaryDestination.entries.mapIndexed { index, _ ->
                val column = index % columns
                val row = index / columns
                Rectangle(
                    startX + column * (widthMenu + gap),
                    topBar.y - (row + 1) * (MIN_TOUCH_SIZE + gap),
                    widthMenu,
                    MIN_TOUCH_SIZE,
                )
            }
        } else {
            emptyList()
        }

        return FerrumPlayerHudLayout(
            safeArea = safe,
            top = topBar,
            primaryNavigation = primary,
            status = status,
            recipe = recipe,
            action = action,
            task = task,
            utility = utility,
            secondaryMenu = secondary,
        )
    }
}
