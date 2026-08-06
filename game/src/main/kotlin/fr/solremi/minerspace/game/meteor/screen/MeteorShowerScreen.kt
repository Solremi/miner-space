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
import kotlin.math.max

class MeteorShowerScreen(
    private val services: GameServices,
    private val onExit: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply { data.setScale(.82f) }
    private val small = BitmapFont().apply { data.setScale(.64f) }
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
        ScreenUtils.clear(BACKGROUND)
        viewport.apply()
        camera.update()
        drawBackground()
        drawFragments()
        drawHud()
        if (codexOpen) drawCodex()
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

    private fun drawBackground() {
        val layout = layout()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = SKY
        shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.color = HORIZON
        shapes.rect(layout.play.x, layout.play.y, layout.play.width, layout.play.height * .24f)
        shapes.color = PLANET
        shapes.arc(
            layout.play.x + layout.play.width / 2f,
            layout.play.y - layout.play.height * .45f,
            layout.play.width * .62f,
            18f,
            144f,
            64,
        )
        shapes.color = STAR
        repeat(28) { index ->
            val x = layout.play.x + ((index * 83) % 997) / 997f * layout.play.width
            val y = layout.play.y + ((index * 47 + 31) % 991) / 991f * layout.play.height
            shapes.circle(x, y, if (index % 5 == 0) 1.5f else 1f, 8)
        }
        shapes.end()
    }

    private fun drawFragments() {
        if (eventState.phase != MeteorEventPhase.ACTIVE) return
        val play = layout().play
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        eventState.fragments.forEach { fragment ->
            val point = engine.position(fragment, eventState.elapsedActiveMillis)
            val x = play.x + point.xMillionths / NORMALIZED * play.width
            val y = play.y + point.yMillionths / NORMALIZED * play.height
            if (x !in play.x - 30f..play.x + play.width + 30f ||
                y !in play.y - 30f..play.y + play.height + 30f
            ) return@forEach

            val rare = fragment.kind == MeteorFragmentKind.RARE
            shapes.color = if (rare) RARE_TRAIL else STANDARD_TRAIL
            shapes.rectLine(x + 30f, y + 18f, x, y, if (rare) 5f else 3f)
            shapes.color = if (rare) RARE_GLOW else STANDARD_GLOW
            shapes.circle(x, y, if (rare) 20f else 13f, 20)
            shapes.color = if (rare) RARE_CORE else STANDARD_CORE
            shapes.circle(x, y, if (rare) 10f else 7f, 16)
        }
        shapes.end()
    }

    private fun drawHud() {
        val layout = layout()
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = HUD
        shapes.rect(layout.top.x, layout.top.y, layout.top.width, layout.top.height)
        shapes.rect(layout.bottom.x, layout.bottom.y, layout.bottom.width, layout.bottom.height)
        drawButton(layout.assist, eventState.phase == MeteorEventPhase.ACTIVE && !transactionBlocked)
        drawButton(layout.ad, adAvailable && !adBusy && !transactionBlocked)
        drawButton(layout.codex, !transactionBlocked)
        drawButton(layout.action, true)
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        val remaining = ((definition.durationMillis - eventState.elapsedActiveMillis).coerceAtLeast(0L) + 999L) / 1_000L
        font.draw(
            batch,
            "PLUIE MÉTÉORIQUE · ${remaining}s",
            layout.top.x + 12f,
            layout.top.y + layout.top.height - 14f,
        )
        small.color = MUTED
        small.draw(
            batch,
            "Fragments ${eventState.standardCollected} · Cœurs ${eventState.rareCollected} · actifs ${eventState.fragments.size}/${definition.maxActiveFragments}",
            layout.top.x + 12f,
            layout.top.y + 15f,
        )
        small.color = TEXT
        small.draw(batch, if (eventState.assistanceEnabled) "ASSIST. OUI" else "ASSIST. NON", layout.assist.x + 7f, layout.assist.y + 30f)
        small.draw(batch, if (adBusy) "PUB..." else "PUB +15S", layout.ad.x + 9f, layout.ad.y + 30f)
        small.draw(batch, "CODEX", layout.codex.x + 14f, layout.codex.y + 30f)
        small.draw(batch, actionLabel(), layout.action.x + 10f, layout.action.y + 30f)
        if (eventState.phase != MeteorEventPhase.ACTIVE) {
            drawSummaryText(layout)
        } else {
            small.color = MUTED
            small.draw(batch, message, layout.bottom.x + 8f, layout.bottom.y + layout.bottom.height - 10f)
        }
        batch.end()
    }

    private fun drawSummaryText(layout: Layout) {
        val panel = layout.play
        font.color = TEXT
        font.draw(batch, "RÉSUMÉ", panel.x + 24f, panel.y + panel.height - 28f)
        small.color = MUTED
        small.draw(batch, "Fragments standards : ${eventState.standardCollected}", panel.x + 24f, panel.y + panel.height - 62f)
        small.draw(batch, "Cœur météorique rare : ${eventState.rareCollected}", panel.x + 24f, panel.y + panel.height - 86f)
        small.draw(batch, "Entrées Codex : ${eventState.codexEntryIds.size}/3", panel.x + 24f, panel.y + panel.height - 110f)
        small.draw(batch, message, panel.x + 24f, panel.y + 28f)
    }

    private fun drawCodex() {
        val play = layout().play
        val panel = Rectangle(
            play.x + play.width * .12f,
            play.y + play.height * .10f,
            play.width * .76f,
            play.height * .80f,
        )
        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = CODEX_BG
        shapes.rect(panel.x, panel.y, panel.width, panel.height)
        shapes.color = ACCENT
        shapes.rect(panel.x, panel.y + panel.height - 4f, panel.width, 4f)
        shapes.end()
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = TEXT
        font.draw(batch, "CODEX TEMPORAIRE", panel.x + 18f, panel.y + panel.height - 22f)
        codexLine(panel, 54f, "Pluie météorique", MeteorEventEngine.CODEX_EVENT in eventState.codexEntryIds)
        codexLine(panel, 82f, "Fragment standard", MeteorEventEngine.CODEX_STANDARD in eventState.codexEntryIds)
        codexLine(panel, 110f, "Cœur météorique", MeteorEventEngine.CODEX_RARE in eventState.codexEntryIds)
        small.color = MUTED
        small.draw(batch, "Touchez CODEX pour fermer", panel.x + 18f, panel.y + 18f)
        batch.end()
    }

    private fun codexLine(panel: Rectangle, offset: Float, label: String, discovered: Boolean) {
        small.color = if (discovered) TEXT else MUTED
        small.draw(batch, (if (discovered) "DÉCOUVERT · " else "INCONNU · ") + label, panel.x + 18f, panel.y + panel.height - offset)
    }

    private fun drawButton(rect: Rectangle, enabled: Boolean) {
        shapes.color = if (enabled) BUTTON else DISABLED
        shapes.rect(rect.x, rect.y, rect.width, rect.height)
        shapes.color = if (enabled) ACCENT else BORDER
        shapes.rect(rect.x, rect.y, rect.width, 4f)
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
        val normalizedX = (((point.x - play.x) / play.width) * NORMALIZED).toInt().coerceIn(0, NORMALIZED.toInt())
        val normalizedY = (((point.y - play.y) / play.height) * NORMALIZED).toInt().coerceIn(0, NORMALIZED.toInt())
        val result = engine.capture(eventState, normalizedX, normalizedY)
        if (result.captured == null) return

        eventState = result.state
        message = if (result.captured == MeteorFragmentKind.RARE) "Cœur météorique récupéré" else "Fragment récupéré"
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
            eventState.phase == MeteorEventPhase.SUMMARY || eventState.phase == MeteorEventPhase.COMMITTING -> finalizeReward()
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
                message = if (result.code == "meteor_reward_storage_full") "Stockage des récompenses insuffisant" else result.code
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
        return newEvent().also { eventState = it; saveEvent() }
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

    private fun layout(): Layout {
        val width = viewport.worldWidth
        val height = viewport.worldHeight
        val scaleX = width / Gdx.graphics.width.coerceAtLeast(1).toFloat()
        val scaleY = height / Gdx.graphics.height.coerceAtLeast(1).toFloat()
        val left = Gdx.graphics.safeInsetLeft * scaleX + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * scaleX - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * scaleY + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * scaleY - 8f)
        val topBar = Rectangle(left, top - 50f, right - left, 50f)
        val bottomBar = Rectangle(left, bottom, right - left, 50f)
        val play = Rectangle(left, bottom + 56f, right - left, (top - bottom - 112f).coerceAtLeast(120f))
        val gap = 6f
        val action = Rectangle(right - 104f, bottom, 104f, 48f)
        val codex = Rectangle(action.x - gap - 76f, bottom, 76f, 48f)
        val ad = Rectangle(codex.x - gap - 94f, bottom, 94f, 48f)
        val assist = Rectangle(ad.x - gap - 104f, bottom, 104f, 48f)
        return Layout(topBar, bottomBar, play, assist, ad, codex, action)
    }

    override fun dispose() {
        hide()
        shapes.dispose()
        batch.dispose()
        font.dispose()
        small.dispose()
    }

    private inner class MeteorInput : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            val point = Vector2(screenX.toFloat(), screenY.toFloat())
            viewport.unproject(point)
            val layout = layout()
            when {
                layout.assist.contains(point) && eventState.phase == MeteorEventPhase.ACTIVE && !transactionBlocked -> {
                    eventState = engine.toggleAssistance(eventState)
                    message = if (eventState.assistanceEnabled) "Assistance activée" else "Assistance désactivée"
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

    private data class Layout(
        val top: Rectangle,
        val bottom: Rectangle,
        val play: Rectangle,
        val assist: Rectangle,
        val ad: Rectangle,
        val codex: Rectangle,
        val action: Rectangle,
    )

    private companion object {
        const val TAG = "MeteorShowerScreen"
        const val NORMALIZED = 1_000_000f
        const val AUTOSAVE_MILLIS = 2_000L
        const val EXTENSION_SECONDS = 15L
        const val EXTENSION_MILLIS = EXTENSION_SECONDS * 1_000L
        val BACKGROUND = Color(.004f, .008f, .025f, 1f)
        val SKY = Color(.012f, .025f, .070f, 1f)
        val HORIZON = Color(.070f, .055f, .080f, 1f)
        val PLANET = Color(.18f, .085f, .045f, 1f)
        val STAR = Color(.68f, .78f, .92f, 1f)
        val STANDARD_TRAIL = Color(.18f, .56f, .86f, .7f)
        val STANDARD_GLOW = Color(.20f, .72f, .98f, 1f)
        val STANDARD_CORE = Color(.78f, .94f, 1f, 1f)
        val RARE_TRAIL = Color(.76f, .34f, .95f, .8f)
        val RARE_GLOW = Color(.92f, .48f, 1f, 1f)
        val RARE_CORE = Color(1f, .88f, .34f, 1f)
        val HUD = Color(.020f, .045f, .085f, .96f)
        val BUTTON = Color(.075f, .17f, .25f, 1f)
        val DISABLED = Color(.045f, .065f, .085f, 1f)
        val BORDER = Color(.16f, .20f, .24f, 1f)
        val ACCENT = Color(.20f, .82f, .88f, 1f)
        val CODEX_BG = Color(.025f, .050f, .090f, .98f)
        val TEXT = Color(.90f, .96f, 1f, 1f)
        val MUTED = Color(.61f, .72f, .82f, 1f)
    }
}
