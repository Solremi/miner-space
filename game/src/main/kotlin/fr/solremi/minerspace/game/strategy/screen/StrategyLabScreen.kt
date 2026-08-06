package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.economy.CoreEconomyContentLoader
import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.data.save.RobotFleetCodec
import fr.solremi.minerspace.data.save.StrategyStateCodec
import fr.solremi.minerspace.data.strategy.StrategyContentLoader
import fr.solremi.minerspace.data.transaction.StrategyStateTransactionCoordinator
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.robot.RobotAutomationEngine
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.domain.strategy.SpecializationId
import fr.solremi.minerspace.domain.strategy.StrategyAccess
import fr.solremi.minerspace.domain.strategy.StrategyCommandResult
import fr.solremi.minerspace.domain.strategy.StrategyEngine
import fr.solremi.minerspace.domain.strategy.StrategyState
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen

class StrategyLabScreen(
    private val services: GameServices,
    private val onBack: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.72f) }
    private val small = BitmapFont().apply { data.setScale(.58f) }

    private val economyDefinitions = CoreEconomyContentLoader().load(services.content)
    private val economy = CoreEconomyEngine(economyDefinitions)
    private val robotDefinitions = RobotContentLoader().load(services.content)
    private val robotEngine = RobotAutomationEngine(robotDefinitions)
    private val strategyDefinitions = StrategyContentLoader().load(services.content)
    private val strategyEngine = StrategyEngine(strategyDefinitions)
    private val mainCodec = ManufacturingSnapshotCodec()
    private val robotCodec = RobotFleetCodec()
    private val strategyCodec = StrategyStateCodec()
    private val transactions = StrategyStateTransactionCoordinator(
        services.save,
        services.clock,
        services.logger,
    )

    private var main = loadMain()
    private var robots = loadRobots()
    private var strategy = loadStrategy()
    private var selectedSpecialization = strategy.activeSpecialization ?: SpecializationId.INDUSTRIAL
    private var selectedModuleId = strategyDefinitions.modules.keys.first()
    private var selectedInstanceId: String? = strategy.modules.keys.firstOrNull()
    private var selectedRobotId = robots.robots.keys.first()
    private var message = "Comparez avant de choisir"
    private var layout: StrategyLabLayout? = null
    private var persistenceBlocked = false

    private val lifecycle = LifecycleObserver {
        if (it == LifecycleState.BACKGROUND && !persistenceBlocked) saveStrategy(strategy)
    }
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat())
            viewport.unproject(point)
            touch(point)
            return true
        }
    }

    override fun show() {
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
        main = loadMain()
        robots = loadRobots()
        strategy = strategyEngine.normalize(loadStrategy())
        persistenceBlocked = false
    }

    override fun hide() {
        if (!persistenceBlocked) saveStrategy(strategy)
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) =
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        val current = StrategyLabLayoutCalculator.calculate(viewport.worldWidth, viewport.worldHeight)
        layout = current
        drawPanels(current)
        drawText(current)
    }

    private fun drawPanels(layout: StrategyLabLayout) {
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL
        shapes.rect(layout.specializationPanel.x, layout.specializationPanel.y, layout.specializationPanel.width, layout.specializationPanel.height)
        shapes.rect(layout.modulePanel.x, layout.modulePanel.y, layout.modulePanel.width, layout.modulePanel.height)
        shapes.rect(layout.comparisonPanel.x, layout.comparisonPanel.y, layout.comparisonPanel.width, layout.comparisonPanel.height)
        layout.specializationButtons.forEachIndexed { index, rect ->
            val id = SpecializationId.entries[index]
            drawButton(rect, if (id == selectedSpecialization) SELECTED else BUTTON, SPECIALIZATION_ACCENT)
        }
        layout.moduleButtons.forEachIndexed { index, rect ->
            val id = strategyDefinitions.modules.keys.elementAt(index)
            drawButton(rect, if (id == selectedModuleId) SELECTED else BUTTON, MODULE_ACCENT)
        }
        layout.robotButtons.forEachIndexed { index, rect ->
            val id = robots.robots.keys.elementAt(index)
            drawButton(rect, if (id == selectedRobotId) SELECTED else BUTTON, ROBOT_ACCENT)
        }
        drawButton(layout.choose, BUTTON, SPECIALIZATION_ACCENT)
        drawButton(layout.craft, BUTTON, MODULE_ACCENT)
        drawButton(layout.equip, BUTTON, ROBOT_ACCENT)
        drawButton(layout.upgrade, BUTTON, MODULE_ACCENT)
        drawButton(layout.dismantle, DANGER_BUTTON, DANGER)
        drawButton(layout.back, BUTTON, ROBOT_ACCENT)
        shapes.end()
    }

    private fun drawButton(rect: Rectangle, fill: Color, accent: Color) {
        shapes.color = fill
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = accent
        shapes.rect(rect.x, rect.y, rect.width, 3f)
    }

    private fun drawText(layout: StrategyLabLayout) {
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        small.color = MUTED
        font.draw(batch, "LABORATOIRE STRATÉGIQUE", layout.top.x + 12f, layout.top.y + layout.top.height - 12f)
        small.draw(batch, "${main.economy.spaceDollars} SD · essai ${if (strategy.trialUsed) "utilisé" else "gratuit"}", layout.top.x + 12f, layout.top.y + 13f)
        small.draw(batch, message, layout.top.x + 270f, layout.top.y + 13f)
        font.draw(batch, "SPÉCIALISATION", layout.specializationPanel.x + 10f, layout.specializationPanel.y + layout.specializationPanel.height - 10f)
        layout.specializationButtons.forEachIndexed { index, rect ->
            small.draw(batch, StrategyLabText.specializationName(SpecializationId.entries[index]), rect.x + 7f, rect.y + 27f)
        }
        font.draw(batch, "MODULES", layout.modulePanel.x + 10f, layout.modulePanel.y + layout.modulePanel.height - 10f)
        layout.moduleButtons.forEachIndexed { index, rect ->
            small.draw(batch, StrategyLabText.moduleName(strategyDefinitions.modules.keys.elementAt(index)), rect.x + 6f, rect.y + 25f)
        }
        font.draw(batch, "ROBOTS", layout.comparisonPanel.x + 10f, layout.comparisonPanel.y + layout.comparisonPanel.height - 10f)
        layout.robotButtons.forEachIndexed { index, rect ->
            val robot = robots.robots.values.elementAt(index)
            small.draw(batch, "${robot.family.name.take(2)} N${robot.level}", rect.x + 7f, rect.y + 24f)
        }

        val preview = strategyEngine.compare(strategy, selectedSpecialization)
        val current = strategyEngine.compare(strategy, strategy.activeSpecialization)
        val x = layout.comparisonPanel.x + 10f
        var y = layout.comparisonPanel.y + layout.comparisonPanel.height - 82f
        small.draw(batch, "Actuel → aperçu", x, y)
        y -= 18f
        small.draw(batch, "Extraction ${StrategyLabText.percent(current.extractionMillionths)} → ${StrategyLabText.percent(preview.extractionMillionths)}", x, y)
        y -= 17f
        small.draw(batch, "Raffinage ${StrategyLabText.percent(current.refiningSpeedMillionths)} → ${StrategyLabText.percent(preview.refiningSpeedMillionths)}", x, y)
        y -= 17f
        small.draw(batch, "Assemblage ${StrategyLabText.percent(current.assemblySpeedMillionths)} → ${StrategyLabText.percent(preview.assemblySpeedMillionths)}", x, y)
        y -= 17f
        small.draw(batch, "Logistique ${StrategyLabText.percent(current.logisticsMillionths)} → ${StrategyLabText.percent(preview.logisticsMillionths)}", x, y)

        val module = strategyDefinitions.modules.getValue(selectedModuleId)
        small.draw(batch, "Coût ${module.craftCostSpaceDollars} SD · ${module.craftInputs.entries.joinToString { "${it.value} ${StrategyLabText.shortResource(it.key)}" }}", layout.modulePanel.x + 10f, layout.modulePanel.y + 58f)
        val selectedOwned = selectedInstanceId?.let(strategy.modules::get)
        small.draw(batch, selectedOwned?.let { "Instance ${it.instanceId} · N${it.level} · ${it.equippedRobotId?.value ?: "libre"}" } ?: "Aucune instance sélectionnée", layout.modulePanel.x + 10f, layout.modulePanel.y + 39f)
        label(layout.choose, "CHOISIR")
        label(layout.craft, "FABRIQUER")
        label(layout.equip, if (selectedOwned?.equippedRobotId == null) "ÉQUIPER" else "RETIRER")
        label(layout.upgrade, "AMÉLIORER")
        label(layout.dismantle, "DÉMONTER")
        label(layout.back, "RETOUR")
        batch.end()
    }

    private fun label(rect: Rectangle, text: String) {
        small.color = TEXT
        small.draw(batch, text, rect.x + 7f, rect.y + 28f)
    }

    private fun touch(point: Vector2) {
        if (persistenceBlocked) {
            message = "Relancez l'application pour terminer la transaction"
            services.haptic.warning()
            return
        }
        val current = layout ?: return
        current.specializationButtons.forEachIndexed { index, rect ->
            if (rect.contains(point)) {
                selectedSpecialization = SpecializationId.entries[index]
                services.haptic.impact()
                return
            }
        }
        current.moduleButtons.forEachIndexed { index, rect ->
            if (rect.contains(point)) {
                selectedModuleId = strategyDefinitions.modules.keys.elementAt(index)
                selectedInstanceId = strategy.modules.values.firstOrNull { it.definitionId == selectedModuleId }?.instanceId
                services.haptic.impact()
                return
            }
        }
        current.robotButtons.forEachIndexed { index, rect ->
            if (rect.contains(point)) {
                selectedRobotId = robots.robots.keys.elementAt(index)
                services.haptic.impact()
                return
            }
        }
        when {
            current.choose.contains(point) -> choose()
            current.craft.contains(point) -> craft()
            current.equip.contains(point) -> equipOrUnequip()
            current.upgrade.contains(point) -> upgrade()
            current.dismantle.contains(point) -> dismantle()
            current.back.contains(point) -> onBack()
        }
    }

    private fun choose() = applyStrategyResult(strategyEngine.chooseSpecialization(strategy, selectedSpecialization, access()))
    private fun craft() = applyStrategyResult(strategyEngine.craft(strategy, selectedModuleId, access()))

    private fun upgrade() {
        val id = selectedInstanceId ?: return reject("Fabriquez d'abord ce module")
        applyStrategyResult(strategyEngine.upgrade(strategy, id, access()))
    }

    private fun dismantle() {
        val id = selectedInstanceId ?: return reject("Aucune instance")
        applyStrategyResult(strategyEngine.dismantle(strategy, id))
    }

    private fun equipOrUnequip() {
        val id = selectedInstanceId ?: return reject("Aucune instance")
        val owned = strategy.modules.getValue(id)
        val result = if (owned.equippedRobotId == null) {
            strategyEngine.equip(strategy, id, selectedRobotId, access())
        } else {
            strategyEngine.unequip(strategy, id)
        }
        applyStrategyResult(result)
    }

    private fun applyStrategyResult(result: StrategyCommandResult) {
        when (result) {
            is StrategyCommandResult.Rejected -> reject(StrategyLabText.rejection(result.code))
            is StrategyCommandResult.Applied -> {
                val inventory = main.economy.inventory.toMutableMap()
                result.transaction.inventoryDeltas.forEach { (id, delta) ->
                    val next = Math.addExact(inventory[id] ?: 0L, delta)
                    if (next < 0L || next > economyDefinitions.resources.getValue(id).storageCapacity) {
                        return reject("Stock incompatible")
                    }
                    inventory[id] = next
                }
                val nextMoney = Math.addExact(main.economy.spaceDollars, result.transaction.spaceDollarDelta)
                if (nextMoney < 0L) return reject("SpaceDollars insuffisants")
                val nextMain = main.copy(economy = main.economy.copy(inventory = inventory, spaceDollars = nextMoney, transactionSequence = Math.addExact(main.economy.transactionSequence, 1L)))
                val committed = transactions.commit(
                    main = nextMain,
                    strategy = result.state,
                    mainContentVersion = economyDefinitions.contentVersion,
                    strategyContentVersion = strategyDefinitions.contentVersion,
                    reason = result.transaction.reason,
                    savedAtEpochMillis = now(),
                )
                if (!committed.committed) {
                    persistenceBlocked = true
                    message = "Transaction en attente · relancez l'application"
                    services.haptic.warning()
                    return
                }
                main = nextMain
                strategy = result.state
                selectedInstanceId = result.transaction.moduleInstanceId ?: selectedInstanceId
                message = StrategyLabText.success(result.transaction.reason)
                services.haptic.success()
            }
        }
    }

    private fun access() = StrategyAccess(now(), main.economy.spaceDollars, main.economy.inventory, robots.robots.mapValues { it.value.level })
    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)
    private fun initialMain() = ManufacturingGameState(economy.initialState(), RefiningState.empty(), AssemblyState.empty())

    private fun loadMain(): ManufacturingGameState = services.save.loadLatest()?.let { payload ->
        runCatching { mainCodec.decode(payload) }
            .onFailure { services.logger.warning(TAG, "Unable to load manufacturing state for strategy.", it) }
            .getOrNull()
    } ?: initialMain()

    private fun loadRobots(): RobotAutomationState = services.save.loadLatest(RobotFleetCodec.SLOT_ID)?.let { payload ->
        runCatching { robotEngine.normalize(robotCodec.decode(payload), now()) }
            .onFailure { services.logger.warning(TAG, "Unable to load robots for strategy.", it) }
            .getOrNull()
    } ?: robotEngine.initialState(now())

    private fun loadStrategy(): StrategyState = services.save.loadLatest(StrategyStateCodec.SLOT_ID)?.let { payload ->
        runCatching {
            require(payload.contentVersion == strategyDefinitions.contentVersion)
            strategyCodec.decode(payload)
        }.onFailure { services.logger.warning(TAG, "Unable to load strategy state.", it) }
            .getOrNull()
    } ?: StrategyState.empty()

    private fun saveStrategy(value: StrategyState): Boolean = services.save.save(
        strategyCodec.encode(value, strategyDefinitions.contentVersion, now()),
    ) == SaveWriteStatus.WRITTEN

    private fun reject(text: String) {
        message = text
        services.haptic.warning()
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        small.dispose()
    }

    private companion object {
        const val TAG = "StrategyLabScreen"
        val BACKGROUND = Color(.007f, .012f, .028f, 1f)
        val TOP = Color(.025f, .055f, .10f, 1f)
        val PANEL = Color(.035f, .072f, .12f, 1f)
        val BUTTON = Color(.07f, .15f, .22f, 1f)
        val SELECTED = Color(.14f, .26f, .34f, 1f)
        val DANGER_BUTTON = Color(.22f, .08f, .09f, 1f)
        val SPECIALIZATION_ACCENT = Color(.84f, .62f, .20f, 1f)
        val MODULE_ACCENT = Color(.60f, .40f, .94f, 1f)
        val ROBOT_ACCENT = Color(.20f, .82f, .88f, 1f)
        val DANGER = Color(.94f, .30f, .30f, 1f)
        val TEXT = Color(.94f, .97f, 1f, 1f)
        val MUTED = Color(.63f, .72f, .82f, 1f)
    }
}
