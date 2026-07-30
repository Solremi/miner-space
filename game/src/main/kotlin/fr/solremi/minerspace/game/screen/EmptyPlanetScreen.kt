package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.domain.services.GameServices
import ktx.app.KtxScreen
import kotlin.math.max

/**
 * Prototype de carte 2.5D de l'étape 1.
 *
 * Le nom du fichier est conservé temporairement pour ne pas casser le point d'entrée créé à l'étape 0.
 */
class EmptyPlanetScreen(
    private val services: GameServices,
) : KtxScreen {
    private val worldCamera = OrthographicCamera()
    private val worldViewport = ExtendViewport(
        MIN_WORLD_WIDTH,
        MIN_WORLD_HEIGHT,
        MAX_WORLD_WIDTH,
        MAX_WORLD_HEIGHT,
        worldCamera,
    )

    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(
        MIN_WORLD_WIDTH,
        MIN_WORLD_HEIGHT,
        MAX_WORLD_WIDTH,
        MAX_WORLD_HEIGHT,
        hudCamera,
    )

    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val worldFont = BitmapFont().apply {
        data.setScale(0.86f)
        color = TEXT_PRIMARY
    }
    private val hudFont = BitmapFont().apply {
        data.setScale(0.94f)
        color = TEXT_PRIMARY
    }
    private val hudSmallFont = BitmapFont().apply {
        data.setScale(0.78f)
        color = TEXT_SECONDARY
    }

    private val baseBounds = Rectangle(
        BASE_POSITION.x - BASE_WIDTH / 2f,
        BASE_POSITION.y - BASE_HEIGHT / 2f,
        BASE_WIDTH,
        BASE_HEIGHT,
    )
    private val deposits = listOf(
        DepositMarker(
            id = "deposit_iron_alpha",
            label = "Fer alpha",
            position = Vector2(430f, 600f),
            radius = 38f,
            color = IRON,
        ),
        DepositMarker(
            id = "deposit_copper_beta",
            label = "Cuivre bêta",
            position = Vector2(1090f, 650f),
            radius = 42f,
            color = COPPER,
        ),
        DepositMarker(
            id = "deposit_crystal_gamma",
            label = "Cristal gamma",
            position = Vector2(1250f, 280f),
            radius = 35f,
            color = CRYSTAL,
        ),
    )

    private val gestureListener = PlanetGestureListener()
    private val gestureDetector = GestureDetector(gestureListener)
    private val inputMultiplexer = InputMultiplexer(gestureDetector)

    private var selectedTarget: SelectionTarget? = null
    private var centeredOnce = false
    private var previousZoomDistance = 0f

    override fun show() {
        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === inputMultiplexer) {
            Gdx.input.inputProcessor = null
        }
    }

    override fun resize(width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        worldViewport.update(safeWidth, safeHeight, false)
        hudViewport.update(safeWidth, safeHeight, true)

        if (!centeredOnce) {
            recenterOnBase(withFeedback = false)
            centeredOnce = true
        } else {
            clampCameraToMap()
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        drawWorld()
        drawHud()
    }

    private fun drawWorld() {
        worldViewport.apply()
        worldCamera.update()
        shapes.projectionMatrix = worldCamera.combined

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = MAP_SHADOW
        shapes.rect(
            MAP_BOUNDS.x + 18f,
            MAP_BOUNDS.y - 18f,
            MAP_BOUNDS.width,
            MAP_BOUNDS.height,
        )
        shapes.color = MAP_GROUND
        shapes.rect(MAP_BOUNDS.x, MAP_BOUNDS.y, MAP_BOUNDS.width, MAP_BOUNDS.height)

        drawBaseFilled()
        deposits.sortedByDescending { it.position.y }.forEach(::drawDepositFilled)
        drawSelectionFilled()
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        drawIsometricGrid()
        shapes.color = MAP_BORDER
        shapes.rect(MAP_BOUNDS.x, MAP_BOUNDS.y, MAP_BOUNDS.width, MAP_BOUNDS.height)
        shapes.end()

        batch.projectionMatrix = worldCamera.combined
        batch.begin()
        worldFont.draw(
            batch,
            "BASE DELTA",
            BASE_POSITION.x - 43f,
            BASE_POSITION.y + BASE_HEIGHT * 0.12f,
        )
        deposits.forEach { deposit ->
            worldFont.draw(
                batch,
                deposit.label,
                deposit.position.x - 45f,
                deposit.position.y + deposit.radius + 25f,
            )
        }
        batch.end()
    }

    private fun drawBaseFilled() {
        val x = baseBounds.x
        val y = baseBounds.y

        shapes.color = BASE_SHADOW
        shapes.rect(x + 14f, y - 14f, baseBounds.width, baseBounds.height)
        shapes.color = BASE_SIDE
        shapes.rect(x + 9f, y - 9f, baseBounds.width, baseBounds.height)
        shapes.color = BASE_MAIN
        shapes.rect(x, y, baseBounds.width, baseBounds.height)
        shapes.color = BASE_ROOF
        shapes.rect(x + 18f, y + baseBounds.height - 24f, baseBounds.width - 36f, 18f)
        shapes.color = ACCENT
        shapes.rect(x + 26f, y + 18f, baseBounds.width - 52f, 7f)
        shapes.color = WINDOW
        shapes.rect(x + baseBounds.width * 0.42f, y + 38f, baseBounds.width * 0.16f, 30f)
    }

    private fun drawDepositFilled(deposit: DepositMarker) {
        val x = deposit.position.x
        val y = deposit.position.y
        val radius = deposit.radius

        shapes.color = OBJECT_SHADOW
        shapes.circle(x + 8f, y - 9f, radius, 24)
        shapes.color = deposit.color
        shapes.circle(x, y, radius, 24)
        shapes.color = DEPOSIT_HIGHLIGHT
        shapes.circle(x - radius * 0.28f, y + radius * 0.30f, radius * 0.28f, 16)
        shapes.color = DEPOSIT_CORE
        shapes.circle(x + radius * 0.20f, y - radius * 0.10f, radius * 0.34f, 16)
    }

    private fun drawSelectionFilled() {
        when (val target = selectedTarget) {
            SelectionTarget.Base -> {
                shapes.color = SELECTION
                shapes.rect(
                    baseBounds.x - 7f,
                    baseBounds.y - 7f,
                    baseBounds.width + 14f,
                    4f,
                )
                shapes.rect(
                    baseBounds.x - 7f,
                    baseBounds.y + baseBounds.height + 3f,
                    baseBounds.width + 14f,
                    4f,
                )
                shapes.rect(
                    baseBounds.x - 7f,
                    baseBounds.y - 7f,
                    4f,
                    baseBounds.height + 14f,
                )
                shapes.rect(
                    baseBounds.x + baseBounds.width + 3f,
                    baseBounds.y - 7f,
                    4f,
                    baseBounds.height + 14f,
                )
            }

            is SelectionTarget.Deposit -> {
                val deposit = deposits.firstOrNull { it.id == target.id } ?: return
                shapes.color = SELECTION
                shapes.circle(
                    deposit.position.x,
                    deposit.position.y,
                    deposit.radius + 8f,
                    28,
                )
                shapes.color = deposit.color
                shapes.circle(
                    deposit.position.x,
                    deposit.position.y,
                    deposit.radius + 3f,
                    28,
                )
            }

            null -> Unit
        }
    }

    private fun drawIsometricGrid() {
        shapes.color = GRID_MINOR
        var offset = -MAP_BOUNDS.height
        while (offset <= MAP_BOUNDS.width) {
            shapes.line(
                MAP_BOUNDS.x + offset,
                MAP_BOUNDS.y,
                MAP_BOUNDS.x + offset + MAP_BOUNDS.height,
                MAP_BOUNDS.y + MAP_BOUNDS.height,
            )
            offset += GRID_STEP
        }

        offset = 0f
        while (offset <= MAP_BOUNDS.width + MAP_BOUNDS.height) {
            shapes.line(
                MAP_BOUNDS.x + offset,
                MAP_BOUNDS.y,
                MAP_BOUNDS.x + offset - MAP_BOUNDS.height,
                MAP_BOUNDS.y + MAP_BOUNDS.height,
            )
            offset += GRID_STEP
        }
    }

    private fun drawHud() {
        hudViewport.apply()
        hudCamera.update()
        val layout = calculateHudLayout()

        shapes.projectionMatrix = hudCamera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HUD_BACKGROUND
        shapes.rect(
            layout.topBar.x,
            layout.topBar.y,
            layout.topBar.width,
            layout.topBar.height,
        )

        shapes.color = HUD_PANEL
        shapes.rect(
            layout.selectionPanel.x,
            layout.selectionPanel.y,
            layout.selectionPanel.width,
            layout.selectionPanel.height,
        )

        shapes.color = if (layout.baseButton.contains(layout.lastPointerHud)) BUTTON_ACTIVE else BUTTON
        shapes.rect(
            layout.baseButton.x,
            layout.baseButton.y,
            layout.baseButton.width,
            layout.baseButton.height,
        )
        shapes.color = ACCENT
        shapes.rect(
            layout.baseButton.x,
            layout.baseButton.y,
            layout.baseButton.width,
            4f,
        )
        shapes.end()

        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        hudFont.color = TEXT_PRIMARY
        hudFont.draw(
            batch,
            "MINER SPACE",
            layout.topBar.x + 12f,
            layout.topBar.y + layout.topBar.height - 14f,
        )

        hudSmallFont.color = TEXT_SECONDARY
        hudSmallFont.draw(
            batch,
            "Carte Ferrum Delta",
            layout.topBar.x + 12f,
            layout.topBar.y + 15f,
        )

        val zoomPercent = (100f / worldCamera.zoom).toInt()
        val status = if (layout.compact) {
            "${Gdx.graphics.framesPerSecond} FPS  ·  ${zoomPercent}%"
        } else {
            "${Gdx.graphics.framesPerSecond} FPS  ·  Zoom ${zoomPercent}%  ·  3 gisements"
        }
        hudSmallFont.draw(
            batch,
            status,
            layout.topBar.x + layout.topBar.width - if (layout.compact) 124f else 222f,
            layout.topBar.y + 18f,
        )

        val selectionTitle = when (val target = selectedTarget) {
            SelectionTarget.Base -> "Base Delta"
            is SelectionTarget.Deposit -> deposits.firstOrNull { it.id == target.id }?.label
                ?: "Gisement"
            null -> "Aucune sélection"
        }
        val selectionHint = when (selectedTarget) {
            SelectionTarget.Base -> "Centre logistique temporaire"
            is SelectionTarget.Deposit -> "Gisement détecté · extraction bientôt disponible"
            null -> if (layout.compact) {
                "Touchez · glissez · pincez"
            } else {
                "Touchez un objet · glissez pour déplacer · pincez pour zoomer"
            }
        }

        hudFont.draw(
            batch,
            selectionTitle,
            layout.selectionPanel.x + 12f,
            layout.selectionPanel.y + layout.selectionPanel.height - 14f,
        )
        hudSmallFont.draw(
            batch,
            selectionHint,
            layout.selectionPanel.x + 12f,
            layout.selectionPanel.y + 13f,
        )

        hudFont.draw(
            batch,
            "BASE",
            layout.baseButton.x + 17f,
            layout.baseButton.y + layout.baseButton.height / 2f + 6f,
        )
        batch.end()
    }

    private fun calculateHudLayout(): HudLayout {
        val worldWidth = hudViewport.worldWidth
        val worldHeight = hudViewport.worldHeight
        val screenWidth = Gdx.graphics.width.coerceAtLeast(1)
        val screenHeight = Gdx.graphics.height.coerceAtLeast(1)
        val scaleX = worldWidth / screenWidth.toFloat()
        val scaleY = worldHeight / screenHeight.toFloat()

        val safeLeft = Gdx.graphics.safeInsetLeft * scaleX + SAFE_MARGIN
        val safeRight = Gdx.graphics.safeInsetRight * scaleX + SAFE_MARGIN
        val safeTop = Gdx.graphics.safeInsetTop * scaleY + SAFE_MARGIN
        val safeBottom = Gdx.graphics.safeInsetBottom * scaleY + SAFE_MARGIN

        val left = safeLeft
        val right = max(left + 1f, worldWidth - safeRight)
        val bottom = safeBottom
        val top = max(bottom + 1f, worldHeight - safeTop)
        val availableWidth = right - left
        val compact = availableWidth < 760f || top - bottom < 360f

        val topBarHeight = if (compact) 50f else 56f
        val panelHeight = if (compact) 58f else 64f
        val baseButtonWidth = 82f
        val baseButton = Rectangle(
            right - baseButtonWidth,
            bottom,
            baseButtonWidth,
            MIN_TOUCH_TARGET,
        )
        val selectionPanelWidth = if (compact) {
            (availableWidth - baseButtonWidth - HUD_GAP).coerceAtLeast(220f)
        } else {
            (availableWidth * 0.48f).coerceIn(300f, 430f)
        }

        return HudLayout(
            topBar = Rectangle(left, top - topBarHeight, availableWidth, topBarHeight),
            selectionPanel = Rectangle(left, bottom, selectionPanelWidth, panelHeight),
            baseButton = baseButton,
            compact = compact,
            lastPointerHud = screenToHud(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()),
        )
    }

    private fun selectAt(screenX: Float, screenY: Float) {
        val worldPoint = Vector2(screenX, screenY)
        worldViewport.unproject(worldPoint)

        val previousSelection = selectedTarget
        val effectivePadding = MIN_SELECTION_RADIUS * worldCamera.zoom
        val deposit = deposits
            .asSequence()
            .map { marker -> marker to marker.position.dst2(worldPoint) }
            .filter { (marker, distanceSquared) ->
                val radius = max(marker.radius, effectivePadding)
                distanceSquared <= radius * radius
            }
            .minByOrNull { (_, distanceSquared) -> distanceSquared }
            ?.first

        selectedTarget = when {
            deposit != null -> SelectionTarget.Deposit(deposit.id)
            expandedBaseBounds(effectivePadding).contains(worldPoint) -> SelectionTarget.Base
            else -> null
        }

        if (selectedTarget != previousSelection) {
            services.haptic.impact()
        }
    }

    private fun expandedBaseBounds(padding: Float): Rectangle = Rectangle(
        baseBounds.x - padding,
        baseBounds.y - padding,
        baseBounds.width + padding * 2f,
        baseBounds.height + padding * 2f,
    )

    private fun recenterOnBase(withFeedback: Boolean = true) {
        worldCamera.zoom = DEFAULT_ZOOM
        worldCamera.position.set(BASE_POSITION.x, BASE_POSITION.y, 0f)
        clampCameraToMap()
        if (withFeedback) {
            services.haptic.success()
        }
    }

    private fun clampCameraToMap() {
        val visibleHalfWidth = worldCamera.viewportWidth * worldCamera.zoom / 2f
        val visibleHalfHeight = worldCamera.viewportHeight * worldCamera.zoom / 2f

        worldCamera.position.x = clampAxis(
            value = worldCamera.position.x,
            minimum = MAP_BOUNDS.x,
            maximum = MAP_BOUNDS.x + MAP_BOUNDS.width,
            visibleHalfSize = visibleHalfWidth,
        )
        worldCamera.position.y = clampAxis(
            value = worldCamera.position.y,
            minimum = MAP_BOUNDS.y,
            maximum = MAP_BOUNDS.y + MAP_BOUNDS.height,
            visibleHalfSize = visibleHalfHeight,
        )
        worldCamera.update()
    }

    private fun clampAxis(
        value: Float,
        minimum: Float,
        maximum: Float,
        visibleHalfSize: Float,
    ): Float {
        val size = maximum - minimum
        return if (visibleHalfSize * 2f >= size) {
            minimum + size / 2f
        } else {
            value.coerceIn(minimum + visibleHalfSize, maximum - visibleHalfSize)
        }
    }

    private fun screenToHud(screenX: Float, screenY: Float): Vector2 {
        val point = Vector2(screenX, screenY)
        hudViewport.unproject(point)
        return point
    }

    private fun isHudPoint(screenX: Float, screenY: Float): Boolean {
        val point = screenToHud(screenX, screenY)
        val layout = calculateHudLayout()
        return layout.topBar.contains(point) ||
            layout.selectionPanel.contains(point) ||
            layout.baseButton.contains(point)
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        worldFont.dispose()
        hudFont.dispose()
        hudSmallFont.dispose()
    }

    private inner class PlanetGestureListener : GestureDetector.GestureAdapter() {
        private var gestureStartedOnHud = false

        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            gestureStartedOnHud = isHudPoint(x, y)
            previousZoomDistance = 0f
            return true
        }

        override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
            val hudPoint = screenToHud(x, y)
            val layout = calculateHudLayout()
            when {
                layout.baseButton.contains(hudPoint) -> recenterOnBase()
                layout.topBar.contains(hudPoint) || layout.selectionPanel.contains(hudPoint) -> Unit
                else -> selectAt(x, y)
            }
            return true
        }

        override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
            if (gestureStartedOnHud) return false

            val screenWidth = worldViewport.screenWidth.coerceAtLeast(1)
            val screenHeight = worldViewport.screenHeight.coerceAtLeast(1)
            val worldPerPixelX = worldCamera.viewportWidth * worldCamera.zoom / screenWidth
            val worldPerPixelY = worldCamera.viewportHeight * worldCamera.zoom / screenHeight

            worldCamera.position.x -= deltaX * worldPerPixelX
            worldCamera.position.y += deltaY * worldPerPixelY
            clampCameraToMap()
            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (gestureStartedOnHud || distance <= MathUtils.FLOAT_ROUNDING_ERROR) return false

            val baseline = if (previousZoomDistance <= 0f) initialDistance else previousZoomDistance
            if (baseline > MathUtils.FLOAT_ROUNDING_ERROR) {
                worldCamera.zoom = (worldCamera.zoom * baseline / distance)
                    .coerceIn(MIN_ZOOM, MAX_ZOOM)
                previousZoomDistance = distance
                clampCameraToMap()
            }
            return true
        }

        override fun pinchStop() {
            previousZoomDistance = 0f
        }
    }

    private data class DepositMarker(
        val id: String,
        val label: String,
        val position: Vector2,
        val radius: Float,
        val color: Color,
    )

    private sealed interface SelectionTarget {
        data object Base : SelectionTarget

        data class Deposit(
            val id: String,
        ) : SelectionTarget
    }

    private data class HudLayout(
        val topBar: Rectangle,
        val selectionPanel: Rectangle,
        val baseButton: Rectangle,
        val compact: Boolean,
        val lastPointerHud: Vector2,
    )

    private companion object {
        const val MIN_WORLD_WIDTH = 640f
        const val MIN_WORLD_HEIGHT = 320f
        const val MAX_WORLD_WIDTH = 960f
        const val MAX_WORLD_HEIGHT = 540f

        const val MAP_WIDTH = 1600f
        const val MAP_HEIGHT = 900f
        const val GRID_STEP = 100f
        const val SAFE_MARGIN = 8f
        const val HUD_GAP = 8f
        const val MIN_TOUCH_TARGET = 48f
        const val MIN_SELECTION_RADIUS = 28f
        const val MIN_ZOOM = 0.58f
        const val MAX_ZOOM = 1.55f
        const val DEFAULT_ZOOM = 1f
        const val BASE_WIDTH = 190f
        const val BASE_HEIGHT = 104f

        val MAP_BOUNDS = Rectangle(0f, 0f, MAP_WIDTH, MAP_HEIGHT)
        val BASE_POSITION = Vector2(800f, 430f)

        val BACKGROUND = Color(0.008f, 0.014f, 0.035f, 1f)
        val MAP_SHADOW = Color(0.01f, 0.01f, 0.02f, 1f)
        val MAP_GROUND = Color(0.075f, 0.095f, 0.13f, 1f)
        val MAP_BORDER = Color(0.20f, 0.82f, 0.88f, 1f)
        val GRID_MINOR = Color(0.12f, 0.17f, 0.22f, 1f)
        val BASE_SHADOW = Color(0.015f, 0.02f, 0.03f, 1f)
        val BASE_SIDE = Color(0.10f, 0.15f, 0.22f, 1f)
        val BASE_MAIN = Color(0.19f, 0.27f, 0.37f, 1f)
        val BASE_ROOF = Color(0.29f, 0.38f, 0.48f, 1f)
        val WINDOW = Color(0.44f, 0.91f, 0.95f, 1f)
        val OBJECT_SHADOW = Color(0.02f, 0.025f, 0.035f, 1f)
        val IRON = Color(0.52f, 0.58f, 0.66f, 1f)
        val COPPER = Color(0.76f, 0.39f, 0.19f, 1f)
        val CRYSTAL = Color(0.42f, 0.52f, 0.94f, 1f)
        val DEPOSIT_HIGHLIGHT = Color(0.77f, 0.83f, 0.90f, 1f)
        val DEPOSIT_CORE = Color(0.11f, 0.15f, 0.23f, 1f)
        val ACCENT = Color(0.20f, 0.82f, 0.88f, 1f)
        val SELECTION = Color(0.96f, 0.78f, 0.24f, 1f)
        val HUD_BACKGROUND = Color(0.025f, 0.055f, 0.10f, 1f)
        val HUD_PANEL = Color(0.035f, 0.075f, 0.13f, 1f)
        val BUTTON = Color(0.08f, 0.18f, 0.26f, 1f)
        val BUTTON_ACTIVE = Color(0.12f, 0.28f, 0.36f, 1f)
        val TEXT_PRIMARY = Color(0.90f, 0.96f, 1f, 1f)
        val TEXT_SECONDARY = Color(0.61f, 0.72f, 0.82f, 1f)
    }
}
