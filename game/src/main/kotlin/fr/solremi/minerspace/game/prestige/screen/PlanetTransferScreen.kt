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
import fr.solremi.minerspace.data.prestige.PlanetTransferCoordinationResult
import fr.solremi.minerspace.data.prestige.PlanetTransferCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionResult
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.prestige.PlanetId
import fr.solremi.minerspace.domain.prestige.PrestigeSnapshot
import fr.solremi.minerspace.domain.prestige.PrestigeState
import fr.solremi.minerspace.domain.prestige.VeteranRobotSnapshot
import fr.solremi.minerspace.domain.services.GameServices
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
    private val coordinator = PlanetTransferCoordinator(services)
    private var prestige: PrestigeState = coordinator.loadState()
    private var snapshot: PrestigeSnapshot = coordinator.snapshot(now())
    private var message = "Le transfert conserve Codex, archives, bonus et un robot vétéran."
    private var current: Layout? = null
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject))
            return true
        }
    }

    override fun show() {
        prestige = coordinator.loadState()
        snapshot = coordinator.snapshot(now())
        Gdx.input.inputProcessor = input
        if (prestige.pendingTransfer != null) resumePreparedTransfer()
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        val layout = layout()
        current = layout
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = TOP
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.color = PANEL
        shapes.rect(layout.summary.x, layout.summary.y, layout.summary.width, layout.summary.height)
        shapes.rect(layout.permanent.x, layout.permanent.y, layout.permanent.width, layout.permanent.height)
        drawButton(layout.primary, primaryEnabled(), LAUNCH)
        drawButton(layout.back, prestige.activePlanet == PlanetId.FERRUM_DELTA && prestige.pendingTransfer == null, BACK)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        small.color = MUTED
        font.draw(batch, "TRANSFERT PLANÉTAIRE", layout.top.x + 12f, layout.top.y + layout.top.height - 14f)
        small.draw(batch, "Journal atomique · reprise automatique · aucun doublon", layout.top.x + 12f, layout.top.y + 15f)
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
        val veteran: VeteranRobotSnapshot? = prestige.veteranRobot
            ?: snapshot.robots.maxByOrNull { it.masteryPoints }?.let(VeteranRobotSnapshot::from)
        return veteran?.let { "${it.displayName} · ${it.serialNumber} · maîtrise ${it.masteryPoints}" }
            ?: "Aucun robot transférable"
    }

    private fun touch(point: Vector2) {
        val layout = current ?: return
        when {
            layout.primary.contains(point) && primaryEnabled() -> when {
                prestige.pendingTransfer != null -> resumePreparedTransfer()
                prestige.activePlanet == PlanetId.CRYOS_IX -> onCryosReady()
                else -> prepareTransfer()
            }
            layout.back.contains(point) && prestige.activePlanet == PlanetId.FERRUM_DELTA && prestige.pendingTransfer == null ->
                onFerrumBack()
        }
    }

    private fun prepareTransfer() {
        handle(coordinator.prepareAndCommit(prestige, snapshot, now()))
    }

    private fun resumePreparedTransfer() {
        handle(coordinator.resumePrepared(prestige, now()))
    }

    private fun handle(result: PlanetTransferCoordinationResult) {
        when (result) {
            is PlanetTransferCoordinationResult.Committed -> {
                prestige = result.state
                snapshot = coordinator.snapshot(now())
                message = "Transfert terminé · 3 Noyaux Stellaires · Cryos IX disponible"
                services.haptic.success()
            }
            is PlanetTransferCoordinationResult.Pending -> {
                prestige = result.state
                message = pendingMessage(result.transaction)
                services.haptic.warning()
            }
            is PlanetTransferCoordinationResult.Rejected -> {
                prestige = result.state
                message = reject(result.code)
                services.haptic.warning()
            }
        }
    }

    private fun pendingMessage(transaction: SaveTransactionResult): String {
        val slot = transaction.failedSlotId?.let { " · slot $it" }.orEmpty()
        return when (transaction.status) {
            SaveTransactionStatus.PREPARE_FAILED -> "Journal non écrit · aucun état local modifié$slot"
            SaveTransactionStatus.PENDING -> "Transfert sécurisé mais incomplet · reprise automatique$slot"
            SaveTransactionStatus.BUSY -> "Une autre transaction locale doit être terminée"
            SaveTransactionStatus.CORRUPT -> "Journal de transaction illisible · redémarrage requis"
            SaveTransactionStatus.NO_PENDING -> "Aucun transfert en attente"
            SaveTransactionStatus.COMMITTED -> "Transfert terminé"
        }
    }

    private fun reject(code: String): String = when (code) {
        "launch_shipyard_locked" -> "Ouvrez le chantier de départ final"
        "veteran_robot_required" -> "Un robot doit atteindre 6 000 points de maîtrise"
        "transfer_already_prepared" -> "Un transfert est déjà en attente"
        "source_planet_not_active" -> "Ferrum Delta n’est plus la planète active"
        "no_pending_transfer" -> "Aucun transfert à reprendre"
        else -> code
    }

    private fun layout(): Layout {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1)
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 52f, right - left, 52f)
        val back = Rectangle(right - 88f, bottom, 88f, 48f)
        val primary = Rectangle(back.x - 8f - 132f, bottom, 132f, 48f)
        val message = Rectangle(left, bottom, primary.x - left - 8f, 48f)
        val panelBottom = bottom + 56f
        val panelTop = topBar.y - 8f
        val gap = 8f
        val panelWidth = (right - left - gap) / 2f
        return Layout(
            top = topBar,
            summary = Rectangle(left, panelBottom, panelWidth, panelTop - panelBottom),
            permanent = Rectangle(left + panelWidth + gap, panelBottom, panelWidth, panelTop - panelBottom),
            message = message,
            primary = primary,
            back = back,
        )
    }

    private fun drawButton(rect: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) accent else GRID
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        small.dispose()
    }

    private data class Layout(
        val top: Rectangle,
        val summary: Rectangle,
        val permanent: Rectangle,
        val message: Rectangle,
        val primary: Rectangle,
        val back: Rectangle,
    )

    private companion object {
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
