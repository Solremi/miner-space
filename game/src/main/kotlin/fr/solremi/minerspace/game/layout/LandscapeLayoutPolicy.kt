package fr.solremi.minerspace.game.layout

data class ScreenInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    init {
        require(left >= 0 && top >= 0 && right >= 0 && bottom >= 0)
    }
}

data class WorldRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    init {
        require(width >= 0f && height >= 0f)
    }

    val right: Float get() = x + width
    val top: Float get() = y + height
}

data class LandscapeLayout(
    val safeArea: WorldRect,
    val topBar: WorldRect,
    val content: WorldRect,
    val bottomBar: WorldRect,
    val controls: List<WorldRect>,
    val compact: Boolean,
)

object LandscapeLayoutPolicy {
    const val MIN_TOUCH_TARGET = 48f

    fun calculate(
        worldWidth: Float,
        worldHeight: Float,
        screenWidth: Int,
        screenHeight: Int,
        insets: ScreenInsets = ScreenInsets(),
        controlCount: Int = 4,
    ): LandscapeLayout {
        require(worldWidth > 0f && worldHeight > 0f)
        require(screenWidth > 0 && screenHeight > 0)
        require(controlCount in 1..6)

        val scaleX = worldWidth / screenWidth.toFloat()
        val scaleY = worldHeight / screenHeight.toFloat()
        val left = insets.left * scaleX + 8f
        val right = (worldWidth - insets.right * scaleX - 8f).coerceAtLeast(left)
        val bottom = insets.bottom * scaleY + 8f
        val top = (worldHeight - insets.top * scaleY - 8f).coerceAtLeast(bottom)
        val safe = WorldRect(left, bottom, right - left, top - bottom)
        val compact = safe.width < 760f || safe.height < 360f
        val barHeight = if (compact) 50f else 56f
        val gap = 6f
        val controlsWidth = safe.width - gap * (controlCount - 1)
        val controlWidth = (controlsWidth / controlCount).coerceAtLeast(MIN_TOUCH_TARGET)
        val actualRowWidth = controlWidth * controlCount + gap * (controlCount - 1)
        val rowX = (safe.right - actualRowWidth).coerceAtLeast(safe.x)
        val controls = List(controlCount) { index ->
            WorldRect(
                x = rowX + index * (controlWidth + gap),
                y = safe.y,
                width = controlWidth,
                height = MIN_TOUCH_TARGET,
            )
        }
        val topBar = WorldRect(safe.x, safe.top - barHeight, safe.width, barHeight)
        val bottomBar = WorldRect(safe.x, safe.y, safe.width, MIN_TOUCH_TARGET)
        val contentBottom = bottomBar.top + gap
        val contentTop = topBar.y - gap
        val content = WorldRect(
            safe.x,
            contentBottom,
            safe.width,
            (contentTop - contentBottom).coerceAtLeast(0f),
        )
        return LandscapeLayout(safe, topBar, content, bottomBar, controls, compact)
    }
}
