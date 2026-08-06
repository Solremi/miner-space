package fr.solremi.minerspace.game.ferrum.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.manufacturing.ManufacturingCoordinator
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.game.ferrum.input.FerrumCameraController
import fr.solremi.minerspace.game.ferrum.input.FerrumInputController
import fr.solremi.minerspace.game.ferrum.model.FerrumColonyDevelopment
import fr.solremi.minerspace.game.ferrum.model.FerrumScreenState
import fr.solremi.minerspace.game.ferrum.presentation.FerrumActionController
import fr.solremi.minerspace.game.ferrum.presentation.FerrumActionFeedback
import fr.solremi.minerspace.game.ferrum.presentation.FerrumFeedbackKind
import fr.solremi.minerspace.game.ferrum.presentation.FerrumProductionAssistant
import fr.solremi.minerspace.game.ferrum.scene.FerrumNodeId
import fr.solremi.minerspace.game.ferrum.scene.FerrumPrimitiveScene
import fr.solremi.minerspace.game.ferrum.text.FerrumTextCatalog
import fr.solremi.minerspace.game.ferrum.text.FrenchFerrumText
import fr.solremi.minerspace.game.ferrum.ui.FerrumHudPresenter
import fr.solremi.minerspace.game.ferrum.ui.FerrumHudRenderer
import fr.solremi.minerspace.game.performance.RuntimePerformanceBudgets
import fr.solremi.minerspace.game.presentation.PresentationController
import fr.solremi.minerspace.game.ui.FerrumPlayerHudLayout
import fr.solremi.minerspace.game.ui.FerrumPlayerHudLayoutCalculator
import fr.solremi.minerspace.game.ui.FerrumPrimaryDestination
import fr.solremi.minerspace.game.ui.FerrumSecondaryDestination
import fr.solremi.minerspace.game.ui.UiInsets
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
    private val text: FerrumTextCatalog = FrenchFerrumText,
) : KtxScreen {
    private val controller = ManufacturingCoordinator.fromServices(services)
    private val actions = FerrumActionController(controller, services.clock, text)
    private val hudPresenter = FerrumHudPresenter(actions, text)
    private val scene = FerrumPrimitiveScene()
    private val hudRenderer = FerrumHudRenderer()

    private val worldCamera = OrthographicCamera()
    private val worldViewport = ExtendViewport(24f, 13.5f, 30f, 18f, worldCamera)
    private val cameraController = FerrumCameraController(worldCamera)
    private val hudCamera = OrthographicCamera()
    private val hudViewport = ExtendViewport(640f, 320f, 960f, 540f, hudCamera)

    private val state = FerrumScreenState(
        message = text.initialMessage,
        advice = FerrumProductionAssistant.initial(text),
        development = FerrumColonyDevelopment.from(controller.state),
    )
    private var layout: FerrumPlayerHudLayout? = null

    private val inputController = FerrumInputController(
        cameraController = cameraController,
        hudPoint = ::hudPoint,
        isHudRegion = ::isHudRegion,
        onHudTap = ::tapHud,
        onWorldTap = ::tapWorld,
    )
    private val gestures = GestureDetector(inputController)
    private val input = InputMultiplexer(gestures)
    private val lifecycle = LifecycleObserver { lifecycleState ->
        if (lifecycleState == LifecycleState.BACKGROUND && !controller.save()) {
            state.message = FrenchGameText.text(GameTextKey.SAVE_DEFERRED)
        }
    }

    override fun show() {
        controller.start()
        refreshPresentation()
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
        cameraController.recenter()
    }

    override fun hide() {
        if (!controller.save()) state.message = FrenchGameText.text(GameTextKey.SAVE_DEFERRED)
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
        if (tick.autosaveFailed) state.message = FrenchGameText.text(GameTextKey.AUTOSAVE_DEFERRED)
        refreshPresentation()

        val budget = RuntimePerformanceBudgets.forQuality(PresentationController.current.quality)
        Gdx.gl.glClearColor(0.006f, 0.010f, 0.025f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        worldViewport.apply()
        worldCamera.update()
        scene.select(state.selected)
        scene.render(
            camera = worldCamera,
            nowMillis = services.clock.monotonicMillis().coerceAtLeast(0L),
            budget = budget,
            productionActive = controller.state.refining.jobs.isNotEmpty() || controller.state.assembly.jobs.isNotEmpty(),
            developmentStage = state.development.stage,
        )
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        drawHud()
    }

    override fun dispose() {
        hide()
        scene.dispose()
        hudRenderer.dispose()
    }

    private fun refreshPresentation() {
        state.advice = FerrumProductionAssistant.evaluate(
            controller.state,
            controller.refiningDefinitions,
            controller.assemblyDefinitions,
            text,
        )
        val current = FerrumColonyDevelopment.from(controller.state)
        if (current.stage.rank > state.lastAnnouncedStage.rank) {
            state.message = text.stageAnnouncement(current.stage)
            state.lastAnnouncedStage = current.stage
            services.haptic.success()
        }
        state.development = current
    }

    private fun drawHud() {
        hudViewport.apply()
        hudCamera.update()
        val current = calculateLayout()
        layout = current
        val model = hudPresenter.present(state, controller.secondsSinceLastSave())
        hudRenderer.render(hudCamera, current, model)
    }

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
            menuOpen = state.menuOpen,
        )
    }

    private fun hudPoint(x: Float, y: Float): Vector2 = Vector2(x, y).also(hudViewport::unproject)

    private fun isHudRegion(point: Vector2): Boolean {
        val current = layout ?: calculateLayout()
        return current.top.contains(point) || current.interactive.any { it.contains(point) }
    }

    private fun tapHud(point: Vector2): Boolean {
        val current = layout ?: calculateLayout()
        current.primaryNavigation.forEachIndexed { index, rect ->
            if (!rect.contains(point)) return@forEachIndexed
            when (FerrumPrimaryDestination.entries[index]) {
                FerrumPrimaryDestination.EXPLORATION -> onMeteorRequested()
                FerrumPrimaryDestination.FLEET -> onRobotsRequested()
                FerrumPrimaryDestination.MISSIONS -> onMissionsRequested()
                FerrumPrimaryDestination.MENU -> state.menuOpen = !state.menuOpen
            }
            services.haptic.impact()
            return true
        }
        current.secondaryMenu.forEachIndexed { index, rect ->
            if (!rect.contains(point)) return@forEachIndexed
            state.menuOpen = false
            when (FerrumSecondaryDestination.entries[index]) {
                FerrumSecondaryDestination.STRATEGY -> onStrategyRequested()
                FerrumSecondaryDestination.ARCHIVES -> onArchivesRequested()
                FerrumSecondaryDestination.SETTINGS -> onPresentationRequested()
                FerrumSecondaryDestination.TRANSFER -> onTransferRequested()
                FerrumSecondaryDestination.BONUS -> onAdsRequested()
            }
            services.haptic.impact()
            return true
        }
        return when {
            current.status.contains(point) -> {
                state.advice.target?.let { state.selected = it } ?: onMissionsRequested()
                services.haptic.impact()
                true
            }
            current.recipe.contains(point) && actions.recipeAvailable(state) -> {
                applyFeedback(actions.cycleRecipe(state)); true
            }
            current.action.contains(point) && actions.actionAvailable(state) -> {
                applyFeedback(actions.performAction(state)); true
            }
            current.task.contains(point) && actions.taskAvailable(state) -> {
                applyFeedback(actions.performTask(state)); true
            }
            current.utility.contains(point) -> {
                if (state.selected == FerrumNodeId.REFINER || state.selected == FerrumNodeId.ASSEMBLER) {
                    applyFeedback(actions.cycleBatch(state))
                } else {
                    cameraController.recenter()
                    services.haptic.success()
                }
                true
            }
            current.top.contains(point) -> true
            else -> false
        }
    }

    private fun tapWorld(screenX: Float, screenY: Float) {
        val next = scene.pick(cameraController.pickRay(screenX, screenY))
        if (next != state.selected) services.haptic.impact()
        state.selected = next
        state.menuOpen = false
    }

    private fun applyFeedback(feedback: FerrumActionFeedback) {
        state.message = feedback.message
        when (feedback.kind) {
            FerrumFeedbackKind.NONE -> Unit
            FerrumFeedbackKind.IMPACT -> services.haptic.impact()
            FerrumFeedbackKind.SUCCESS -> services.haptic.success()
            FerrumFeedbackKind.WARNING -> services.haptic.warning()
        }
    }
}
