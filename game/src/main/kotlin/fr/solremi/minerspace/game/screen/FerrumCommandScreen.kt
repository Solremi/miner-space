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
import fr.solremi.minerspace.game.performance.RuntimePerformanceBudgets
import fr.solremi.minerspace.game.presentation.FerrumProductionAdvice
import fr.solremi.minerspace.game.presentation.FerrumProductionAssistant
import fr.solremi.minerspace.game.presentation.PresentationController
import fr.solremi.minerspace.game.scene.FerrumColonyVisualState
import fr.solremi.minerspace.game.scene.FerrumNodeId
import fr.solremi.minerspace.game.scene.FerrumPrimitiveScene
import fr.solremi.minerspace.game.text.GameplayText
import fr.solremi.minerspace.game.ui.FerrumPlayerHudLayout
import fr.solremi.minerspace.game.ui.FerrumPlayerHudLayoutCalculator
import fr.solremi.minerspace.game.ui.FerrumPrimaryDestination
import fr.solremi.minerspace.game.ui.FerrumSecondaryDestination
import fr.solremi.minerspace.game.ui.UiInsets
import fr.solremi.minerspace.game.ui.UiText
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.text.FrenchGameText
import fr.solremi.minerspace.shared.text.GameTextKey
import ktx.app.KtxScreen

class FerrumCommandScreen(
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
    private val titleFont = BitmapFont().apply { data.setScale(0.76f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.57f) }

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
    private var batchMode = BatchMode.ONE
    private var menuOpen = false
    private var message = "NOVA · Touchez le conseil pour rejoindre la prochaine action."
    private var previousZoomDistance = 0f
    private var layout: FerrumPlayerHudLayout? = null
    private var advice = initialAdvice()
    private var lastAnnouncedDevelopmentStage = FerrumColonyVisualState.stage

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
        val tick = controller.tick()
        if (tick.autosaveFailed) message = FrenchGameText.text(GameTextKey.AUTOSAVE_DEFERRED)
        advice = FerrumProductionAssistant.evaluate(gameState, refiningDefinitions, assemblyDefinitions)
        announceDevelopmentStageIfNeeded()

        val budget = RuntimePerformanceBudgets.forQuality(PresentationController.current.quality)
        Gdx.gl.glClearColor(0.006f, 0.010f, 0.025f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        worldViewport.apply()
        worldCamera.update()
        scene.select(selected)
        scene.render(
            camera = worldCamera,
            nowMillis = services.clock.monotonicMillis().coerceAtLeast(0L),
            budget = budget,
            productionActive = gameState.refining.jobs.isNotEmpty() || gameState.assembly.jobs.isNotEmpty(),
        )
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        drawHud()
    }

    private fun announceDevelopmentStageIfNeeded() {
        val current = FerrumColonyVisualState.stage
        if (current.rank > lastAnnouncedDevelopmentStage.rank) {
            message = "NOVA · La colonie atteint le stade ${current.label}."
            services.haptic.success()
        }
        lastAnnouncedDevelopmentStage = current
    }

    private fun drawHud() {
        hudViewport.apply()
        hudCamera.update()
        val current = calculateLayout()
        layout = current

        shapes.projectionMatrix = hudCamera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP
        shapes.rect(current.top.x, current.top.y, current.top.width, current.top.height)
        shapes.color = PANEL
        shapes.rect(current.status.x, current.status.y, current.status.width, current.status.height)
        current.primaryNavigation.forEachIndexed { index, rect ->
            val active = FerrumPrimaryDestination.entries[index] == FerrumPrimaryDestination.MENU && menuOpen
            button(rect, true, if (active) ACTION_ACCENT else NAV_ACCENT)
        }
        button(current.recipe, recipeAvailable(), RECIPE_ACCENT)
        button(current.action, actionAvailable(), ACTION_ACCENT)
        button(current.task, taskAvailable(), TASK_ACCENT)
        button(current.utility, true, NAV_ACCENT)
        current.secondaryMenu.forEach { button(it, true, MENU_ACCENT) }
        shapes.end()

        batch.projectionMatrix = hudCamera.combined
        batch.begin()
        titleFont.color = TEXT
        smallFont.color = MUTED
        titleFont.draw(batch, "FERRUM DELTA", current.top.x + 10f, current.top.y + 40f)
        smallFont.draw(
            batch,
            UiText.ellipsis("${selectionTitle()} · ${economyLine()}", 52),
            current.top.x + 10f,
            current.top.y + 16f,
        )
        current.primaryNavigation.forEachIndexed { index, rect ->
            label(rect, FerrumPrimaryDestination.entries[index].label)
        }

        titleFont.draw(
            batch,
            UiText.ellipsis("${advice.progressLabel} · ${advice.title}", 48),
            current.status.x + 8f,
            current.status.y + 34f,
        )
        smallFont.draw(
            batch,
            UiText.ellipsis(advice.detail, 68),
            current.status.x + 8f,
            current.status.y + 13f,
        )

        label(current.recipe, recipeLabel())
        label(current.action, actionLabel())
        label(current.task, taskLabel())
        label(current.utility, utilityLabel())
        current.secondaryMenu.forEachIndexed { index, rect ->
            label(rect, FerrumSecondaryDestination.entries[index].label)
        }
        smallFont.color = MUTED
        smallFont.draw(
            batch,
            UiText.ellipsis(
                "${FerrumColonyVisualState.stage.label} · ${saveStatusLine()} · $message",
                100,
            ),
            current.safeArea.left,
            current.status.y + current.status.height + 13f,
        )
        batch.end()
    }

    private fun button(rect: com.badlogic.gdx.math.Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) accent else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun label(rect: com.badlogic.gdx.math.Rectangle, value: String) {
        smallFont.color = TEXT
        smallFont.draw(batch, value, rect.x + 7f, rect.y + 29f)
    }

    private fun economyLine(): String =
        "${gameState.economy.spaceDollars} SD · fer ${controller.stock(RAW_IRON)} · " +
            "cuivre ${controller.stock(RAW_COPPER)} · cristal ${controller.stock(RAW_CRYSTAL)}"

    private fun saveStatusLine(): String {
        val seconds = controller.secondsSinceLastSave() ?: return "NON SAUVEGARDÉ"
        return when {
            seconds < 5L -> "SAUVEGARDÉ"
            seconds < 60L -> "SAUV. ${seconds}s"
            else -> "SAUV. ${seconds / 60L}min"
        }
    }

    private fun selectionTitle(): String = when (selected) {
        FerrumNodeId.BASE -> "Base Delta"
        FerrumNodeId.REFINER -> "Rhea · Raffinerie"
        FerrumNodeId.ASSEMBLER -> "Kestrel · Assembleur"
        FerrumNodeId.IRON_DEPOSIT -> "Aster · Gisement de fer"
        FerrumNodeId.COPPER_DEPOSIT -> "Gisement de cuivre"
        FerrumNodeId.CRYSTAL_DEPOSIT -> "Gisement de cristal"
        null -> "Vue générale"
    }

    private fun refiningDetails(): String {
        val ready = gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val running = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING }
        return when {
            ready > 0 -> "$ready production(s) prête(s)"
            running != null -> "${refiningRecipeName(running.recipeId)} · ${remainingSeconds(running.finishesAtEpochMillis)} s"
            else -> "${selectedRefiningRecipeName()} · file ${gameState.refining.jobs.size}/${refiningDefinitions.robot.queueCapacity}"
        }
    }

    private fun assemblyDetails(): String {
        val recipe = selectedAssemblyRecipe() ?: return "Aucune recette disponible"
        if (!gameState.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)) {
            return "${selectedAssemblyRecipeName()} · technologie préalable requise"
        }
        val ready = gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }
        if (ready > 0) return "$ready production(s) prête(s)"
        return "${selectedAssemblyRecipeName()} · file ${gameState.assembly.jobs.size}/${assemblyDefinitions.robot.queueCapacity}"
    }

    private fun remainingSeconds(finish: Long): Long =
        ((finish - services.clock.nowEpochMillis()) / 1_000L).coerceAtLeast(0L)

    private fun recipeAvailable(): Boolean = selected == FerrumNodeId.REFINER || selected == FerrumNodeId.ASSEMBLER

    private fun recipeLabel(): String = when (selected) {
        FerrumNodeId.REFINER -> if (selectedRefiningRecipeId() == RECIPE_COPPER) "CUIVRE" else "FER"
        FerrumNodeId.ASSEMBLER -> when (selectedAssemblyRecipeId()) {
            ASSEMBLY_POWER_CELL -> "PILE"
            ASSEMBLY_SENSOR_ARRAY -> "CAPTEUR"
            ASSEMBLY_TECH_EXTRACTION -> "PROTOCOLE"
            ASSEMBLY_TECH_SORTING -> "TRI Q."
            else -> "RECETTE"
        }
        else -> "RECETTE"
    }

    private fun actionLabel(): String = when (selected) {
        FerrumNodeId.BASE -> "VENDRE"
        FerrumNodeId.REFINER, FerrumNodeId.ASSEMBLER -> "LANCER"
        FerrumNodeId.IRON_DEPOSIT, FerrumNodeId.COPPER_DEPOSIT, FerrumNodeId.CRYSTAL_DEPOSIT -> "PRENDRE"
        null -> "ACTION"
    }

    private fun taskLabel(): String = when (selected) {
        FerrumNodeId.BASE -> "TOUT PRENDRE"
        FerrumNodeId.REFINER -> if (gameState.refining.jobs.any { it.status == RefiningJobStatus.READY_TO_COLLECT }) "RÉCUPÉRER" else "ANNULER"
        FerrumNodeId.ASSEMBLER -> when {
            gameState.assembly.jobs.any { it.status == AssemblyJobStatus.READY_TO_COLLECT } -> "RÉCUPÉRER"
            technologyInstallAvailable() -> "INSTALLER"
            else -> "TÂCHE"
        }
        else -> "TÂCHE"
    }

    private fun utilityLabel(): String = when (selected) {
        FerrumNodeId.REFINER, FerrumNodeId.ASSEMBLER -> batchMode.label
        else -> "CENTRER"
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
        FerrumNodeId.BASE -> readyCount() > 0 || gameState.refining.refundBuffer.values.any { it > 0L }
        FerrumNodeId.REFINER -> gameState.refining.jobs.isNotEmpty()
        FerrumNodeId.ASSEMBLER -> gameState.assembly.jobs.isNotEmpty() || technologyInstallAvailable()
        else -> false
    }

    private fun readyCount(): Int =
        gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT } +
            gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }

    private fun pending(id: GameId): Long = gameState.economy.deposits.getValue(id).pendingCollection

    private fun performAction() {
        when (selected) {
            FerrumNodeId.REFINER -> launchBatch(selectedRefiningRecipeId(), refining = true)
            FerrumNodeId.ASSEMBLER -> launchBatch(selectedAssemblyRecipeId(), refining = false)
            else -> {
                val result = when (selected) {
                    FerrumNodeId.BASE -> controller.sellAll()
                    FerrumNodeId.IRON_DEPOSIT -> controller.collectDeposit(DEPOSIT_IRON)
                    FerrumNodeId.COPPER_DEPOSIT -> controller.collectDeposit(DEPOSIT_COPPER)
                    FerrumNodeId.CRYSTAL_DEPOSIT -> controller.collectDeposit(DEPOSIT_CRYSTAL)
                    else -> return
                }
                handle(result)
            }
        }
    }

    private fun launchBatch(recipeId: GameId?, refining: Boolean) {
        recipeId ?: return
        var launched = 0
        repeat(batchMode.limit) {
            val available = if (refining) controller.canLaunchRefining(recipeId) else controller.canLaunchAssembly(recipeId)
            if (!available) return@repeat
            val result = if (refining) controller.launchRefining(recipeId) else controller.launchAssembly(recipeId)
            if (result is ManufacturingActionResult.Applied) launched++
            handle(result)
        }
        if (launched > 1) message = "$launched productions ajoutées à la file."
        if (launched == 0) {
            message = if (refining) refiningDetails() else assemblyDetails()
            services.haptic.warning()
        }
    }

    private fun performTask() {
        when (selected) {
            FerrumNodeId.BASE -> collectAllAvailable()
            FerrumNodeId.REFINER -> {
                val ready = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }
                handle(if (ready != null) controller.collectRefining(ready.id) else controller.cancelRefining(gameState.refining.jobs.firstOrNull()?.id ?: return))
            }
            FerrumNodeId.ASSEMBLER -> {
                val ready = gameState.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }
                handle(if (ready != null) controller.collectAssembly(ready.id) else controller.installTechnology(selectedAssemblyTechnology()?.id ?: return))
            }
            else -> Unit
        }
    }

    private fun collectAllAvailable() {
        var collected = 0
        gameState.refining.jobs.filter { it.status == RefiningJobStatus.READY_TO_COLLECT }.map { it.id }.forEach { id ->
            if (controller.collectRefining(id) is ManufacturingActionResult.Applied) collected++
        }
        gameState.assembly.jobs.filter { it.status == AssemblyJobStatus.READY_TO_COLLECT }.map { it.id }.forEach { id ->
            if (controller.collectAssembly(id) is ManufacturingActionResult.Applied) collected++
        }
        if (gameState.refining.refundBuffer.values.any { it > 0L }) {
            if (controller.collectRefiningRefunds() is ManufacturingActionResult.Applied) collected++
        }
        message = if (collected > 0) "$collected lot(s) récupéré(s)." else "Aucune production prête."
        if (collected > 0) services.haptic.success() else services.haptic.warning()
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

    private fun cycleUtility() {
        if (selected == FerrumNodeId.REFINER || selected == FerrumNodeId.ASSEMBLER) {
            batchMode = batchMode.next()
            message = "Lot de production : ${batchMode.label}."
            services.haptic.impact()
        } else {
            recenter()
        }
    }

    private fun selectedRefiningRecipeId(): GameId? = refiningRecipeIds.getOrNull(refiningRecipeIndex)
    private fun selectedRefiningRecipeName(): String = selectedRefiningRecipeId()?.let(::refiningRecipeName) ?: "Aucune recette"
    private fun refiningRecipeName(id: GameId): String = if (id == RECIPE_IRON) "Lingots de fer" else "Plaques de cuivre"
    private fun selectedAssemblyRecipeId(): GameId? = assemblyRecipeIds.getOrNull(assemblyRecipeIndex)
    private fun selectedAssemblyRecipe() = selectedAssemblyRecipeId()?.let(assemblyDefinitions.recipes::get)
    private fun selectedAssemblyRecipeName(): String = when (selectedAssemblyRecipeId()) {
        ASSEMBLY_POWER_CELL -> "Pile énergétique"
        ASSEMBLY_SENSOR_ARRAY -> "Réseau de capteurs"
        ASSEMBLY_TECH_EXTRACTION -> "Protocole d’extraction"
        ASSEMBLY_TECH_SORTING -> "Tri quantique"
        else -> "Aucune recette"
    }

    private fun selectedAssemblyTechnology(): TechnologyDefinition? {
        val output = selectedAssemblyRecipe()?.outputResourceId ?: return null
        return assemblyDefinitions.technologies.values.firstOrNull { it.itemResourceId == output }
    }

    private fun technologyInstallAvailable(): Boolean =
        selectedAssemblyTechnology()?.let { controller.canInstallTechnology(it.id) } == true

    private fun calculateLayout(): FerrumPlayerHudLayout {
        val sx = hudViewport.worldWidth / Gdx.graphics.width.coerceAtLeast(1)
        val sy = hudViewport.worldHeight / Gdx.graphics.height.coerceAtLeast(1)
        return FerrumPlayerHudLayoutCalculator.calculate(
            width = hudViewport.worldWidth,
            height = hudViewport.worldHeight,
            insets = UiInsets(
                left = Gdx.graphics.safeInsetLeft * sx,
                right = Gdx.graphics.safeInsetRight * sx,
                bottom = Gdx.graphics.safeInsetBottom * sy,
                top = Gdx.graphics.safeInsetTop * sy,
            ),
            menuOpen = menuOpen,
        )
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
        current.primaryNavigation.forEachIndexed { index, rect ->
            if (!rect.contains(point)) return@forEachIndexed
            when (FerrumPrimaryDestination.entries[index]) {
                FerrumPrimaryDestination.EXPLORATION -> onMeteorRequested()
                FerrumPrimaryDestination.FLEET -> onRobotsRequested()
                FerrumPrimaryDestination.MISSIONS -> onMissionsRequested()
                FerrumPrimaryDestination.MENU -> menuOpen = !menuOpen
            }
            services.haptic.impact()
            return true
        }
        current.secondaryMenu.forEachIndexed { index, rect ->
            if (!rect.contains(point)) return@forEachIndexed
            menuOpen = false
            when (FerrumSecondaryDestination.entries[index]) {
                FerrumSecondaryDestination.STRATEGY -> onStrategyRequested()
                FerrumSecondaryDestination.ARCHIVES -> onArchivesRequested()
                FerrumSecondaryDestination.SETTINGS -> onPresentationRequested()
                FerrumSecondaryDestination.TRANSFER -> onTransferRequested()
                FerrumSecondaryDestination.BONUS -> onAdsRequested()
            }
            return true
        }
        return when {
            current.status.contains(point) -> {
                advice.target?.let { selected = it } ?: onMissionsRequested()
                services.haptic.impact()
                true
            }
            current.recipe.contains(point) && recipeAvailable() -> { cycleRecipe(); true }
            current.action.contains(point) && actionAvailable() -> { performAction(); true }
            current.task.contains(point) && taskAvailable() -> { performTask(); true }
            current.utility.contains(point) -> { cycleUtility(); true }
            current.top.contains(point) -> true
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
            menuOpen = false
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
            return current.top.contains(point) || current.interactive.any { it.contains(point) }
        }
    }

    private enum class BatchMode(val label: String, val limit: Int) {
        ONE("X1", 1),
        FIVE("X5", 5),
        MAX("MAX", 32),
        ;

        fun next(): BatchMode = entries[(ordinal + 1) % entries.size]
    }

    private fun initialAdvice(): FerrumProductionAdvice = FerrumProductionAdvice(
        phase = 1,
        totalPhases = 7,
        title = "Initialisation",
        detail = "NOVA analyse la chaîne de production.",
        target = FerrumNodeId.IRON_DEPOSIT,
    )

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
        val MENU_ACCENT = Color(0.42f, 0.58f, 0.94f, 1f)
        val RECIPE_ACCENT = Color(0.72f, 0.46f, 0.96f, 1f)
        val ACTION_ACCENT = Color(0.28f, 0.88f, 0.66f, 1f)
        val TASK_ACCENT = Color(0.96f, 0.62f, 0.22f, 1f)
        val TEXT = Color(0.94f, 0.97f, 1f, 1f)
        val MUTED = Color(0.62f, 0.72f, 0.82f, 1f)
    }
}
