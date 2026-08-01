package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.manufacturing.ManufacturingActionResult
import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.game.performance.FrameTimeMonitor
import fr.solremi.minerspace.game.performance.RuntimePerformanceBudgets
import fr.solremi.minerspace.game.presentation.PresentationController
import fr.solremi.minerspace.game.scene.FerrumNodeId
import fr.solremi.minerspace.game.scene.FerrumPrimitiveScene
import fr.solremi.minerspace.game.text.GameplayText
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.text.FrenchGameText
import fr.solremi.minerspace.shared.text.GameTextKey
import ktx.app.KtxScreen
import kotlin.math.max

class FerrumVerticalSliceScreen(
    private val services: GameServices,
    private val onMeteorRequested: () -> Unit,
    private val onRobotsRequested: () -> Unit,
    private val onStrategyRequested: () -> Unit,
    private val onMissionsRequested: () -> Unit,
    private val onArchivesRequested: () -> Unit,
    private val onPresentationRequested: () -> Unit,
    private val onTransferRequested: () -> Unit,
    private val onAdsRequested: () -> Unit,
) : KtxScreen {
    private val controller = ManufacturingCoordinator.fromServices(services)
    private val refiningDefinitions = controller.refiningDefinitions
    private val assemblyDefinitions = controller.assemblyDefinitions
    private val worldCamera = OrthographicCamera()
    private val worldViewport = ExtendViewport(24f, 13.5f, 30f, 18f, worldCamera)
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(640f, 320f, 960f, 540f, hudCamera)
    private val scene = FerrumPrimitiveScene()
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val titleFont = BitmapFont().apply { data.setScale(0.74f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.56f) }
    private val frameTimes = FrameTimeMonitor()

    private val refiningRecipeIds = listOf(RECIPE_IRON, RECIPE_COPPER)
        .filter(refiningDefinitions.recipes::containsKey)
    private val assemblyRecipeIds = listOf(
        ASSEMBLY_POWER_CELL,
        ASSEMBLY_SENSOR_ARRAY,
        ASSEMBLY_TECH_EXTRACTION,
        ASSEMBLY_TECH_SORTING,
    ).filter(assemblyDefinitions.recipes::containsKey)

    private var selected: FerrumNodeId? = FerrumNodeId.BASE
    private var refiningRecipeIndex = 0
    private var assemblyRecipeIndex = 0
    private var message = "Vertical slice Ferrum Delta"
    private var previousZoomDistance = 0f
    private var layout: Layout? = null
    private var lastFrameSnapshotAt = 0L
    private var p95Millis = 0.0

    private val lifecycle = LifecycleObserver { state ->
        if (state == LifecycleState.BACKGROUND && !controller.save()) {
            message = FrenchGameText.text(GameTextKey.SAVE_DEFERRED)
        }
    }
    private val gestures = GestureDetector(Gestures())
    private val input = InputMultiplexer(gestures)
    private val gameState get() = controller.state

    override fun show() {
        controller.start()
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
        recenter(false)
    }

    override fun hide() {
        if (!controller.save()) message = FrenchGameText.text(GameTextKey.SAVE_DEFERRED)
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), false)
        hudViewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
        worldCamera.viewportWidth = worldViewport.worldWidth
        worldCamera.viewportHeight = worldViewport.worldHeight
        worldCamera.update()
    }

    override fun render(delta: Float) {
        frameTimes.record((delta.coerceAtLeast(0f) * 1_000_000_000L).toLong())
        val now = services.clock.monotonicMillis().coerceAtLeast(0L)
        if (now - lastFrameSnapshotAt >= 1_000L) {
            p95Millis = frameTimes.snapshot().p95Millis
            lastFrameSnapshotAt = now
        }
        val tick = controller.tick()
        if (tick.autosaveFailed) message = FrenchGameText.text(GameTextKey.AUTOSAVE_DEFERRED)

        val budget = RuntimePerformanceBudgets.forQuality(PresentationController.current.quality)
        Gdx.gl.glClearColor(0.006f, 0.010f, 0.025f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        worldViewport.apply()
        worldCamera.update()
        scene.select(selected)
        scene.render(
            camera = worldCamera,
            nowMillis = now,
            budget = budget,
            productionActive = gameState.refining.jobs.isNotEmpty() || gameState.assembly.jobs.isNotEmpty(),
        )
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        drawHud(budget.maxVisibleRobots, budget.maxParticles)
    }

    private fun drawHud(maxRobots: Int, maxParticles: Int) {
        hudViewport.apply()
        hudCamera.update()
        val current = calculateLayout()
        layout = current
        shapes.projectionMatrix = hudCamera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP
        shapes.rect(current.top.x, current.top.y, current.top.width, current.top.height)
        shapes.color = PANEL
        shapes.rect(current.info.x, current.info.y, current.info.width, current.info.height)
        current.navigation.forEach { button(it, true, NAV_ACCENT) }
        button(current.recipe, recipeAvailable(), RECIPE_ACCENT)
        button(current.action, actionAvailable(), ACTION_ACCENT)
        button(current.task, taskAvailable(), TASK_ACCENT)
        button(current.center, true, NAV_ACCENT)
        shapes.end()

        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        titleFont.color = TEXT
        smallFont.color = MUTED
        titleFont.draw(batch, "FERRUM DELTA · VERTICAL SLICE 2.5D", current.top.x + 10f, current.top.y + current.top.height - 11f)
        smallFont.draw(batch, economyLine(), current.top.x + 10f, current.top.y + 13f)
        smallFont.draw(
            batch,
            "${Gdx.graphics.framesPerSecond} FPS · P95 ${"%.1f".format(p95Millis)} ms · robots $maxRobots · particules $maxParticles",
            current.top.x + current.top.width - 245f,
            current.top.y + 13f,
        )
        titleFont.draw(batch, selectionTitle(), current.info.x + 10f, current.info.y + current.info.height - 10f)
        smallFont.draw(batch, selectionDetails(), current.info.x + 10f, current.info.y + 14f)
        smallFont.draw(batch, message, current.info.x + 230f, current.info.y + 14f)
        val navLabels = listOf("MÉT.", "ROBOTS", "STRAT.", "MISS.", "ARCH.", "FX", "DÉPART", "PUB")
        current.navigation.forEachIndexed { index, rect -> label(rect, navLabels[index]) }
        label(current.recipe, recipeLabel())
        label(current.action, actionLabel())
        label(current.task, taskLabel())
        label(current.center, "CENTRER")
        batch.end()
    }

    private fun button(rect: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) accent else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun label(rect: Rectangle, value: String) {
        smallFont.color = TEXT
        smallFont.draw(batch, value, rect.x + 6f, rect.y + 27f)
    }

    private fun economyLine(): String =
        "${gameState.economy.spaceDollars} SD · Fe ${controller.stock(RAW_IRON)} · " +
            "Cu ${controller.stock(RAW_COPPER)} · cristal ${controller.stock(RAW_CRYSTAL)} · " +
            "tech ${gameState.assembly.installedTechnologyIds.size}"

    private fun selectionTitle(): String = when (selected) {
        FerrumNodeId.BASE -> "Base Delta"
        FerrumNodeId.REFINER -> "Raffineur RF-01"
        FerrumNodeId.ASSEMBLER -> "Assembleur AS-01"
        FerrumNodeId.IRON_DEPOSIT -> "Gisement de fer"
        FerrumNodeId.COPPER_DEPOSIT -> "Gisement de cuivre"
        FerrumNodeId.CRYSTAL_DEPOSIT -> "Gisement de cristal"
        null -> "Aucune sélection"
    }

    private fun selectionDetails(): String = when (selected) {
        FerrumNodeId.BASE -> "Vendez le stock ou récupérez les remboursements de raffinage."
        FerrumNodeId.REFINER -> refiningDetails()
        FerrumNodeId.ASSEMBLER -> assemblyDetails()
        FerrumNodeId.IRON_DEPOSIT -> depositDetails(DEPOSIT_IRON)
        FerrumNodeId.COPPER_DEPOSIT -> depositDetails(DEPOSIT_COPPER)
        FerrumNodeId.CRYSTAL_DEPOSIT -> depositDetails(DEPOSIT_CRYSTAL)
        null -> "Touchez une installation ou un gisement."
    }

    private fun depositDetails(id: GameId): String {
        val deposit = gameState.economy.deposits.getValue(id)
        return "Réserve ${deposit.remainingReserve} · collecte ${deposit.pendingCollection}"
    }

    private fun refiningDetails(): String {
        val ready = gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val running = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING }
        return when {
            ready > 0 -> "$ready production(s) prête(s)"
            running != null -> "${refiningRecipeName(running.recipeId)} · ${remainingSeconds(running.finishesAtEpochMillis)} s"
            refiningRecipeIds.isNotEmpty() -> "${selectedRefiningRecipeName()} · file ${gameState.refining.jobs.size}/${refiningDefinitions.robot.queueCapacity}"
            else -> "Aucune recette de raffinage disponible"
        }
    }

    private fun assemblyDetails(): String {
        val recipe = selectedAssemblyRecipe() ?: return "Aucune recette d’assemblage disponible"
        if (!gameState.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)) {
            return "${selectedAssemblyRecipeName()} · technologie préalable requise"
        }
        val ready = gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }
        if (ready > 0) return "$ready production(s) prête(s)"
        val technology = selectedAssemblyTechnology()
        return when {
            technology == null -> "${selectedAssemblyRecipeName()} · file ${gameState.assembly.jobs.size}/${assemblyDefinitions.robot.queueCapacity}"
            technology.id in gameState.assembly.installedTechnologyIds -> "${selectedAssemblyRecipeName()} · installée"
            else -> "${selectedAssemblyRecipeName()} · prête à fabriquer puis installer"
        }
    }

    private fun remainingSeconds(finish: Long): Long =
        ((finish - services.clock.nowEpochMillis()) / 1_000L).coerceAtLeast(0L)

    private fun recipeAvailable(): Boolean = selected == FerrumNodeId.REFINER || selected == FerrumNodeId.ASSEMBLER

    private fun recipeLabel(): String = when (selected) {
        FerrumNodeId.REFINER -> if (selectedRefiningRecipeId() == RECIPE_COPPER) "REC. CUIV." else "REC. FER"
        FerrumNodeId.ASSEMBLER -> when (selectedAssemblyRecipeId()) {
            ASSEMBLY_POWER_CELL -> "PILE"
            ASSEMBLY_SENSOR_ARRAY -> "CAPTEUR"
            ASSEMBLY_TECH_EXTRACTION -> "TECH 1"
            ASSEMBLY_TECH_SORTING -> "TECH 2"
            else -> "RECETTE"
        }
        else -> "RECETTE"
    }

    private fun actionLabel(): String = when (selected) {
        FerrumNodeId.BASE -> "VENDRE"
        FerrumNodeId.REFINER, FerrumNodeId.ASSEMBLER -> "LANCER"
        FerrumNodeId.IRON_DEPOSIT, FerrumNodeId.COPPER_DEPOSIT, FerrumNodeId.CRYSTAL_DEPOSIT -> "COLLECTER"
        null -> "ACTION"
    }

    private fun taskLabel(): String = when (selected) {
        FerrumNodeId.BASE -> "REMBOURS."
        FerrumNodeId.REFINER -> if (gameState.refining.jobs.any { it.status == RefiningJobStatus.READY_TO_COLLECT }) "COLLECTER" else "ANNULER"
        FerrumNodeId.ASSEMBLER -> when {
            gameState.assembly.jobs.any { it.status == AssemblyJobStatus.READY_TO_COLLECT } -> "COLLECTER"
            technologyInstallAvailable() -> "INSTALLER"
            else -> "TÂCHE"
        }
        else -> "TÂCHE"
    }

    private fun actionAvailable(): Boolean = when (selected) {
        FerrumNodeId.BASE -> gameState.economy.inventory.values.any { it > 0L }
        FerrumNodeId.REFINER -> selectedRefiningRecipeId()?.let(controller::canLaunchRefining) == true
        FerrumNodeId.ASSEMBLER -> selectedAssemblyRecipeId()?.let(controller::canLaunchAssembly) == true
        FerrumNodeId.IRON_DEPOSIT -> pending(DEPOSIT_IRON) > 0L
        FerrumNodeId.COPPER_DEPOSIT -> pending(DEPOSIT_COPPER) > 0L
        FerrumNodeId.CRYSTAL_DEPOSIT -> pending(DEPOSIT_CRYSTAL) > 0L
        null -> false
    }

    private fun taskAvailable(): Boolean = when (selected) {
        FerrumNodeId.BASE -> gameState.refining.refundBuffer.isNotEmpty()
        FerrumNodeId.REFINER -> gameState.refining.jobs.isNotEmpty()
        FerrumNodeId.ASSEMBLER -> gameState.assembly.jobs.isNotEmpty() || technologyInstallAvailable()
        else -> false
    }

    private fun pending(id: GameId): Long = gameState.economy.deposits.getValue(id).pendingCollection

    private fun performAction() {
        val result = when (selected) {
            FerrumNodeId.BASE -> controller.sellAll()
            FerrumNodeId.REFINER -> selectedRefiningRecipeId()?.let(controller::launchRefining) ?: return
            FerrumNodeId.ASSEMBLER -> selectedAssemblyRecipeId()?.let(controller::launchAssembly) ?: return
            FerrumNodeId.IRON_DEPOSIT -> controller.collectDeposit(DEPOSIT_IRON)
            FerrumNodeId.COPPER_DEPOSIT -> controller.collectDeposit(DEPOSIT_COPPER)
            FerrumNodeId.CRYSTAL_DEPOSIT -> controller.collectDeposit(DEPOSIT_CRYSTAL)
            null -> return
        }
        handle(result)
    }

    private fun performTask() {
        val result = when (selected) {
            FerrumNodeId.BASE -> controller.collectRefiningRefunds()
            FerrumNodeId.REFINER -> {
                val ready = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }
                if (ready != null) controller.collectRefining(ready.id)
                else controller.cancelRefining(gameState.refining.jobs.firstOrNull()?.id ?: return)
            }
            FerrumNodeId.ASSEMBLER -> {
                val ready = gameState.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }
                if (ready != null) controller.collectAssembly(ready.id)
                else controller.installTechnology(selectedAssemblyTechnology()?.id ?: return)
            }
            else -> return
        }
        handle(result)
    }

    private fun handle(result: ManufacturingActionResult) {
        when (result) {
            is ManufacturingActionResult.Applied -> {
                message = GameplayText.manufacturingSuccess(result.reason)
                services.haptic.success()
            }
            is ManufacturingActionResult.Rejected -> {
                message = GameplayText.manufacturingError(result.code)
                services.haptic.warning()
            }
            is ManufacturingActionResult.PersistenceFailed -> {
                message = FrenchGameText.text(GameTextKey.ACTION_CANCELLED_SAVE_UNAVAILABLE)
                services.haptic.warning()
            }
        }
    }

    private fun cycleRecipe() {
        when (selected) {
            FerrumNodeId.REFINER -> if (refiningRecipeIds.isNotEmpty()) refiningRecipeIndex = (refiningRecipeIndex + 1) % refiningRecipeIds.size
            FerrumNodeId.ASSEMBLER -> if (assemblyRecipeIds.isNotEmpty()) assemblyRecipeIndex = (assemblyRecipeIndex + 1) % assemblyRecipeIds.size
            else -> return
        }
        services.haptic.impact()
    }

    private fun selectedRefiningRecipeId(): GameId? = refiningRecipeIds.getOrNull(refiningRecipeIndex)
    private fun selectedRefiningRecipeName(): String = selectedRefiningRecipeId()?.let(::refiningRecipeName) ?: "Aucune recette"
    private fun refiningRecipeName(id: GameId): String = if (id == RECIPE_IRON) "Lingots de fer" else "Plaques de cuivre"
    private fun selectedAssemblyRecipeId(): GameId? = assemblyRecipeIds.getOrNull(assemblyRecipeIndex)
    private fun selectedAssemblyRecipe() = selectedAssemblyRecipeId()?.let(assemblyDefinitions.recipes::get)
    private fun selectedAssemblyRecipeName(): String = when (selectedAssemblyRecipeId()) {
        ASSEMBLY_POWER_CELL -> "Pile énergétique"
        ASSEMBLY_SENSOR_ARRAY -> "Réseau capteur"
        ASSEMBLY_TECH_EXTRACTION -> "Protocole extraction"
        ASSEMBLY_TECH_SORTING -> "Tri quantique"
        else -> "Aucune recette"
    }

    private fun selectedAssemblyTechnology(): TechnologyDefinition? {
        val output = selectedAssemblyRecipe()?.outputResourceId ?: return null
        return assemblyDefinitions.technologies.values.firstOrNull { it.itemResourceId == output }
    }

    private fun technologyInstallAvailable(): Boolean =
        selectedAssemblyTechnology()?.let { controller.canInstallTechnology(it.id) } == true

    private fun calculateLayout(): Layout {
        val safe = safeArea()
        val top = Rectangle(safe.left, safe.top - 50f, safe.width, 50f)
        val navY = top.y - 48f
        val navGap = 4f
        val navWidth = (safe.width - navGap * 7f) / 8f
        val navigation = List(8) { index -> Rectangle(safe.left + index * (navWidth + navGap), navY, navWidth, 44f) }
        val controlsY = safe.bottom
        val gap = 5f
        val center = Rectangle(safe.right - 78f, controlsY, 78f, 48f)
        val task = Rectangle(center.x - gap - 88f, controlsY, 88f, 48f)
        val action = Rectangle(task.x - gap - 88f, controlsY, 88f, 48f)
        val recipe = Rectangle(action.x - gap - 82f, controlsY, 82f, 48f)
        val info = Rectangle(safe.left, controlsY, (recipe.x - gap - safe.left).coerceAtLeast(205f), 48f)
        return Layout(top, navigation, info, recipe, action, task, center)
    }

    private fun safeArea(): SafeArea {
        val width = hudViewport.worldWidth
        val height = hudViewport.worldHeight
        val sx = width / Gdx.graphics.width.coerceAtLeast(1)
        val sy = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * sy - 8f)
        return SafeArea(left, right, bottom, top)
    }

    private fun recenter(feedback: Boolean = true) {
        worldCamera.position.set(12.5f, 13f, 12.5f)
        worldCamera.up.set(Vector3.Y)
        worldCamera.lookAt(0f, 0f, 0f)
        worldCamera.near = 0.1f
        worldCamera.far = 80f
        worldCamera.zoom = 1f
        worldCamera.update()
        if (feedback) services.haptic.success()
    }

    private fun hudPoint(x: Float, y: Float): Vector2 = Vector2(x, y).also(hudViewport::unproject)

    private fun tapHud(point: Vector2): Boolean {
        val current = layout ?: calculateLayout()
        current.navigation.forEachIndexed { index, rect ->
            if (rect.contains(point)) {
                when (index) {
                    0 -> onMeteorRequested()
                    1 -> onRobotsRequested()
                    2 -> onStrategyRequested()
                    3 -> onMissionsRequested()
                    4 -> onArchivesRequested()
                    5 -> onPresentationRequested()
                    6 -> onTransferRequested()
                    7 -> onAdsRequested()
                }
                return true
            }
        }
        return when {
            current.recipe.contains(point) && recipeAvailable() -> { cycleRecipe(); true }
            current.action.contains(point) && actionAvailable() -> { performAction(); true }
            current.task.contains(point) && taskAvailable() -> { performTask(); true }
            current.center.contains(point) -> { recenter(); true }
            current.top.contains(point) || current.info.contains(point) -> true
            else -> false
        }
    }

    override fun dispose() {
        hide()
        scene.dispose()
        shapes.dispose()
        batch.dispose()
        titleFont.dispose()
        smallFont.dispose()
    }

    private inner class Gestures : GestureDetector.GestureAdapter() {
        private var startedOnHud = false

        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            startedOnHud = isHudRegion(hudPoint(x, y))
            previousZoomDistance = 0f
            return true
        }

        override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
            if (tapHud(hudPoint(x, y))) return true
            val ray = worldCamera.getPickRay(x, y, 0f, 0f, Gdx.graphics.width, Gdx.graphics.height)
            val next = scene.pick(ray)
            if (next != selected) services.haptic.impact()
            selected = next
            return true
        }

        override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
            if (startedOnHud) return false
            val scale = 0.018f * worldCamera.zoom
            worldCamera.translate(-deltaX * scale, 0f, -deltaY * scale)
            worldCamera.position.x = worldCamera.position.x.coerceIn(7f, 17f)
            worldCamera.position.z = worldCamera.position.z.coerceIn(7f, 17f)
            worldCamera.update()
            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (startedOnHud || distance <= MathUtils.FLOAT_ROUNDING_ERROR) return false
            val baseline = if (previousZoomDistance <= 0f) initialDistance else previousZoomDistance
            worldCamera.zoom = (worldCamera.zoom * baseline / distance).coerceIn(0.65f, 1.45f)
            previousZoomDistance = distance
            worldCamera.update()
            return true
        }

        override fun pinchStop() {
            previousZoomDistance = 0f
        }

        private fun isHudRegion(point: Vector2): Boolean {
            val current = layout ?: calculateLayout()
            return current.top.contains(point) || current.info.contains(point) ||
                current.recipe.contains(point) || current.action.contains(point) ||
                current.task.contains(point) || current.center.contains(point) ||
                current.navigation.any { it.contains(point) }
        }
    }

    private data class Layout(
        val top: Rectangle,
        val navigation: List<Rectangle>,
        val info: Rectangle,
        val recipe: Rectangle,
        val action: Rectangle,
        val task: Rectangle,
        val center: Rectangle,
    )

    private data class SafeArea(val left: Float, val right: Float, val bottom: Float, val top: Float) {
        val width: Float get() = right - left
    }

    private companion object {
        val RAW_IRON = GameId.of("raw_iron")
        val RAW_COPPER = GameId.of("raw_copper")
        val RAW_CRYSTAL = GameId.of("raw_crystal")
        val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
        val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
        val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")
        val RECIPE_IRON = GameId.of("recipe_iron_ingot")
        val RECIPE_COPPER = GameId.of("recipe_copper_plate")
        val ASSEMBLY_POWER_CELL = GameId.of("assembly_power_cell")
        val ASSEMBLY_SENSOR_ARRAY = GameId.of("assembly_sensor_array")
        val ASSEMBLY_TECH_EXTRACTION = GameId.of("assembly_tech_extraction_protocol")
        val ASSEMBLY_TECH_SORTING = GameId.of("assembly_tech_quantum_sorting")

        val TOP = Color(0.020f, 0.047f, 0.085f, 0.97f)
        val PANEL = Color(0.030f, 0.070f, 0.120f, 0.97f)
        val BUTTON = Color(0.065f, 0.145f, 0.215f, 0.98f)
        val DISABLED = Color(0.030f, 0.050f, 0.072f, 0.98f)
        val GRID = Color(0.14f, 0.19f, 0.24f, 1f)
        val NAV_ACCENT = Color(0.30f, 0.72f, 0.96f, 1f)
        val RECIPE_ACCENT = Color(0.72f, 0.46f, 0.96f, 1f)
        val ACTION_ACCENT = Color(0.28f, 0.88f, 0.66f, 1f)
        val TASK_ACCENT = Color(0.96f, 0.62f, 0.22f, 1f)
        val TEXT = Color(0.94f, 0.97f, 1f, 1f)
        val MUTED = Color(0.62f, 0.72f, 0.82f, 1f)
    }
}
