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
import fr.solremi.minerspace.data.robot.RobotContentLoader
import fr.solremi.minerspace.data.save.*
import fr.solremi.minerspace.domain.prestige.*
import fr.solremi.minerspace.domain.robot.RobotAutomationEngine
import fr.solremi.minerspace.domain.robot.RobotAutomationState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.domain.strategy.SpecializationId
import fr.solremi.minerspace.shared.GameId
import ktx.app.KtxScreen
import kotlin.math.max

class PlanetTransferScreen(
    private val services: GameServices,
    private val onFerrumBack: () -> Unit,
    private val onCryosReady: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.76f) }
    private val small = BitmapFont().apply { data.setScale(.58f) }
    private val engine = PlanetPrestigeEngine()
    private val prestigeCodec = PrestigeStateCodec()
    private val cryosCodec = CryosIxStateCodec()
    private val sectorCodec = SectorProgressCodec()
    private val progressionCodec = ProgressionStateCodec()
    private val narrativeCodec = NarrativeStateCodec()
    private val strategyCodec = StrategyStateCodec()
    private val robotCodec = RobotFleetCodec()
    private val robotDefinitions = RobotContentLoader().load(services.content)
    private val robotEngine = RobotAutomationEngine(robotDefinitions)
    private var prestige = loadPrestige()
    private var snapshot = snapshot()
    private var message = "Le transfert conserve Codex, archives, bonus et un robot vétéran."
    private var current: Layout? = null
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject)); return true
        }
    }

    override fun show() {
        prestige = loadPrestige()
        snapshot = snapshot()
        Gdx.input.inputProcessor = input
        if (prestige.pendingTransfer != null) recoverPreparedTransfer()
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply(); camera.update()
        val layout = layout(); current = layout
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP; shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL; shapes.rect(layout.summary.x, layout.summary.y, layout.summary.width, layout.summary.height)
        shapes.color = PANEL; shapes.rect(layout.permanent.x, layout.permanent.y, layout.permanent.width, layout.permanent.height)
        drawButton(layout.primary, primaryEnabled(), LAUNCH)
        drawButton(layout.back, prestige.activePlanet == PlanetId.FERRUM_DELTA && prestige.pendingTransfer == null, BACK)
        shapes.end()

        batch.projectionMatrix = camera.combined; batch.begin()
        font.color = TEXT; small.color = MUTED
        font.draw(batch, "TRANSFERT PLANÉTAIRE", layout.top.x + 12f, layout.top.y + layout.top.height - 14f)
        small.draw(batch, "Transaction reprenable · aucune duplication de Noyaux Stellaires", layout.top.x + 12f, layout.top.y + 15f)
        font.draw(batch, planetLine(), layout.summary.x + 12f, layout.summary.y + layout.summary.height - 14f)
        small.draw(batch, requirementLine(), layout.summary.x + 12f, layout.summary.y + layout.summary.height - 40f)
        small.draw(batch, veteranLine(), layout.summary.x + 12f, layout.summary.y + 18f)
        font.draw(batch, "ACQUIS PERMANENTS", layout.permanent.x + 12f, layout.permanent.y + layout.permanent.height - 14f)
        small.draw(batch, "Codex ${prestige.permanentCodexEntryIds.size} · Archives ${prestige.permanentArchiveIds.size}", layout.permanent.x + 12f, layout.permanent.y + layout.permanent.height - 40f)
        small.draw(batch, "Bonus ${prestige.permanentBonusIds.size} · Noyaux ${prestige.stellarCores}", layout.permanent.x + 12f, layout.permanent.y + 18f)
        small.draw(batch, message, layout.message.x, layout.message.y + 17f)
        small.draw(batch, primaryLabel(), layout.primary.x + 10f, layout.primary.y + 29f)
        small.draw(batch, "RETOUR", layout.back.x + 18f, layout.back.y + 29f)
        batch.end()
    }

    private fun primaryLabel(): String = when {
        prestige.pendingTransfer != null -> "REPRENDRE"
        prestige.activePlanet == PlanetId.CRYOS_IX -> "CRYOS IX"
        else -> "TRANSFÉRER"
    }

    private fun primaryEnabled(): Boolean = when {
        prestige.pendingTransfer != null -> true
        prestige.activePlanet == PlanetId.CRYOS_IX -> true
        else -> snapshot.launchShipyardUnlocked && snapshot.robots.any { it.masteryPoints >= 6_000L }
    }

    private fun planetLine(): String = when (prestige.activePlanet) {
        PlanetId.FERRUM_DELTA -> "FERRUM DELTA → CRYOS IX"
        PlanetId.CRYOS_IX -> "CRYOS IX · transfert ${prestige.completedTransfers} terminé"
    }

    private fun requirementLine(): String {
        if (prestige.activePlanet == PlanetId.CRYOS_IX) return "Boucle thermique active · frontière à reconstruire"
        val shipyard = if (snapshot.launchShipyardUnlocked) "chantier prêt" else "chantier final verrouillé"
        val veteran = if (snapshot.robots.any { it.masteryPoints >= 6_000L }) "vétéran prêt" else "robot vétéran requis"
        return "$shipyard · $veteran · récompense 3 Noyaux Stellaires"
    }

    private fun veteranLine(): String {
        val veteran = prestige.veteranRobot ?: snapshot.robots.maxByOrNull { it.masteryPoints }?.let { VeteranRobotSnapshot.from(it) }
        return veteran?.let { "${it.displayName} · ${it.serialNumber} · maîtrise ${it.masteryPoints}" } ?: "Aucun robot transférable"
    }

    private fun touch(point: Vector2) {
        val layout = current ?: return
        when {
            layout.primary.contains(point) && primaryEnabled() -> {
                when {
                    prestige.pendingTransfer != null -> recoverPreparedTransfer()
                    prestige.activePlanet == PlanetId.CRYOS_IX -> onCryosReady()
                    else -> prepareTransfer()
                }
            }
            layout.back.contains(point) && prestige.activePlanet == PlanetId.FERRUM_DELTA && prestige.pendingTransfer == null -> onFerrumBack()
        }
    }

    private fun prepareTransfer() {
        when (val result = engine.prepareTransfer(prestige, snapshot, now())) {
            is PrestigeCommandResult.Rejected -> {
                message = reject(result.code); services.haptic.warning()
            }
            is PrestigeCommandResult.Applied -> {
                if (!savePrestige(result.state)) {
                    message = "Préparation non enregistrée · aucun état réinitialisé"
                    services.haptic.warning(); return
                }
                prestige = result.state
                message = "Transfert préparé · réinitialisation atomique en cours"
                recoverPreparedTransfer()
            }
        }
    }

    private fun recoverPreparedTransfer() {
        val pending = prestige.pendingTransfer ?: return
        // Le slot PREPARED est la source de vérité. Ces opérations sont toutes idempotentes.
        services.save.clear("primary")
        services.save.clear(SectorProgressCodec.SLOT_ID)
        services.save.clear(StrategyStateCodec.SLOT_ID)
        services.save.clear(RobotFleetCodec.SLOT_ID)
        services.save.clear("meteor_event")

        val cryosPayload = services.save.loadLatest(CryosIxStateCodec.SLOT_ID)
        if (cryosPayload == null) {
            val initial = fr.solremi.minerspace.domain.cryos.CryosIxEngine(
                fr.solremi.minerspace.data.cryos.CryosIxContentFactory.create(),
            ).initialState(pending.veteranRobot.id)
            if (services.save.save(cryosCodec.encode(initial, "1.0.0", now())) != SaveWriteStatus.WRITTEN) {
                message = "État Cryos non écrit · reprise automatique au prochain lancement"
                services.haptic.warning(); return
            }
        }

        val reconciled = engine.reconcilePrepared(prestige) as? PrestigeCommandResult.Applied ?: return
        if (!savePrestige(reconciled.state)) {
            message = "Acquis permanents en attente · reprise automatique"
            services.haptic.warning(); return
        }
        prestige = reconciled.state
        val finalized = engine.finalizeTransfer(prestige) as? PrestigeCommandResult.Applied ?: return
        if (!savePrestige(finalized.state)) {
            message = "Clôture en attente · aucun doublon possible"
            services.haptic.warning(); return
        }
        prestige = finalized.state
        message = "Transfert terminé · 3 Noyaux Stellaires · Cryos IX disponible"
        services.haptic.success()
    }

    private fun snapshot(): PrestigeSnapshot {
        val sectors = runCatching {
            services.save.loadLatest(SectorProgressCodec.SLOT_ID)?.let(sectorCodec::decode)
        }.getOrNull()
        val progression = runCatching {
            services.save.loadLatest(ProgressionStateCodec.SLOT_ID)?.let(progressionCodec::decode)
        }.getOrNull()
        val narrative = runCatching {
            services.save.loadLatest(NarrativeStateCodec.SLOT_ID)?.let(narrativeCodec::decode)
        }.getOrNull()
        val strategy = runCatching {
            services.save.loadLatest(StrategyStateCodec.SLOT_ID)?.let(strategyCodec::decode)
        }.getOrNull()
        val robots = loadRobots()
        val bonuses = linkedSetOf<GameId>()
        strategy?.activeSpecialization?.let { bonuses += specializationBonus(it) }
        if (strategy?.modules?.isNotEmpty() == true) bonuses += GameId.of("bonus_ferrum_modules")
        return PrestigeSnapshot(
            launchShipyardUnlocked = sectors?.unlockedSectorIds?.contains(LAUNCH_SHIPYARD) == true,
            discoveredCodexEntryIds = progression?.discoveredCodexEntryIds.orEmpty(),
            archiveIds = narrative?.let { it.readTransmissionIds + it.resolvedChapterIds }.orEmpty(),
            permanentBonusIds = bonuses,
            robots = robots.robots.values,
        )
    }

    private fun loadRobots(): RobotAutomationState {
        val initial = robotEngine.initialState(now())
        val payload = services.save.loadLatest(RobotFleetCodec.SLOT_ID) ?: return initial
        return runCatching { robotEngine.normalize(robotCodec.decode(payload), now()) }.getOrElse { initial }
    }

    private fun loadPrestige(): PrestigeState {
        val payload = services.save.loadLatest(PrestigeStateCodec.SLOT_ID) ?: return engine.initialState()
        return runCatching { engine.normalize(prestigeCodec.decode(payload)) }.getOrElse { engine.initialState() }
    }

    private fun savePrestige(value: PrestigeState): Boolean =
        services.save.save(prestigeCodec.encode(value, now())) == SaveWriteStatus.WRITTEN

    private fun specializationBonus(value: SpecializationId): GameId =
        GameId.of("bonus_specialization_${value.name.lowercase()}")

    private fun reject(code: String): String = when (code) {
        "launch_shipyard_locked" -> "Ouvrez le chantier de départ final"
        "veteran_robot_required" -> "Un robot doit atteindre 6 000 points de maîtrise"
        else -> code
    }

    private fun layout(): Layout {
        val w = viewport.worldWidth; val h = viewport.worldHeight
        val sx = w / Gdx.graphics.width.coerceAtLeast(1); val sy = h / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, w - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, h - Gdx.graphics.safeInsetTop * sy - 8f)
        val topBar = Rectangle(left, top - 52f, right - left, 52f)
        val buttonsY = bottom
        val back = Rectangle(right - 88f, buttonsY, 88f, 48f)
        val primary = Rectangle(back.x - 8f - 132f, buttonsY, 132f, 48f)
        val message = Rectangle(left, bottom, primary.x - left - 8f, 48f)
        val panelBottom = bottom + 56f
        val panelTop = topBar.y - 8f
        val gap = 8f
        val width = (right - left - gap) / 2f
        return Layout(
            topBar,
            Rectangle(left, panelBottom, width, panelTop - panelBottom),
            Rectangle(left + width + gap, panelBottom, width, panelTop - panelBottom),
            message,
            primary,
            back,
        )
    }

    private fun drawButton(rect: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED; shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) accent else GRID; shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)
    override fun dispose() { hide(); shapes.dispose(); batch.dispose(); font.dispose(); small.dispose() }

    private data class Layout(
        val top: Rectangle,
        val summary: Rectangle,
        val permanent: Rectangle,
        val message: Rectangle,
        val primary: Rectangle,
        val back: Rectangle,
    )

    private companion object {
        val LAUNCH_SHIPYARD: GameId = GameId.of("sector_launch_shipyard")
        val BACKGROUND = Color(.008f, .014f, .025f, 1f)
        val TOP = Color(.05f, .075f, .12f, 1f)
        val PANEL = Color(.055f, .10f, .15f, .96f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val DISABLED = Color(.04f, .065f, .09f, 1f)
        val GRID = Color(.14f, .20f, .25f, 1f)
        val LAUNCH = Color(.94f, .48f, .16f, 1f)
        val BACK = Color(.42f, .58f, .66f, 1f)
        val TEXT = Color(.94f, .96f, 1f, 1f)
        val MUTED = Color(.65f, .74f, .80f, 1f)
    }
}
