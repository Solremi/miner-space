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
import fr.solremi.minerspace.data.ads.ContextualRewardedAdCoordinator
import fr.solremi.minerspace.data.ads.ContextualRewardedResult
import fr.solremi.minerspace.data.ads.EntitlementConsumptionResult
import fr.solremi.minerspace.data.offline.OfflineReturnCoordinator
import fr.solremi.minerspace.data.offline.OfflineReturnReport
import fr.solremi.minerspace.data.offline.OfflineReturnSession
import fr.solremi.minerspace.domain.ads.RewardType
import fr.solremi.minerspace.domain.services.GameServices
import ktx.app.KtxScreen
import kotlin.math.max

class OfflineReturnScreen(
    private val services: GameServices,
    private val onContinue: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val titleFont = BitmapFont().apply { data.setScale(1.15f) }
    private val font = BitmapFont().apply { data.setScale(0.82f) }
    private val smallFont = BitmapFont().apply { data.setScale(0.70f) }
    private val offline = OfflineReturnCoordinator(services)
    private val contextualAds = ContextualRewardedAdCoordinator(services)

    private var session: OfflineReturnSession = offline.loadAndApplyStandard()
    private var continued = false
    private var adBusy = false
    private var adAvailable = false
    private var transactionBlocked = false
    private var message = ""

    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat())
            viewport.unproject(point)
            val layout = layout()
            when {
                layout.adButton.contains(point) -> startOfflineDoubleAd()
                layout.continueButton.contains(point) && !transactionBlocked && !adBusy -> continueToGame()
            }
            return true
        }
    }

    override fun show() {
        applyPendingOfflineDouble()
        refreshAdAvailability()
        if (!session.shouldShow) {
            Gdx.app.postRunnable(::continueToGame)
        } else {
            Gdx.input.inputProcessor = input
        }
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        if (!session.shouldShow) return
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        val layout = layout()

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = PANEL_SHADOW
        shapes.rect(layout.panel.x + 10f, layout.panel.y - 10f, layout.panel.width, layout.panel.height)
        shapes.color = PANEL
        shapes.rect(layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height)
        shapes.color = ACCENT
        shapes.rect(layout.panel.x, layout.panel.y + layout.panel.height - 5f, layout.panel.width, 5f)
        drawButton(layout.adButton, adAvailable && !adBusy && !transactionBlocked, REWARD)
        drawButton(layout.continueButton, !transactionBlocked && !adBusy, ACCENT)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        titleFont.color = TEXT
        titleFont.draw(batch, "RETOUR SUR FERRUM DELTA", layout.panel.x + 22f, layout.panel.y + layout.panel.height - 28f)
        font.color = TEXT
        smallFont.color = MUTED
        var y = layout.panel.y + layout.panel.height - 66f
        lines().forEachIndexed { index, line ->
            (if (index == 0) font else smallFont).draw(batch, line, layout.panel.x + 22f, y)
            y -= if (index == 0) 28f else 22f
        }
        smallFont.color = TEXT
        smallFont.draw(
            batch,
            if (session.doubled) "PRODUCTION DOUBLÉE" else if (adBusy) "CHARGEMENT..." else "PUB · DOUBLER",
            layout.adButton.x + 10f,
            layout.adButton.y + 30f,
        )
        font.color = TEXT
        font.draw(batch, "CONTINUER", layout.continueButton.x + 25f, layout.continueButton.y + 31f)
        batch.end()
    }

    private fun drawButton(rect: Rectangle, enabled: Boolean, accent: Color) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) accent else BORDER
        shapes.rect(rect.x, rect.y, rect.width, 4f)
    }

    private fun startOfflineDoubleAd() {
        val scopeId = session.scopeId ?: return
        if (!adAvailable || adBusy || transactionBlocked || session.doubled) return
        adBusy = true
        message = "Publicité facultative en cours"
        contextualAds.watch(
            ContextualRewardedAdCoordinator.OFFLINE_DOUBLE_OFFER,
            scopeId,
        ) { result ->
            Gdx.app.postRunnable {
                adBusy = false
                when (result) {
                    is ContextualRewardedResult.Granted -> applyPendingOfflineDouble()
                    is ContextualRewardedResult.Rejected -> {
                        message = result.code
                        refreshAdAvailability()
                        services.haptic.warning()
                    }
                    is ContextualRewardedResult.Cancelled -> {
                        message = "Publicité fermée · progression normale conservée"
                        refreshAdAvailability()
                    }
                    is ContextualRewardedResult.PersistenceFailed -> {
                        message = "Récompense reçue · reprise automatique au prochain affichage"
                        services.haptic.warning()
                    }
                }
            }
        }
    }

    private fun applyPendingOfflineDouble() {
        if (session.doubled || !contextualAds.hasEntitlement(RewardType.OFFLINE_DOUBLE)) return
        val doubled = offline.doubled(session) ?: return
        val state = doubled.currentState ?: return
        val scopeId = doubled.scopeId ?: return
        val payload = offline.encode(state, doubled.nowEpochMillis)
        when (
            contextualAds.consumeWithPayload(
                rewardType = RewardType.OFFLINE_DOUBLE,
                amount = 1L,
                transactionId = "consume_offline_double_$scopeId",
                externalPayload = payload,
            )
        ) {
            EntitlementConsumptionResult.Committed -> {
                session = doubled
                transactionBlocked = false
                message = "Production hors ligne doublée, plafonnée à 8 heures"
                services.haptic.success()
                refreshAdAvailability()
            }
            is EntitlementConsumptionResult.Rejected -> {
                message = "Bonus hors ligne indisponible"
                refreshAdAvailability()
            }
            is EntitlementConsumptionResult.Pending -> {
                transactionBlocked = true
                message = "Doublement interrompu · redémarrez pour reprendre"
                services.haptic.warning()
            }
        }
    }

    private fun refreshAdAvailability() {
        val scopeId = session.scopeId
        adAvailable = scopeId != null && !session.doubled && session.report?.hasMeaningfulProgress == true &&
            contextualAds.availability(
                ContextualRewardedAdCoordinator.OFFLINE_DOUBLE_OFFER,
                scopeId,
            ).available
    }

    private fun continueToGame() {
        if (continued || transactionBlocked || adBusy) return
        continued = true
        hide()
        onContinue()
    }

    private fun lines(): List<String> {
        if (session.unrecoverable) {
            return listOf(
                "Aucune sauvegarde valide n’a pu être restaurée.",
                "Une nouvelle partie sera utilisée.",
                "Aucune ressource partielle n’a été attribuée.",
            )
        }
        val output = mutableListOf<String>()
        session.report?.let { report ->
            output += "Absence : ${formatDuration(report.absentSeconds)} · simulée : ${formatDuration(report.simulatedSeconds)}"
            output += "Extraction hors ligne : ${report.extractedByResource.values.sum()} unité(s)"
            output += "Productions terminées : RF ${report.refiningCompleted} · AS ${report.assemblyCompleted}"
            if (report.depletedDepositIds.isNotEmpty()) output += "${report.depletedDepositIds.size} gisement(s) épuisé(s)"
            if (report.storageBlockedDepositIds.isNotEmpty()) output += "Production arrêtée par stockage ou transport plein"
            if (report.capped) output += "Progression plafonnée à 8 heures"
            if (report.clockMovedBackward) output += "Horloge modifiée : aucun gain excessif appliqué"
        }
        if (session.doubled) output += "Bonus facultatif appliqué : production doublée"
        if (session.recoveredOlderSnapshot) output += "La dernière copie valide de la sauvegarde a été restaurée"
        if (session.migrated) output += "Sauvegarde mise à niveau vers le format actuel"
        if (session.saveFailed) output += "La progression est chargée, mais sa réécriture a échoué"
        if (message.isNotBlank()) output += message
        return output.ifEmpty { listOf("Progression restaurée") }
    }

    private fun layout(): ReturnLayout {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat()
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        val safeLeft = Gdx.graphics.safeInsetLeft * scaleX + 12f
        val safeRight = Gdx.graphics.safeInsetRight * scaleX + 12f
        val safeTop = Gdx.graphics.safeInsetTop * scaleY + 12f
        val safeBottom = Gdx.graphics.safeInsetBottom * scaleY + 12f
        val availableWidth = max(1f, width - safeLeft - safeRight)
        val availableHeight = max(1f, height - safeTop - safeBottom)
        val panelWidth = availableWidth.coerceAtMost(620f)
        val panelHeight = availableHeight.coerceAtMost(300f)
        val panel = Rectangle(
            safeLeft + (availableWidth - panelWidth) / 2f,
            safeBottom + (availableHeight - panelHeight) / 2f,
            panelWidth,
            panelHeight,
        )
        val continueButton = Rectangle(panel.x + panel.width - 154f, panel.y + 18f, 132f, 48f)
        val adButton = Rectangle(continueButton.x - 176f, panel.y + 18f, 164f, 48f)
        return ReturnLayout(panel, adButton, continueButton)
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        titleFont.dispose()
        font.dispose()
        smallFont.dispose()
    }

    private data class ReturnLayout(
        val panel: Rectangle,
        val adButton: Rectangle,
        val continueButton: Rectangle,
    )

    private companion object {
        fun formatDuration(seconds: Long): String = when {
            seconds >= 3_600L -> "${seconds / 3_600L} h ${seconds % 3_600L / 60L} min"
            seconds >= 60L -> "${seconds / 60L} min"
            else -> "$seconds s"
        }

        val BACKGROUND = Color(0.008f, 0.014f, 0.035f, 1f)
        val PANEL_SHADOW = Color(0.005f, 0.008f, 0.018f, 1f)
        val PANEL = Color(0.035f, 0.075f, 0.13f, 1f)
        val BUTTON = Color(0.08f, 0.18f, 0.26f, 1f)
        val DISABLED = Color(0.045f, 0.065f, 0.085f, 1f)
        val BORDER = Color(0.16f, 0.20f, 0.24f, 1f)
        val ACCENT = Color(0.20f, 0.82f, 0.88f, 1f)
        val REWARD = Color(0.30f, 0.90f, 0.64f, 1f)
        val TEXT = Color(0.90f, 0.96f, 1f, 1f)
        val MUTED = Color(0.61f, 0.72f, 0.82f, 1f)
    }
}
