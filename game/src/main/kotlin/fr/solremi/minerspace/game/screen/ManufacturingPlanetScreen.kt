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
import fr.solremi.minerspace.data.assembly.AssemblyContentLoader
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.refining.RefiningContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.domain.assembly.AssemblyCommandResult
import fr.solremi.minerspace.domain.assembly.AssemblyEngine
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.EconomyCommandResult
import fr.solremi.minerspace.domain.economy.advanceExtraction
import fr.solremi.minerspace.domain.refining.RefiningCommandResult
import fr.solremi.minerspace.domain.refining.RefiningEngine
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class ManufacturingPlanetScreen(
    private val services: GameServices,
) : KtxScreen {
    private val worldCamera = OrthographicCamera()
    private val worldViewport = ExtendViewport(640f, 320f, 960f, 540f, worldCamera)
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(640f, 320f, 960f, 540f, hudCamera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(0.80f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.64f) }

    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val refiningDefinitions = RefiningContentLoader().load(services.content)
    private val assemblyDefinitions = AssemblyContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val refiner = RefiningEngine(
        refiningDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )
    private val assembler = AssemblyEngine(
        assemblyDefinitions,
        economyDefinitions.resources.mapValues { it.value.storageCapacity },
    )
    private val snapshotCodec = ManufacturingSnapshotCodec()

    private var gameState = loadState()
    private var selected: Selection? = null
    private var refiningRecipeIndex = 0
    private var assemblyRecipeIndex = 0
    private var message = "Chaîne brute → technologie active"
    private var lastTickMillis = 0L
    private var remainderMillis = 0L
    private var lastAutosaveMillis = 0L
    private var previousZoomDistance = 0f
    private var centered = false

    private val baseBounds = Rectangle(690f, 378f, 190f, 104f)
    private val assemblerBounds = Rectangle(520f, 390f, 138f, 86f)
    private val refinerBounds = Rectangle(910f, 390f, 138f, 86f)
    private val deposits = listOf(
        Marker(DEPOSIT_IRON, "Fer", Vector2(430f, 600f), 38f, IRON),
        Marker(DEPOSIT_COPPER, "Cuivre", Vector2(1090f, 650f), 42f, COPPER),
        Marker(DEPOSIT_CRYSTAL, "Cristal", Vector2(1250f, 280f), 35f, CRYSTAL),
    )
    private val refiningRecipeIds = listOf(RECIPE_IRON, RECIPE_COPPER)
        .filter(refiningDefinitions.recipes::containsKey)
    private val assemblyRecipeIds = listOf(
        ASSEMBLY_POWER_CELL,
        ASSEMBLY_SENSOR_ARRAY,
        ASSEMBLY_TECH_EXTRACTION,
        ASSEMBLY_TECH_SORTING,
    ).filter(assemblyDefinitions.recipes::containsKey)

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
        reconcileProduction()
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

    private fun initialState(): ManufacturingGameState = ManufacturingGameState(
        economy.initialState(),
        RefiningState.empty(),
        AssemblyState.empty(),
    )

    private fun loadState(): ManufacturingGameState {
        val initial = initialState()
        val payload = services.save.loadLatest() ?: return initial
        return runCatching {
            require(payload.contentVersion == economyDefinitions.contentVersion)
            require(refiningDefinitions.contentVersion == payload.contentVersion)
            require(assemblyDefinitions.contentVersion == payload.contentVersion)
            val restored = snapshotCodec.decode(payload)
            economy.requireValid(restored.economy)
            val withRefining = restored.withRefining(
                refiner.reconcile(restored.refiningView(), services.clock.nowEpochMillis()),
            )
            assembler.reconcile(withRefining, services.clock.nowEpochMillis())
        }.getOrElse {
            message = "Sauvegarde antérieure ignorée"
            initial
        }
    }

    private fun saveState(): Boolean = services.save.save(
        snapshotCodec.encode(gameState, economyDefinitions.contentVersion),
    ) == SaveWriteStatus.WRITTEN

    private fun reconcileProduction() {
        val now = services.clock.nowEpochMillis()
        gameState = gameState.withRefining(refiner.reconcile(gameState.refiningView(), now))
        gameState = assembler.reconcile(gameState, now)
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
            val extraction = economy.advanceExtraction(
                gameState.economy,
                seconds,
                assembler.productionMultipliers(gameState.assembly),
            )
            if (extraction.state != gameState.economy) {
                gameState = gameState.copy(economy = extraction.state)
                changed = true
            }
        }
        val before = gameState
        reconcileProduction()
        if (before != gameState) changed = true
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
        drawMachine(baseBounds, BASE, false)
        drawMachine(assemblerBounds, ASSEMBLER, gameState.assembly.jobs.any { it.status == AssemblyJobStatus.RUNNING })
        drawMachine(refinerBounds, REFINER, gameState.refining.jobs.any { it.status == RefiningJobStatus.RUNNING })
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
        drawProgress(refinerBounds, refiningProgress(), HOT)
        drawProgress(assemblerBounds, assemblyProgress(), TECH_GLOW)
        shapes.color = ACCENT
        shapes.rect(0f, 0f, MAP_WIDTH, MAP_HEIGHT)
        shapes.end()

        batch.projectionMatrix = worldCamera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "BASE DELTA", 739f, 445f)
        font.draw(batch, "AS-01", assemblerBounds.x + 39f, assemblerBounds.y + 54f)
        font.draw(batch, "RF-01", refinerBounds.x + 42f, refinerBounds.y + 54f)
        deposits.forEach { marker ->
            val state = gameState.economy.deposits.getValue(marker.id)
            font.draw(batch, "${marker.label} · ${state.remainingReserve}", marker.position.x - 55f, marker.position.y + marker.radius + 24f)
        }
        batch.end()
    }

    private fun drawMachine(bounds: Rectangle, color: Color, active: Boolean) {
        shapes.color = SHADOW
        shapes.rect(bounds.x + 9f, bounds.y - 9f, bounds.width, bounds.height)
        shapes.color = if (active) TECH_GLOW else color
        shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
        shapes.color = BASE_SIDE
        shapes.rect(bounds.x + 12f, bounds.y + 12f, 28f, bounds.height - 24f)
        shapes.rect(bounds.x + bounds.width - 40f, bounds.y + 12f, 28f, bounds.height - 24f)
        shapes.color = if (active) TECH_GLOW else WINDOW
        shapes.circle(bounds.x + bounds.width / 2f, bounds.y + 26f, 12f, 18)
    }

    private fun drawDeposit(marker: Marker) {
        val state = gameState.economy.deposits.getValue(marker.id)
        shapes.color = SHADOW
        shapes.circle(marker.position.x + 8f, marker.position.y - 9f, marker.radius, 24)
        shapes.color = if (state.remainingReserve > 0L) marker.color else DEPLETED
        shapes.circle(marker.position.x, marker.position.y, marker.radius, 24)
        shapes.color = HIGHLIGHT
        shapes.circle(marker.position.x - marker.radius * 0.25f, marker.position.y + marker.radius * 0.28f, marker.radius * 0.25f, 16)
    }

    private fun drawSelection() {
        shapes.color = SELECTION
        when (val target = selected) {
            Selection.Base -> drawRectSelection(baseBounds)
            Selection.Assembler -> drawRectSelection(assemblerBounds)
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

    private fun drawProgress(bounds: Rectangle, progress: Float?, color: Color) {
        if (progress == null) return
        shapes.color = color
        shapes.line(bounds.x + 10f, bounds.y + 7f, bounds.x + 10f + (bounds.width - 20f) * progress, bounds.y + 7f)
    }

    private fun refiningProgress(): Float? {
        val job = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING } ?: return null
        return progress(job.startsAtEpochMillis, job.finishesAtEpochMillis)
    }

    private fun assemblyProgress(): Float? {
        val job = gameState.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.RUNNING } ?: return null
        return progress(job.startsAtEpochMillis, job.finishesAtEpochMillis)
    }

    private fun progress(start: Long, finish: Long): Float {
        val duration = (finish - start).coerceAtLeast(1L)
        return (services.clock.nowEpochMillis() - start).coerceIn(0L, duration).toFloat() / duration.toFloat()
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
        drawButton(layout.recipe, recipeAvailable())
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
        smallFont.draw(batch, "${Gdx.graphics.framesPerSecond} FPS", layout.top.x + layout.top.width - 62f, layout.top.y + 17f)
        font.draw(batch, selectionTitle(), layout.panel.x + 12f, layout.panel.y + layout.panel.height - 13f)
        smallFont.draw(batch, selectionDetails(), layout.panel.x + 12f, layout.panel.y + 13f)
        drawButtonLabel(layout.recipe, recipeLabel())
        drawButtonLabel(layout.launch, launchLabel())
        drawButtonLabel(layout.task, taskLabel())
        drawButtonLabel(layout.base, "BASE")
        batch.end()
    }

    private fun drawButton(rect: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else BUTTON_DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) ACCENT else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun drawButtonLabel(rect: Rectangle, label: String) {
        smallFont.color = TEXT
        smallFont.draw(batch, label, rect.x + 6f, rect.y + 29f)
    }

    private fun economyLine(): String {
        val technologyMultiplier = assembler.productionMultipliers(gameState.assembly).technologies
        return "${gameState.economy.spaceDollars} SD · Fe ${stock(RAW_IRON)} · Lg ${stock(REFINED_IRON)} · Pile ${stock(COMPONENT_POWER_CELL)} · Tech ${gameState.assembly.installedTechnologyIds.size} · x${technologyMultiplier / 10_000L}%"
    }

    private fun selectionTitle(): String = when (val target = selected) {
        Selection.Base -> "Base Delta"
        Selection.Assembler -> "Assembleur AS-01"
        Selection.Refiner -> "Raffineur RF-01"
        is Selection.Deposit -> deposits.first { it.id == target.id }.label
        null -> "Aucune sélection"
    }

    private fun selectionDetails(): String = when (val target = selected) {
        Selection.Base -> message
        Selection.Assembler -> assemblyDetails()
        Selection.Refiner -> refiningDetails()
        is Selection.Deposit -> {
            val state = gameState.economy.deposits.getValue(target.id)
            val rate = economyDefinitions.deposits.getValue(target.id).extractionPerSecond
            "Réserve ${state.remainingReserve} · collecte ${state.pendingCollection} · $rate/s"
        }
        null -> "Touchez un gisement, RF-01, AS-01 ou la base"
    }

    private fun refiningDetails(): String {
        val ready = gameState.refining.jobs.count { it.status == RefiningJobStatus.READY_TO_COLLECT }
        val running = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.RUNNING }
        return when {
            ready > 0 -> "$ready produit(s) raffiné(s) prêt(s)"
            running != null -> "${refiningRecipeName(running.recipeId)} · ${remainingSeconds(running.finishesAtEpochMillis)}s"
            else -> "${selectedRefiningRecipeName()} · file ${gameState.refining.jobs.size}/${refiningDefinitions.robot.queueCapacity}"
        }
    }

    private fun assemblyDetails(): String {
        val recipe = selectedAssemblyRecipe()
        val technology = selectedAssemblyTechnology()
        val missingPrerequisite = !gameState.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)
        if (missingPrerequisite) return "${selectedAssemblyRecipeName()} · verrouillée par l’arbre technologique"
        val ready = gameState.assembly.jobs.count { it.status == AssemblyJobStatus.READY_TO_COLLECT }
        if (ready > 0) return "$ready production(s) AS prête(s) · sortie conservée"
        if (technology != null) {
            val comparison = assembler.compareExtraction(BASE_EXTRACTION_PER_SECOND, gameState.assembly, technology.id)
            val installed = technology.id in gameState.assembly.installedTechnologyIds
            return if (installed) {
                "${selectedAssemblyRecipeName()} installée · ${comparison.currentPerMinute}/min"
            } else {
                "${selectedAssemblyRecipeName()} · ${comparison.currentPerMinute} → ${comparison.projectedPerMinute}/min"
            }
        }
        return "${selectedAssemblyRecipeName()} · file ${gameState.assembly.jobs.size}/${assemblyDefinitions.robot.queueCapacity}"
    }

    private fun remainingSeconds(finish: Long): Long =
        ((finish - services.clock.nowEpochMillis()) / 1_000L).coerceAtLeast(0L)

    private fun stock(resourceId: GameId): Long = gameState.economy.inventory[resourceId] ?: 0L

    private fun recipeAvailable(): Boolean = selected == Selection.Refiner || selected == Selection.Assembler

    private fun recipeLabel(): String = when (selected) {
        Selection.Refiner -> if (refiningRecipeIndex == 0) "REC. FER" else "REC. CUIV."
        Selection.Assembler -> when (selectedAssemblyRecipeId()) {
            ASSEMBLY_POWER_CELL -> "PILE"
            ASSEMBLY_SENSOR_ARRAY -> "CAPTEUR"
            ASSEMBLY_TECH_EXTRACTION -> "TECH 1"
            else -> "TECH 2"
        }
        else -> "RECETTE"
    }

    private fun launchLabel(): String = when (selected) {
        Selection.Base -> "VENDRE"
        Selection.Refiner, Selection.Assembler -> "LANCER"
        is Selection.Deposit -> "COLLECTER"
        null -> "ACTION"
    }

    private fun taskLabel(): String = when (selected) {
        Selection.Base -> if (gameState.refining.refundBuffer.isNotEmpty()) "REMBOURS." else "TÂCHE"
        Selection.Refiner -> if (gameState.refining.jobs.any { it.status == RefiningJobStatus.READY_TO_COLLECT }) "COLLECTER" else "ANNULER"
        Selection.Assembler -> when {
            gameState.assembly.jobs.any { it.status == AssemblyJobStatus.READY_TO_COLLECT } -> "COLLECTER"
            technologyInstallAvailable() -> "INSTALLER"
            else -> "TÂCHE"
        }
        else -> "TÂCHE"
    }

    private fun launchAvailable(): Boolean = when (val target = selected) {
        Selection.Base -> gameState.economy.inventory.values.any { it > 0L }
        Selection.Refiner -> canLaunchRefining()
        Selection.Assembler -> canLaunchAssembly()
        is Selection.Deposit -> gameState.economy.deposits.getValue(target.id).pendingCollection > 0L
        null -> false
    }

    private fun taskAvailable(): Boolean = when (selected) {
        Selection.Base -> gameState.refining.refundBuffer.isNotEmpty()
        Selection.Refiner -> gameState.refining.jobs.isNotEmpty()
        Selection.Assembler -> gameState.assembly.jobs.isNotEmpty() || technologyInstallAvailable()
        else -> false
    }

    private fun canLaunchRefining(): Boolean {
        if (gameState.refining.jobs.size >= refiningDefinitions.robot.queueCapacity) return false
        return selectedRefiningRecipe().inputs.all { (id, quantity) -> stock(id) >= quantity }
    }

    private fun canLaunchAssembly(): Boolean {
        val recipe = selectedAssemblyRecipe()
        if (gameState.assembly.jobs.size >= assemblyDefinitions.robot.queueCapacity) return false
        if (!gameState.assembly.installedTechnologyIds.containsAll(recipe.requiredTechnologyIds)) return false
        return recipe.inputs.all { (id, quantity) -> stock(id) >= quantity }
    }

    private fun technologyInstallAvailable(): Boolean {
        val technology = selectedAssemblyTechnology() ?: return false
        if (technology.id in gameState.assembly.installedTechnologyIds) return false
        if (!gameState.assembly.installedTechnologyIds.containsAll(technology.requiredTechnologyIds)) return false
        return stock(technology.itemResourceId) > 0L
    }

    private fun performLaunchAction() {
        when (val target = selected) {
            Selection.Base -> applyEconomy(economy.sellAllSellable(gameState.economy))
            Selection.Refiner -> applyRefining(
                refiner.launch(gameState.refiningView(), selectedRefiningRecipeId(), services.clock.nowEpochMillis()),
            )
            Selection.Assembler -> applyAssembly(
                assembler.launch(gameState, selectedAssemblyRecipeId(), services.clock.nowEpochMillis()),
            )
            is Selection.Deposit -> applyEconomy(economy.collect(gameState.economy, target.id))
            null -> Unit
        }
    }

    private fun performTaskAction() {
        when (selected) {
            Selection.Base -> applyRefining(refiner.collectRefunds(gameState.refiningView()))
            Selection.Refiner -> {
                val ready = gameState.refining.jobs.firstOrNull { it.status == RefiningJobStatus.READY_TO_COLLECT }
                val result = if (ready != null) {
                    refiner.collect(gameState.refiningView(), ready.id, services.clock.nowEpochMillis())
                } else {
                    val first = gameState.refining.jobs.firstOrNull() ?: return
                    refiner.cancel(gameState.refiningView(), first.id, services.clock.nowEpochMillis())
                }
                applyRefining(result)
            }
            Selection.Assembler -> {
                val ready = gameState.assembly.jobs.firstOrNull { it.status == AssemblyJobStatus.READY_TO_COLLECT }
                if (ready != null) {
                    applyAssembly(assembler.collect(gameState, ready.id, services.clock.nowEpochMillis()))
                } else {
                    val technology = selectedAssemblyTechnology() ?: return
                    applyAssembly(assembler.installTechnology(gameState, technology.id))
                }
            }
            else -> Unit
        }
    }

    private fun applyEconomy(result: EconomyCommandResult) {
        when (result) {
            is EconomyCommandResult.Applied -> {
                gameState = gameState.copy(economy = result.state)
                message = if (result.transaction.reason == "sell_all") "+${result.transaction.spaceDollarDelta} SD" else "Collecte transférée"
                persistAction()
            }
            is EconomyCommandResult.Rejected -> reject(result.code)
        }
    }

    private fun applyRefining(result: RefiningCommandResult) {
        when (result) {
            is RefiningCommandResult.Applied -> {
                gameState = gameState.withRefining(result.state)
                message = when {
                    result.transaction.reason == "launch_refining" -> "Raffinage lancé · ingrédients réservés"
                    result.transaction.reason == "collect_refining" -> "Matériau raffiné collecté"
                    result.transaction.reason.startsWith("cancel_refining") -> "Raffinage annulé · remboursement appliqué"
                    else -> "Remboursement collecté"
                }
                persistAction()
            }
            is RefiningCommandResult.Rejected -> reject(result.code)
        }
    }

    private fun applyAssembly(result: AssemblyCommandResult) {
        when (result) {
            is AssemblyCommandResult.Applied -> {
                gameState = result.state
                message = when (result.transaction.reason) {
                    "launch_assembly" -> "Assemblage lancé · composants réservés"
                    "collect_assembly" -> "Production AS collectée"
                    "install_technology" -> "Technologie installée · effet appliqué"
                    else -> result.transaction.reason
                }
                persistAction()
            }
            is AssemblyCommandResult.Rejected -> reject(result.code)
        }
    }

    private fun persistAction() {
        if (saveState()) services.haptic.success() else {
            message = "Action appliquée · sauvegarde échouée"
            services.haptic.warning()
        }
    }

    private fun reject(code: String) {
        message = when {
            code == "technology_prerequisite_missing" -> "Technologie verrouillée · installez le nœud précédent"
            code == "technology_item_missing" -> "Fabriquez puis collectez la technologie"
            code == "output_storage_full" -> "Stockage plein · résultat conservé"
            code.startsWith("missing_input") -> "Matériaux insuffisants"
            else -> code
        }
        services.haptic.warning()
    }

    private fun cycleRecipe() {
        when (selected) {
            Selection.Refiner -> refiningRecipeIndex = (refiningRecipeIndex + 1) % refiningRecipeIds.size
            Selection.Assembler -> assemblyRecipeIndex = (assemblyRecipeIndex + 1) % assemblyRecipeIds.size
            else -> return
        }
        services.haptic.impact()
    }

    private fun selectedRefiningRecipeId(): GameId = refiningRecipeIds[refiningRecipeIndex]
    private fun selectedRefiningRecipe() = refiningDefinitions.recipes.getValue(selectedRefiningRecipeId())
    private fun selectedRefiningRecipeName(): String = refiningRecipeName(selectedRefiningRecipeId())
    private fun refiningRecipeName(id: GameId): String = if (id == RECIPE_IRON) "Lingots de fer" else "Plaques de cuivre"

    private fun selectedAssemblyRecipeId(): GameId = assemblyRecipeIds[assemblyRecipeIndex]
    private fun selectedAssemblyRecipe() = assemblyDefinitions.recipes.getValue(selectedAssemblyRecipeId())
    private fun selectedAssemblyRecipeName(): String = when (selectedAssemblyRecipeId()) {
        ASSEMBLY_POWER_CELL -> "Pile énergétique"
        ASSEMBLY_SENSOR_ARRAY -> "Réseau capteur"
        ASSEMBLY_TECH_EXTRACTION -> "Protocole extraction"
        else -> "Tri quantique"
    }

    private fun selectedAssemblyTechnology(): TechnologyDefinition? {
        val output = selectedAssemblyRecipe().outputResourceId
        return assemblyDefinitions.technologies.values.firstOrNull { it.itemResourceId == output }
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
            Rectangle(left, top - if (compact) 50f else 56f, right - left, if (compact) 50f else 56f),
            Rectangle(left, bottom, (recipe.x - gap - left).coerceAtLeast(190f), if (compact) 58f else 64f),
            recipe,
            launch,
            task,
            base,
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
            expanded(assemblerBounds, padding).contains(point) -> Selection.Assembler
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
        return layout.top.contains(point) || layout.panel.contains(point) || layout.recipe.contains(point) ||
            layout.launch.contains(point) || layout.task.contains(point) || layout.base.contains(point)
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
                layout.recipe.contains(point) && recipeAvailable() -> cycleRecipe()
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
        val label: String,
        val position: Vector2,
        val radius: Float,
        val color: Color,
    )

    private sealed interface Selection {
        data object Base : Selection
        data object Assembler : Selection
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
        const val BASE_EXTRACTION_PER_SECOND = 6L

        val RAW_IRON = GameId.of("raw_iron")
        val REFINED_IRON = GameId.of("refined_iron_ingot")
        val COMPONENT_POWER_CELL = GameId.of("component_power_cell")
        val DEPOSIT_IRON = GameId.of("deposit_iron_alpha")
        val DEPOSIT_COPPER = GameId.of("deposit_copper_beta")
        val DEPOSIT_CRYSTAL = GameId.of("deposit_crystal_gamma")
        val RECIPE_IRON = GameId.of("recipe_iron_ingot")
        val RECIPE_COPPER = GameId.of("recipe_copper_plate")
        val ASSEMBLY_POWER_CELL = GameId.of("assembly_power_cell")
        val ASSEMBLY_SENSOR_ARRAY = GameId.of("assembly_sensor_array")
        val ASSEMBLY_TECH_EXTRACTION = GameId.of("assembly_tech_extraction_protocol")
        val ASSEMBLY_TECH_SORTING = GameId.of("assembly_tech_quantum_sorting")

        val BACKGROUND = Color(0.008f, 0.014f, 0.035f, 1f)
        val MAP_SHADOW = Color(0.01f, 0.01f, 0.02f, 1f)
        val MAP_GROUND = Color(0.075f, 0.095f, 0.13f, 1f)
        val GRID = Color(0.12f, 0.17f, 0.22f, 1f)
        val SHADOW = Color(0.02f, 0.025f, 0.035f, 1f)
        val BASE_SIDE = Color(0.10f, 0.15f, 0.22f, 1f)
        val BASE = Color(0.19f, 0.27f, 0.37f, 1f)
        val ASSEMBLER = Color(0.17f, 0.25f, 0.29f, 1f)
        val REFINER = Color(0.22f, 0.24f, 0.31f, 1f)
        val HOT = Color(1f, 0.47f, 0.16f, 1f)
        val TECH_GLOW = Color(0.20f, 0.90f, 0.72f, 1f)
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
