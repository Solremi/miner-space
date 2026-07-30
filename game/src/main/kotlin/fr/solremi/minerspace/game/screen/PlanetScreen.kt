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
import fr.solremi.minerspace.data.refining.RefiningContentLoader
import fr.solremi.minerspace.data.save.GameStateSnapshotCodec
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyCommandResult
import fr.solremi.minerspace.domain.refining.RefiningCommandResult
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.refining.RefiningGameState
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
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
    private val font = BitmapFont().apply { data.setScale(0.82f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.68f) }

    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val refiningDefinitions = RefiningContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val refiner = RefiningEngine(
        refiningDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )
    private val snapshotCodec = GameStateSnapshotCodec()
    private var message = "Extraction et raffinage actifs"
    private var gameState = loadState()
    private var selected: Selection? = null
    private var selectedRecipeIndex = 0
    private var lastLaunchedRecipeId: GameId? = null
    private var lastTickMillis = 0L
    private var remainderMillis = 0L
    private var lastAutosaveMillis = 0L
    private var previousZoomDistance = 0f
    private var centered = false

    private val baseBounds = Rectangle(690f, 378f, 190f, 104f)
    private val refinerBounds = Rectangle(910f, 390f, 138f, 86f)
    private val deposits = listOf(
        Marker(DEPOSIT_IRON, RAW_IRON, "Fer alpha", Vector2(430f, 600f), 38f, IRON),
        Marker(DEPOSIT_COPPER, RAW_COPPER, "Cuivre bêta", Vector2(1090f, 650f), 42f, COPPER),
        Marker(DEPOSIT_CRYSTAL, RAW_CRYSTAL, "Cristal gamma", Vector2(1250f, 280f), 35f, CRYSTAL),
    )
    private val recipeIds = listOf(RECIPE_IRON, RECIPE_COPPER)
        .filter { refiningDefinitions.recipes.containsKey(it) }

    private val lifecycleObserver = LifecycleObserver { state ->
        if (state == LifecycleState.BACKGROUND) saveState()
    }
    private val gestureListener = PlanetGestureListener()
    private val input = InputMultiplexer(GestureDetector(gestureListener))

    override fun show() {
        Gdx.input.inputProcessor = input
        services.lifecycle.addObserver(lifecycleObserver)
        lastTickMillis = services.clock.monotonicMillis()
        lastAutosaveMillis = lastTickMillis
        gameState = refiner.reconcile(gameState, services.clock.nowEpochMillis())
    }

    override fun hide() {
        saveState()
        services.lifecycle.removeObserver(lifecycleObserver)
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
        updateSimulation()
        ScreenUtils.clear(BACKGROUND)
        drawWorld()
        drawHud()
    }

    private fun loadState(): RefiningGameState {
        val initial = RefiningGameState(economy.initialState(), RefiningState.empty())
        val payload = services.save.loadLatest() ?: return initial
        return runCatching {
            require(payload.contentVersion == economyDefinitions.contentVersion)
            require(refiningDefinitions.contentVersion == economyDefinitions.contentVersion)
            val restored = snapshotCodec.decode(payload)
            economy.requireValid(restored.economy)
            refiner.reconcile(restored, services.clock.nowEpochMillis())
        }.getOrElse {
            message = "Sauvegarde incompatible ignorée"
            initial
        }
    }

    private fun saveState(): Boolean {
        val payload = snapshotCodec.encode(gameState, economyDefinitions.contentVersion)
        return when (services.save.save(payload)) {
            SaveWriteStatus.WRITTEN -> true
            SaveWriteStatus.REJECTED, SaveWriteStatus.FAILED -> false
        }
    }

    private fun updateSimulation() {
        val monotonicNow = services.clock.monotonicMillis()
        remainderMillis = Math.addExact(
            remainderMillis,
            (monotonicNow - lastTickMillis).coerceAtLeast(0L),
        )
        lastTickMillis = monotonicNow
        var changed = false
        val seconds = remainderMillis / 1_000L
        if (seconds > 0L) {
            remainderMillis %= 1_000L
            val extraction = economy.advanceExtraction(gameState.economy, seconds)
            if (extraction.state != gameState.economy) {
                gameState = gameState.copy(economy = extraction.state)
                changed = true
            }
        }
        val reconciled = refiner.reconcile(gameState, services.clock.nowEpochMillis())
        if (reconciled != gameState) {
            gameState = reconciled
            changed = true
        }
        if (changed && monotonicNow - lastAutosaveMillis >= AUTOSAVE_INTERVAL_MILLIS) {
            saveState()
            lastAutosaveMillis = monotonicNow
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
        drawRefiner()
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
        drawRefinerEffect()
        shapes.color = ACCENT
        shapes.rect(0f, 0f, MAP_WIDTH, MAP_HEIGHT)
        shapes.end()

        batch.projectionMatrix = worldCamera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "BASE DELTA", 739f, 445f)
        font.draw(batch, "RF-01", refinerBounds.x + 42f, refinerBounds.y + 54f)
        deposits.forEach { marker ->
            val state = gameState.economy.deposits.getValue(marker.id)
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

    private fun drawRefiner() {
        val active = gameState.refining.jobs.any { it.status == RefiningJobStatus.RUNNING }
        shapes.color = SHADOW
        shapes.rect(refinerBounds.x + 10f, refinerBounds.y - 10f, refinerBounds.width, refinerBounds.height)
        shapes.color = if (active) REFINER_ACTIVE else REFINER
        shapes.rect(refinerBounds.x, refinerBounds.y, refinerBounds.width, refinerBounds.height)
        shapes.color = BASE_SIDE
        shapes.rect(refinerBounds.x + 12f, refinerBounds.y + 12f, 32f, refinerBounds.height - 24f)
        shapes.rect(refinerBounds.x + refinerBounds.width - 44f, refinerBounds.y + 12f, 32f, refinerBounds.height - 24f)
        shapes.color = if (active) HOT else WINDOW
        shapes.circle(refinerBounds.x + refinerBounds.width / 2f, refinerBounds.y + 28f, 13f, 20)
    }

    private fun drawRefinerEffect() {
        val running = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING } ?: return
        val now = services.clock.nowEpochMillis()
        val duration = (running.finishesAtEpochMillis - running.startsAtEpochMillis).coerceAtLeast(1L)
        val elapsed = (now - running.startsAtEpochMillis).coerceIn(0L, duration)
        val progress = elapsed.toFloat() / duration.toFloat()
        val pulse = 4f + 5f * ((MathUtils.sin((services.clock.monotonicMillis() % 1_200L) / 1_200f * MathUtils.PI2) + 1f) / 2f)
        shapes.color = HOT
        shapes.circle(
            refinerBounds.x + refinerBounds.width / 2f,
            refinerBounds.y + refinerBounds.height / 2f,
            28f + pulse,
            28,
        )
        shapes.line(
            refinerBounds.x + 12f,
            refinerBounds.y + 7f,
            refinerBounds.x + 12f + (refinerBounds.width - 24f) * progress,
            refinerBounds.y + 7f,
        )
    }

    private fun drawDeposit(marker: Marker) {
        val state = gameState.economy.deposits.getValue(marker.id)
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
            Selection.Base -> drawRectSelection(baseBounds)
            Selection.Refiner -> drawRectSelection(refinerBounds)
            is Selection.Deposit -> {
                val marker = deposits.first { it.id == target.id }
                shapes.circle(marker.position.x, marker.position.y, marker.radius + 8f, 28)
                shapes.color = marker.color
                shapes.circle(marker.position.x, marker.position.y, marker.radius + 3f, 28)
            }
            null -> Unit
        }
    }

    private fun drawRectSelection(bounds: Rectangle) {
        shapes.rect(bounds.x - 6f, bounds.y - 6f, bounds.width + 12f, 4f)
        shapes.rect(bounds.x - 6f, bounds.y + bounds.height + 2f, bounds.width + 12f, 4f)
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
        drawButton(layout.recipe, selected == Selection.Refiner)
        drawButton(layout.launch, launchAvailable())
        drawButton(layout.task, taskAvailable())
        drawButton(layout.base, true)
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
        drawButtonLabel(layout.recipe, recipeLabel())
        drawButtonLabel(layout.launch, launchLabel())
        drawButtonLabel(layout.task, taskLabel())
        drawButtonLabel(layout.base, "BASE")
        batch.end()
    }

    private fun drawButton(rectangle: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else BUTTON_DISABLED
        shapes.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height)
        shapes.color = if (enabled) ACCENT else GRID
        shapes.rect(rectangle.x, rectangle.y, rectangle.width, 4f)
    }

    private fun drawButtonLabel(rectangle: Rectangle, label: String) {
        smallFont.color = TEXT
        smallFont.draw(batch, label, rectangle.x + 7f, rectangle.y + 29f)
    }

    private fun economyLine(): String =
        "${gameState.economy.spaceDollars} SD · Fe ${stock(RAW_IRON)} · Cu ${stock(RAW_COPPER)} · Lg ${stock(REFINED_IRON)} · Pl ${stock(REFINED_COPPER)}"

    private fun selectionTitle(): String = when (val target = selected) {
        Selection.Base -> "Base Delta"
        Selection.Refiner -> "Raffineur RF-01"
        is Selection.Deposit -> deposits.first { it.id == target.id }.label
        null -> "Aucune sélection"
    }

    private fun selectionDetails(): String = when (val target = selected) {
        Selection.Base -> if (gameState.refining.refundBuffer.isNotEmpty()) {
            "Remboursement en attente ${gameState.refining.refundBuffer.values.sum()} · libérez du stockage"
        } else if (gameState.economy.inventory.values.any { it > 0L }) {
            "Stock ${gameState.economy.inventory.values.sum()} · vente disponible"
        } else {
            message
        }
        Selection.Refiner -> refiningDetails()
        is Selection.Deposit -> {
            val state = gameState.economy.deposits.getValue(target.id)
            val rate = economyDefinitions.deposits.getValue(target.id).extractionPerSecond
            "Réserve ${state.remainingReserve} · collecte ${state.pendingCollection} · $rate/s"
        }
        null -> "Touchez un gisement, la base ou RF-01"
    }

    private fun refiningDetails(): String {
        val jobs = gameState.refining.jobs
        val ready = jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val running = jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING }
        val queued = jobs.count { it.status == RefiningJobStatus.QUEUED }
        return when {
            ready > 0 -> "$ready produit(s) prêt(s) · $queued en file · collecte conservée"
            running != null -> {
                val remaining = ((running.finishesAtEpochMillis - services.clock.nowEpochMillis()) / 1_000L)
                    .coerceAtLeast(0L)
                "${recipeShortName(running.recipeId)} · ${remaining}s · $queued en file"
            }
            queued > 0 -> "$queued tâche(s) en file"
            else -> "${selectedRecipeName()} · ${selectedRecipe().durationSeconds}s · file vide"
        }
    }

    private fun stock(resourceId: GameId): Long = gameState.economy.inventory[resourceId] ?: 0L

    private fun recipeLabel(): String = if (selected == Selection.Refiner) {
        if (selectedRecipeIndex == 0) "REC. FER" else "REC. CUIV."
    } else {
        "RECETTE"
    }

    private fun launchLabel(): String = when (selected) {
        Selection.Base -> "VENDRE"
        Selection.Refiner -> if (lastLaunchedRecipeId == selectedRecipeId()) "RELANCER" else "LANCER"
        is Selection.Deposit -> "COLLECTER"
        null -> "ACTION"
    }

    private fun taskLabel(): String = when {
        selected == Selection.Base && gameState.refining.refundBuffer.isNotEmpty() -> "REMBOURS."
        selected == Selection.Refiner && gameState.refining.jobs.any { it.status == RefiningJobStatus.READY_TO_COLLECT } -> "COLLECTER"
        selected == Selection.Refiner && gameState.refining.jobs.isNotEmpty() -> "ANNULER"
        else -> "TÂCHE"
    }

    private fun launchAvailable(): Boolean = when (val target = selected) {
        Selection.Base -> gameState.economy.inventory.values.any { it > 0L }
        Selection.Refiner -> canLaunch(selectedRecipeId())
        is Selection.Deposit -> gameState.economy.deposits.getValue(target.id).pendingCollection > 0L
        null -> false
    }

    private fun taskAvailable(): Boolean = when (selected) {
        Selection.Base -> gameState.refining.refundBuffer.isNotEmpty()
        Selection.Refiner -> gameState.refining.jobs.isNotEmpty()
        else -> false
    }

    private fun canLaunch(recipeId: GameId): Boolean {
        if (gameState.refining.jobs.size >= refiningDefinitions.robot.queueCapacity) return false
        val recipe = refiningDefinitions.recipes.getValue(recipeId)
        return recipe.inputs.all { (resourceId, quantity) -> stock(resourceId) >= quantity }
    }

    private fun performLaunchAction() {
        when (val target = selected) {
            Selection.Base -> applyEconomyResult(economy.sellAllSellable(gameState.economy))
            Selection.Refiner -> applyRefiningResult(
                refiner.launch(gameState, selectedRecipeId(), services.clock.nowEpochMillis()),
                launchedRecipeId = selectedRecipeId(),
            )
            is Selection.Deposit -> applyEconomyResult(economy.collect(gameState.economy, target.id))
            null -> Unit
        }
    }

    private fun performTaskAction() {
        val result = when (selected) {
            Selection.Base -> refiner.collectRefunds(gameState)
            Selection.Refiner -> {
                val ready = gameState.refining.jobs.firstOrNull {
                    it.status == RefiningJobStatus.READY_TO_COLLECT
                }
                if (ready != null) {
                    refiner.collect(gameState, ready.id, services.clock.nowEpochMillis())
                } else {
                    val cancellable = gameState.refining.jobs.firstOrNull()
                        ?: return
                    refiner.cancel(gameState, cancellable.id, services.clock.nowEpochMillis())
                }
            }
            else -> return
        }
        applyRefiningResult(result)
    }

    private fun applyEconomyResult(result: EconomyCommandResult) {
        when (result) {
            is EconomyCommandResult.Applied -> {
                gameState = gameState.copy(economy = result.state)
                message = when (result.transaction.reason) {
                    "sell_all" -> "+${result.transaction.spaceDollarDelta} SpaceDollars"
                    "collect" -> "Collecte transférée sans duplication"
                    else -> result.transaction.reason
                }
                persistSuccessfulAction()
            }
            is EconomyCommandResult.Rejected -> rejectAction(result.code)
        }
    }

    private fun applyRefiningResult(
        result: RefiningCommandResult,
        launchedRecipeId: GameId? = null,
    ) {
        when (result) {
            is RefiningCommandResult.Applied -> {
                gameState = result.state
                if (launchedRecipeId != null) lastLaunchedRecipeId = launchedRecipeId
                message = when {
                    result.transaction.reason == "launch_refining" -> "Ingrédients réservés · tâche ajoutée"
                    result.transaction.reason.startsWith("cancel_refining") -> "Tâche annulée · remboursement appliqué"
                    result.transaction.reason == "collect_refining" -> "Produit raffiné collecté"
                    result.transaction.reason == "collect_refunds" -> "Remboursement collecté"
                    else -> result.transaction.reason
                }
                persistSuccessfulAction()
            }
            is RefiningCommandResult.Rejected -> rejectAction(result.code)
        }
    }

    private fun persistSuccessfulAction() {
        if (saveState()) {
            services.haptic.success()
        } else {
            message = "Action appliquée · sauvegarde locale échouée"
            services.haptic.warning()
        }
    }

    private fun rejectAction(code: String) {
        message = when (code) {
            "output_storage_full" -> "Stockage de sortie plein · produit conservé"
            "queue_full" -> "File RF pleine"
            "nothing_to_collect", "job_not_completed" -> "Rien à collecter"
            else -> code
        }
        services.haptic.warning()
    }

    private fun cycleRecipe() {
        if (selected != Selection.Refiner || recipeIds.isEmpty()) return
        selectedRecipeIndex = (selectedRecipeIndex + 1) % recipeIds.size
        services.haptic.impact()
    }

    private fun selectedRecipeId(): GameId = recipeIds[selectedRecipeIndex]
    private fun selectedRecipe() = refiningDefinitions.recipes.getValue(selectedRecipeId())
    private fun selectedRecipeName(): String = recipeShortName(selectedRecipeId())
    private fun recipeShortName(recipeId: GameId): String = when (recipeId) {
        RECIPE_IRON -> "Lingots de fer"
        RECIPE_COPPER -> "Plaques de cuivre"
        else -> recipeId.value
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
        val gap = 6f
        val base = Rectangle(right - 72f, bottom, 72f, 48f)
        val task = Rectangle(base.x - gap - 92f, bottom, 92f, 48f)
        val launch = Rectangle(task.x - gap - 92f, bottom, 92f, 48f)
        val recipe = Rectangle(launch.x - gap - 86f, bottom, 86f, 48f)
        return HudLayout(
            top = Rectangle(left, top - if (compact) 50f else 56f, right - left, if (compact) 50f else 56f),
            panel = Rectangle(left, bottom, (recipe.x - gap - left).coerceAtLeast(190f), if (compact) 58f else 64f),
            recipe = recipe,
            launch = launch,
            task = task,
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
            expanded(refinerBounds, padding).contains(point) -> Selection.Refiner
            expanded(baseBounds, padding).contains(point) -> Selection.Base
            else -> null
        }
        if (selected != previous) services.haptic.impact()
    }

    private fun expanded(bounds: Rectangle, padding: Float): Rectangle = Rectangle(
        bounds.x - padding,
        bounds.y - padding,
        bounds.width + padding * 2f,
        bounds.height + padding * 2f,
    )

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
            layout.recipe.contains(point) || layout.launch.contains(point) ||
            layout.task.contains(point) || layout.base.contains(point)
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
                layout.recipe.contains(point) -> cycleRecipe()
                layout.launch.contains(point) && launchAvailable() -> performLaunchAction()
                layout.task.contains(point) && taskAvailable() -> performTaskAction()
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
        data object Refiner : Selection
        data class Deposit(val id: GameId) : Selection
    }

    private data class HudLayout(
        val top: Rectangle,
        val panel: Rectangle,
        val recipe: Rectangle,
        val launch: Rectangle,
        val task: Rectangle,
        val base: Rectangle,
    )

    private companion object {
        const val MAP_WIDTH = 1600f
        const val MAP_HEIGHT = 900f
        const val AUTOSAVE_INTERVAL_MILLIS = 5_000L
        val RAW_IRON = GameId.of("raw_iron")
        val RAW_COPPER = GameId.of("raw_copper")
        val RAW_CRYSTAL = GameId.of("raw_crystal")
        val REFINED_IRON = GameId.of("refined_iron_ingot")
        val REFINED_COPPER = GameId.of("refined_copper_plate")
        val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
        val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
        val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")
        val RECIPE_IRON = GameId.of("recipe_iron_ingot")
        val RECIPE_COPPER = GameId.of("recipe_copper_plate")

        val BACKGROUND = Color(0.008f, 0.014f, 0.035f, 1f)
        val MAP_SHADOW = Color(0.01f, 0.01f, 0.02f, 1f)
        val MAP_GROUND = Color(0.075f, 0.095f, 0.13f, 1f)
        val GRID = Color(0.12f, 0.17f, 0.22f, 1f)
        val SHADOW = Color(0.02f, 0.025f, 0.035f, 1f)
        val BASE_SIDE = Color(0.10f, 0.15f, 0.22f, 1f)
        val BASE = Color(0.19f, 0.27f, 0.37f, 1f)
        val REFINER = Color(0.22f, 0.24f, 0.31f, 1f)
        val REFINER_ACTIVE = Color(0.34f, 0.25f, 0.19f, 1f)
        val HOT = Color(1f, 0.47f, 0.16f, 1f)
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
