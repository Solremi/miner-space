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
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyCommandResult
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class PlanetScreen(
    private val services: GameServices,
) : KtxScreen {
    private val worldCamera = OrthographicCamera()
    private val worldViewport = ExtendViewport(640f, 320f, 960f, 540f, worldCamera)
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(640f, 320f, 960f, 540f, hudCamera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(0.86f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.72f) }

    private val definitions = CoreEconomyContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(definitions)
    private var economyState: EconomyState = economy.initialState()
    private var selected: Selection? = null
    private var lastTickMillis = 0L
    private var remainderMillis = 0L
    private var previousZoomDistance = 0f
    private var centered = false
    private var message = "Extraction continue active"

    private val baseBounds = Rectangle(705f, 378f, 190f, 104f)
    private val deposits = listOf(
        Marker(DEPOSIT_IRON, RAW_IRON, "Fer alpha", Vector2(430f, 600f), 38f, IRON),
        Marker(DEPOSIT_COPPER, RAW_COPPER, "Cuivre bêta", Vector2(1090f, 650f), 42f, COPPER),
        Marker(DEPOSIT_CRYSTAL, RAW_CRYSTAL, "Cristal gamma", Vector2(1250f, 280f), 35f, CRYSTAL),
    )

    private val gestureListener = PlanetGestureListener()
    private val input = InputMultiplexer(GestureDetector(gestureListener))

    override fun show() {
        Gdx.input.inputProcessor = input
        lastTickMillis = services.clock.monotonicMillis()
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), false)
        hudViewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
        if (!centered) {
            recenter(false)
            centered = true
        } else {
            clampCamera()
        }
    }

    override fun render(delta: Float) {
        updateEconomy()
        ScreenUtils.clear(BACKGROUND)
        drawWorld()
        drawHud()
    }

    private fun updateEconomy() {
        val now = services.clock.monotonicMillis()
        remainderMillis = Math.addExact(remainderMillis, (now - lastTickMillis).coerceAtLeast(0L))
        lastTickMillis = now
        val seconds = remainderMillis / 1_000L
        if (seconds > 0L) {
            remainderMillis %= 1_000L
            economyState = economy.advanceExtraction(economyState, seconds).state
        }
    }

    private fun drawWorld() {
        worldViewport.apply()
        worldCamera.update()
        shapes.projectionMatrix = worldCamera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = MAP_SHADOW
        shapes.rect(18f, -18f, MAP_WIDTH, MAP_HEIGHT)
        shapes.color = MAP_GROUND
        shapes.rect(0f, 0f, MAP_WIDTH, MAP_HEIGHT)
        drawBase()
        deposits.forEach(::drawDeposit)
        drawSelection()
        shapes.end()

        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = GRID
        var offset = -MAP_HEIGHT
        while (offset <= MAP_WIDTH) {
            shapes.line(offset, 0f, offset + MAP_HEIGHT, MAP_HEIGHT)
            offset += 100f
        }
        offset = 0f
        while (offset <= MAP_WIDTH + MAP_HEIGHT) {
            shapes.line(offset, 0f, offset - MAP_HEIGHT, MAP_HEIGHT)
            offset += 100f
        }
        shapes.color = ACCENT
        shapes.rect(0f, 0f, MAP_WIDTH, MAP_HEIGHT)
        shapes.end()

        batch.projectionMatrix = worldCamera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "BASE DELTA", 754f, 445f)
        deposits.forEach { marker ->
            val state = economyState.deposits.getValue(marker.id)
            font.draw(
                batch,
                "${marker.label} · ${state.remainingReserve}",
                marker.position.x - 62f,
                marker.position.y + marker.radius + 25f,
            )
        }
        batch.end()
    }

    private fun drawBase() {
        shapes.color = SHADOW
        shapes.rect(baseBounds.x + 12f, baseBounds.y - 12f, baseBounds.width, baseBounds.height)
        shapes.color = BASE_SIDE
        shapes.rect(baseBounds.x + 7f, baseBounds.y - 7f, baseBounds.width, baseBounds.height)
        shapes.color = BASE
        shapes.rect(baseBounds.x, baseBounds.y, baseBounds.width, baseBounds.height)
        shapes.color = ACCENT
        shapes.rect(baseBounds.x + 24f, baseBounds.y + 18f, baseBounds.width - 48f, 7f)
        shapes.color = WINDOW
        shapes.rect(baseBounds.x + 80f, baseBounds.y + 39f, 30f, 28f)
    }

    private fun drawDeposit(marker: Marker) {
        val state = economyState.deposits.getValue(marker.id)
        shapes.color = SHADOW
        shapes.circle(marker.position.x + 8f, marker.position.y - 9f, marker.radius, 24)
        shapes.color = if (state.remainingReserve > 0L) marker.color else DEPLETED
        shapes.circle(marker.position.x, marker.position.y, marker.radius, 24)
        shapes.color = HIGHLIGHT
        shapes.circle(
            marker.position.x - marker.radius * 0.25f,
            marker.position.y + marker.radius * 0.28f,
            marker.radius * 0.25f,
            16,
        )
    }

    private fun drawSelection() {
        shapes.color = SELECTION
        when (val target = selected) {
            Selection.Base -> {
                shapes.rect(baseBounds.x - 6f, baseBounds.y - 6f, baseBounds.width + 12f, 4f)
                shapes.rect(baseBounds.x - 6f, baseBounds.y + baseBounds.height + 2f, baseBounds.width + 12f, 4f)
            }
            is Selection.Deposit -> {
                val marker = deposits.first { it.id == target.id }
                shapes.circle(marker.position.x, marker.position.y, marker.radius + 8f, 28)
                shapes.color = marker.color
                shapes.circle(marker.position.x, marker.position.y, marker.radius + 3f, 28)
            }
            null -> Unit
        }
    }

    private fun drawHud() {
        hudViewport.apply()
        hudCamera.update()
        val layout = hudLayout()
        shapes.projectionMatrix = hudCamera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HUD
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL
        shapes.rect(layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height)
        shapes.color = if (actionAvailable()) BUTTON else BUTTON_DISABLED
        shapes.rect(layout.action.x, layout.action.y, layout.action.width, layout.action.height)
        shapes.color = BUTTON
        shapes.rect(layout.base.x, layout.base.y, layout.base.width, layout.base.height)
        shapes.color = ACCENT
        shapes.rect(layout.action.x, layout.action.y, layout.action.width, 4f)
        shapes.rect(layout.base.x, layout.base.y, layout.base.width, 4f)
        shapes.end()

        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        font.color = TEXT
        smallFont.color = MUTED
        font.draw(batch, "MINER SPACE", layout.top.x + 12f, layout.top.y + layout.top.height - 13f)
        smallFont.draw(batch, economyLine(), layout.top.x + 12f, layout.top.y + 14f)
        smallFont.draw(
            batch,
            "${Gdx.graphics.framesPerSecond} FPS · ${(100f / worldCamera.zoom).toInt()}%",
            layout.top.x + layout.top.width - 112f,
            layout.top.y + 17f,
        )
        font.draw(batch, selectionTitle(), layout.panel.x + 12f, layout.panel.y + layout.panel.height - 13f)
        smallFont.draw(batch, selectionDetails(), layout.panel.x + 12f, layout.panel.y + 13f)
        font.draw(batch, actionLabel(), layout.action.x + 9f, layout.action.y + 31f)
        font.draw(batch, "BASE", layout.base.x + 17f, layout.base.y + 31f)
        batch.end()
    }

    private fun economyLine(): String =
        "${economyState.spaceDollars} SD · Fe ${stock(RAW_IRON)} · Cu ${stock(RAW_COPPER)} · Cr ${stock(RAW_CRYSTAL)}"

    private fun selectionTitle(): String = when (val target = selected) {
        Selection.Base -> "Base Delta"
        is Selection.Deposit -> deposits.first { it.id == target.id }.label
        null -> "Aucune sélection"
    }

    private fun selectionDetails(): String = when (val target = selected) {
        Selection.Base -> if (economyState.inventory.values.any { it > 0L }) {
            "Stock ${economyState.inventory.values.sum()} · vente atomique disponible"
        } else {
            message
        }
        is Selection.Deposit -> {
            val state = economyState.deposits.getValue(target.id)
            val rate = definitions.deposits.getValue(target.id).extractionPerSecond
            "Réserve ${state.remainingReserve} · collecte ${state.pendingCollection} · $rate/s"
        }
        null -> "Touchez un gisement · glissez · pincez"
    }

    private fun actionLabel(): String = when (val target = selected) {
        Selection.Base -> "VENDRE"
        is Selection.Deposit -> if (economyState.deposits.getValue(target.id).pendingCollection > 0L) {
            "COLLECTER"
        } else {
            "EN COURS"
        }
        null -> "ACTION"
    }

    private fun stock(resourceId: GameId): Long = economyState.inventory[resourceId] ?: 0L

    private fun actionAvailable(): Boolean = when (val target = selected) {
        Selection.Base -> economyState.inventory.values.any { it > 0L }
        is Selection.Deposit -> economyState.deposits.getValue(target.id).pendingCollection > 0L
        null -> false
    }

    private fun performAction() {
        val result = when (val target = selected) {
            Selection.Base -> economy.sellAllSellable(economyState)
            is Selection.Deposit -> economy.collect(economyState, target.id)
            null -> return
        }
        when (result) {
            is EconomyCommandResult.Applied -> {
                economyState = result.state
                message = if (result.transaction.reason == "sell_all") {
                    "+${result.transaction.spaceDollarDelta} SpaceDollars"
                } else {
                    "Collecte transférée sans duplication"
                }
                services.haptic.success()
            }
            is EconomyCommandResult.Rejected -> {
                message = result.code
                services.haptic.warning()
            }
        }
    }

    private fun hudLayout(): HudLayout {
        val width = hudViewport.worldWidth
        val height = hudViewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat()
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val compact = right - left < 760f || top - bottom < 360f
        val base = Rectangle(right - 82f, bottom, 82f, 48f)
        val actionWidth = if (compact) 102f else 116f
        val action = Rectangle(base.x - 8f - actionWidth, bottom, actionWidth, 48f)
        return HudLayout(
            top = Rectangle(left, top - if (compact) 50f else 56f, right - left, if (compact) 50f else 56f),
            panel = Rectangle(left, bottom, (action.x - 8f - left).coerceAtLeast(220f), if (compact) 58f else 64f),
            action = action,
            base = base,
        )
    }

    private fun selectAt(screenX: Float, screenY: Float) {
        val point = Vector2(screenX, screenY)
        worldViewport.unproject(point)
        val padding = 28f * worldCamera.zoom
        val marker = deposits
            .map { it to it.position.dst2(point) }
            .filter { (candidate, distance) -> distance <= max(candidate.radius, padding).let { it * it } }
            .minByOrNull { it.second }
            ?.first
        val previous = selected
        selected = when {
            marker != null -> Selection.Deposit(marker.id)
            Rectangle(
                baseBounds.x - padding,
                baseBounds.y - padding,
                baseBounds.width + padding * 2f,
                baseBounds.height + padding * 2f,
            ).contains(point) -> Selection.Base
            else -> null
        }
        if (selected != previous) services.haptic.impact()
    }

    private fun recenter(feedback: Boolean = true) {
        worldCamera.zoom = 1f
        worldCamera.position.set(800f, 430f, 0f)
        clampCamera()
        if (feedback) services.haptic.success()
    }

    private fun clampCamera() {
        val halfWidth = worldCamera.viewportWidth * worldCamera.zoom / 2f
        val halfHeight = worldCamera.viewportHeight * worldCamera.zoom / 2f
        worldCamera.position.x = clampAxis(worldCamera.position.x, MAP_WIDTH, halfWidth)
        worldCamera.position.y = clampAxis(worldCamera.position.y, MAP_HEIGHT, halfHeight)
        worldCamera.update()
    }

    private fun clampAxis(value: Float, size: Float, halfVisible: Float): Float =
        if (halfVisible * 2f >= size) size / 2f else value.coerceIn(halfVisible, size - halfVisible)

    private fun screenToHud(x: Float, y: Float): Vector2 = Vector2(x, y).also(hudViewport::unproject)

    private fun isHudPoint(x: Float, y: Float): Boolean {
        val point = screenToHud(x, y)
        val layout = hudLayout()
        return layout.top.contains(point) || layout.panel.contains(point) ||
            layout.action.contains(point) || layout.base.contains(point)
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        smallFont.dispose()
    }

    private inner class PlanetGestureListener : GestureDetector.GestureAdapter() {
        private var startedOnHud = false

        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            startedOnHud = isHudPoint(x, y)
            previousZoomDistance = 0f
            return true
        }

        override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
            val point = screenToHud(x, y)
            val layout = hudLayout()
            when {
                layout.base.contains(point) -> recenter()
                layout.action.contains(point) -> performAction()
                layout.top.contains(point) || layout.panel.contains(point) -> Unit
                else -> selectAt(x, y)
            }
            return true
        }

        override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
            if (startedOnHud) return false
            worldCamera.position.x -= deltaX * worldCamera.viewportWidth * worldCamera.zoom /
                worldViewport.screenWidth.coerceAtLeast(1)
            worldCamera.position.y += deltaY * worldCamera.viewportHeight * worldCamera.zoom /
                worldViewport.screenHeight.coerceAtLeast(1)
            clampCamera()
            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (startedOnHud || distance <= MathUtils.FLOAT_ROUNDING_ERROR) return false
            val baseline = if (previousZoomDistance <= 0f) initialDistance else previousZoomDistance
            worldCamera.zoom = (worldCamera.zoom * baseline / distance).coerceIn(0.58f, 1.55f)
            previousZoomDistance = distance
            clampCamera()
            return true
        }

        override fun pinchStop() {
            previousZoomDistance = 0f
        }
    }

    private data class Marker(
        val id: GameId,
        val resourceId: GameId,
        val label: String,
        val position: Vector2,
        val radius: Float,
        val color: Color,
    )

    private sealed interface Selection {
        data object Base : Selection
        data class Deposit(val id: GameId) : Selection
    }

    private data class HudLayout(
        val top: Rectangle,
        val panel: Rectangle,
        val action: Rectangle,
        val base: Rectangle,
    )

    private companion object {
        const val MAP_WIDTH = 1600f
        const val MAP_HEIGHT = 900f
        val RAW_IRON = GameId.of("raw_iron")
        val RAW_COPPER = GameId.of("raw_copper")
        val RAW_CRYSTAL = GameId.of("raw_crystal")
        val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
        val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
        val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")

        val BACKGROUND = Color(0.008f, 0.014f, 0.035f, 1f)
        val MAP_SHADOW = Color(0.01f, 0.01f, 0.02f, 1f)
        val MAP_GROUND = Color(0.075f, 0.095f, 0.13f, 1f)
        val GRID = Color(0.12f, 0.17f, 0.22f, 1f)
        val SHADOW = Color(0.02f, 0.025f, 0.035f, 1f)
        val BASE_SIDE = Color(0.10f, 0.15f, 0.22f, 1f)
        val BASE = Color(0.19f, 0.27f, 0.37f, 1f)
        val WINDOW = Color(0.44f, 0.91f, 0.95f, 1f)
        val IRON = Color(0.52f, 0.58f, 0.66f, 1f)
        val COPPER = Color(0.76f, 0.39f, 0.19f, 1f)
        val CRYSTAL = Color(0.42f, 0.52f, 0.94f, 1f)
        val DEPLETED = Color(0.20f, 0.22f, 0.25f, 1f)
        val HIGHLIGHT = Color(0.77f, 0.83f, 0.90f, 1f)
        val ACCENT = Color(0.20f, 0.82f, 0.88f, 1f)
        val SELECTION = Color(0.96f, 0.78f, 0.24f, 1f)
        val HUD = Color(0.025f, 0.055f, 0.10f, 1f)
        val PANEL = Color(0.035f, 0.075f, 0.13f, 1f)
        val BUTTON = Color(0.08f, 0.18f, 0.26f, 1f)
        val BUTTON_DISABLED = Color(0.05f, 0.08f, 0.11f, 1f)
        val TEXT = Color(0.90f, 0.96f, 1f, 1f)
        val MUTED = Color(0.61f, 0.72f, 0.82f, 1f)
    }
}
