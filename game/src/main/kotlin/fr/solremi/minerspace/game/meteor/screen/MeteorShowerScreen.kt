package fr.solremi.minerspace.game.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import fr.solremi.minerspace.data.ads.ContextualRewardedAdCoordinator
import fr.solremi.minerspace.data.ads.ContextualRewardedResult
import fr.solremi.minerspace.data.ads.EntitlementConsumptionResult
import fr.solremi.minerspace.data.event.MeteorContentLoader
import fr.solremi.minerspace.data.event.MeteorRewardCommitResult
import fr.solremi.minerspace.data.event.MeteorRewardCoordinator
import fr.solremi.minerspace.data.save.MeteorEventCodec
import fr.solremi.minerspace.domain.ads.RewardType
import fr.solremi.minerspace.domain.event.MeteorEventEngine
import fr.solremi.minerspace.domain.event.MeteorEventPhase
import fr.solremi.minerspace.domain.event.MeteorEventState
import fr.solremi.minerspace.domain.event.MeteorFragmentKind
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import ktx.app.KtxScreen

class MeteorShowerScreen(
    private val services: GameServices,
    private val onExit: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val renderer = MeteorShowerRenderer()
    private val definition = MeteorContentLoader().load(services.content)
    private val engine = MeteorEventEngine(definition)
    private val eventCodec = MeteorEventCodec()
    private val rewardCoordinator = MeteorRewardCoordinator.fromServices(services, definition)
    private val contextualAds = ContextualRewardedAdCoordinator(services)

    private var eventState = newEvent()
    private var lastTickMillis = 0L
    private var lastSaveMillis = 0L
    private var codexOpen = false
    private var transactionBlocked = false
    private var adBusy = false
    private var adAvailable = false
    private var message = "Touchez ou glissez sur les fragments"
    private val input = MeteorInput()
    private val lifecycle = LifecycleObserver { state ->
        if (state == LifecycleState.BACKGROUND) saveEvent()
    }

    override fun show() {
        eventState = loadOrStartEvent()
        if (eventState.phase == MeteorEventPhase.COMMITTING) finalizeReward()
        applyPendingMeteorExtension()
        refreshAdAvailability()
        lastTickMillis = services.clock.monotonicMillis()
        lastSaveMillis = lastTickMillis
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
    }

    override fun hide() {
        if (!transactionBlocked) saveEvent()
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)
    }

    override fun render(delta: Float) {
        updateEvent()
        viewport.apply()
        camera.update()
        renderer.render(
            camera,
            viewport.worldWidth,
            viewport.worldHeight,
            layout(),
            MeteorShowerViewModel(
                event = eventState,
                message = message,
                codexOpen = codexOpen,
                transactionBlocked = transactionBlocked,
                adBusy = adBusy,
                adAvailable = adAvailable,
                actionLabel = actionLabel(),
            ),
            definition,
            engine,
        )
    }

    private fun updateEvent() {
        val now = services.clock.monotonicMillis()
        val elapsed = (now - lastTickMillis).coerceIn(0L, 250L)
        lastTickMillis = now
        if (transactionBlocked || eventState.phase != MeteorEventPhase.ACTIVE || elapsed <= 0L) return
        val before = eventState
        eventState = engine.advance(eventState, elapsed)
        if (before.phase == MeteorEventPhase.ACTIVE && eventState.phase == MeteorEventPhase.SUMMARY) {
            message = "Pluie terminée · récompenses prêtes"
            adAvailable = false
            services.haptic.success()
            saveEvent()
        } else if (eventState != before && now - lastSaveMillis >= AUTOSAVE_MILLIS) {
            saveEvent()
            lastSaveMillis = now
        }
    }

    private fun actionLabel(): String = when {
        transactionBlocked -> "RELANCER"
        eventState.phase == MeteorEventPhase.ACTIVE -> "PAUSE"
        eventState.phase == MeteorEventPhase.SUMMARY -> "RÉCUPÉRER"
        eventState.phase == MeteorEventPhase.COMMITTING -> "FINALISER"
        else -> "RETOUR"
    }

    private fun capture(screenX: Float, screenY: Float) {
        if (transactionBlocked || eventState.phase != MeteorEventPhase.ACTIVE || codexOpen || adBusy) return
        val point = Vector2(screenX, screenY)
        viewport.unproject(point)
        val play = layout().play
        if (!play.contains(point)) return
        val normalizedX = (((point.x - play.x) / play.width) * NORMALIZED)
            .toInt()
            .coerceIn(0, NORMALIZED.toInt())
        val normalizedY = (((point.y - play.y) / play.height) * NORMALIZED)
            .toInt()
            .coerceIn(0, NORMALIZED.toInt())
        val result = engine.capture(eventState, normalizedX, normalizedY)
        if (result.captured == null) return
        eventState = result.state
        message = if (result.captured == MeteorFragmentKind.RARE) {
            "Cœur météorique récupéré"
        } else {
            "Fragment récupéré"
        }
        if (result.captured == MeteorFragmentKind.RARE) services.haptic.success() else services.haptic.impact()
        saveEvent()
    }

    private fun startExtensionAd() {
        if (!adAvailable || adBusy || eventState.phase != MeteorEventPhase.ACTIVE) return
        adBusy = true
        message = "Chargement de la publicité facultative"
        contextualAds.watch(
            ContextualRewardedAdCoordinator.METEOR_EXTENSION_OFFER,
            eventState.eventId,
        ) { result ->
            Gdx.app.postRunnable {
                adBusy = false
                when (result) {
                    is ContextualRewardedResult.Granted -> applyPendingMeteorExtension()
                    is ContextualRewardedResult.Rejected -> {
                        message = result.code
                        refreshAdAvailability()
                        services.haptic.warning()
                    }
                    is ContextualRewardedResult.Cancelled -> {
                        message = "Publicité fermée · aucun avantage consommé"
                        refreshAdAvailability()
                    }
                    is ContextualRewardedResult.PersistenceFailed -> {
                        message = "Récompense reçue · reprise au prochain affichage"
                        services.haptic.warning()
                    }
                }
            }
        }
    }

    private fun applyPendingMeteorExtension() {
        if (!contextualAds.hasEntitlement(RewardType.METEOR_EXTENSION, EXTENSION_SECONDS)) return
        val candidate = eventState.copy(
            elapsedActiveMillis = (eventState.elapsedActiveMillis - EXTENSION_MILLIS).coerceAtLeast(0L),
            transactionSequence = Math.addExact(eventState.transactionSequence, 1L),
        )
        val payload = eventCodec.encode(
            state = candidate,
            contentVersion = definition.contentVersion,
            savedAtEpochMillis = services.clock.nowEpochMillis().coerceAtLeast(0L),
        )
        when (
            contextualAds.consumeWithPayload(
                rewardType = RewardType.METEOR_EXTENSION,
                amount = EXTENSION_SECONDS,
                transactionId = "consume_meteor_extension_${eventState.eventId}",
                externalPayload = payload,
            )
        ) {
            EntitlementConsumptionResult.Committed -> {
                eventState = candidate
                transactionBlocked = false
                message = "+15 secondes ajoutées à cet événement"
                services.haptic.success()
                refreshAdAvailability()
            }
            is EntitlementConsumptionResult.Rejected -> {
                message = "Avantage météorique indisponible"
                refreshAdAvailability()
            }
            is EntitlementConsumptionResult.Pending -> {
                transactionBlocked = true
                message = "Prolongation interrompue · redémarrez pour reprendre"
                services.haptic.warning()
            }
        }
    }

    private fun refreshAdAvailability() {
        adAvailable = eventState.phase == MeteorEventPhase.ACTIVE &&
            contextualAds.availability(
                ContextualRewardedAdCoordinator.METEOR_EXTENSION_OFFER,
                eventState.eventId,
            ).available
    }

    private fun performAction() {
        when {
            transactionBlocked -> finalizeReward()
            eventState.phase == MeteorEventPhase.ACTIVE -> {
                saveEvent()
                onExit()
            }
            eventState.phase == MeteorEventPhase.SUMMARY ||
                eventState.phase == MeteorEventPhase.COMMITTING -> finalizeReward()
            eventState.phase == MeteorEventPhase.COMMITTED -> {
                services.save.clear(MeteorEventCodec.SLOT_ID)
                onExit()
            }
        }
    }

    private fun finalizeReward() {
        when (val result = rewardCoordinator.commit(rewardCoordinator.loadMain(), eventState)) {
            is MeteorRewardCommitResult.Committed -> {
                transactionBlocked = false
                eventState = result.event
                message = "Récompenses attribuées une seule fois"
                services.haptic.success()
            }
            is MeteorRewardCommitResult.Rejected -> {
                transactionBlocked = false
                message = if (result.code == "meteor_reward_storage_full") {
                    "Stockage des récompenses insuffisant"
                } else {
                    result.code
                }
                services.haptic.warning()
            }
            is MeteorRewardCommitResult.Pending -> {
                transactionBlocked = true
                message = "Finalisation interrompue · relancez ou redémarrez"
                services.haptic.warning()
            }
        }
    }

    private fun loadOrStartEvent(): MeteorEventState {
        val payload = services.save.loadLatest(MeteorEventCodec.SLOT_ID)
        if (payload != null) {
            val restored = runCatching {
                require(payload.contentVersion == definition.contentVersion)
                eventCodec.decode(payload)
            }.onFailure {
                services.logger.warning(TAG, "Meteor event save could not be decoded.", it)
            }.getOrNull()
            if (restored != null && restored.phase != MeteorEventPhase.COMMITTED) return restored
        }
        return newEvent().also {
            eventState = it
            saveEvent()
        }
    }

    private fun newEvent(): MeteorEventState {
        val now = services.clock.nowEpochMillis().coerceAtLeast(1L)
        return engine.start("meteor_$now", now xor services.clock.monotonicMillis(), true)
    }

    private fun saveEvent(): Boolean {
        val status = runCatching {
            services.save.save(
                eventCodec.encode(
                    eventState,
                    definition.contentVersion,
                    services.clock.nowEpochMillis().coerceAtLeast(0L),
                ),
            )
        }.onFailure {
            services.logger.error(TAG, "Unable to persist meteor event.", it)
        }.getOrElse { SaveWriteStatus.FAILED }
        return status == SaveWriteStatus.WRITTEN
    }

    private fun layout(): MeteorShowerLayout =
        MeteorShowerLayoutCalculator.calculate(viewport.worldWidth, viewport.worldHeight)

    override fun dispose() {
        hide()
        renderer.dispose()
    }

    private inner class MeteorInput : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat())
            viewport.unproject(point)
            val layout = layout()
            when {
                layout.assist.contains(point) &&
                    eventState.phase == MeteorEventPhase.ACTIVE &&
                    !transactionBlocked -> {
                    eventState = engine.toggleAssistance(eventState)
                    message = if (eventState.assistanceEnabled) {
                        "Assistance activée"
                    } else {
                        "Assistance désactivée"
                    }
                    saveEvent()
                    services.haptic.impact()
                }
                layout.ad.contains(point) -> startExtensionAd()
                layout.codex.contains(point) && !transactionBlocked -> {
                    codexOpen = !codexOpen
                    services.haptic.impact()
                }
                layout.action.contains(point) -> performAction()
                else -> capture(screenX.toFloat(), screenY.toFloat())
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            capture(screenX.toFloat(), screenY.toFloat())
            return true
        }
    }

    private companion object {
        const val TAG = "MeteorShowerScreen"
        const val NORMALIZED = 1_000_000f
        const val AUTOSAVE_MILLIS = 2_000L
        const val EXTENSION_SECONDS = 15L
        const val EXTENSION_MILLIS = EXTENSION_SECONDS * 1_000L
    }
}
