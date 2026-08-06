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
import fr.solremi.minerspace.data.ads.RewardedAdvertisingContentFactory
import fr.solremi.minerspace.data.save.RewardedAdvertisingStateCodec
import fr.solremi.minerspace.domain.ads.*
import fr.solremi.minerspace.domain.services.*
import ktx.app.KtxScreen
import kotlin.math.max

class RewardedAdsScreen(
    private val services: GameServices,
    private val onBack: () -> Unit,
) : KtxScreen {
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(640f, 320f, 960f, 540f, camera)
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val title = BitmapFont().apply { data.setScale(.88f) }
    private val font = BitmapFont().apply { data.setScale(.66f) }
    private val small = BitmapFont().apply { data.setScale(.53f) }
    private val definitions = RewardedAdvertisingContentFactory.create()
    private val engine = RewardedAdvertisingEngine(definitions)
    private val codec = RewardedAdvertisingStateCodec()
    private val offers = definitions.offers.values.sortedBy { it.id.value }
    private var state = loadState()
    private var selected = 0
    private var busy = false
    private var message = "Une publicité récompensée reste toujours facultative."
    private var current: Layout? = null
    private val lifecycle = LifecycleObserver { if (it == LifecycleState.BACKGROUND) saveState() }
    private val input = object : InputAdapter() {
        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            touch(Vector2(screenX.toFloat(), screenY.toFloat()).also(viewport::unproject))
            return true
        }
    }

    override fun show() {
        state = loadState()
        val recovery = engine.recoverRewarded(state, now())
        if (recovery.committedRequestIds.isNotEmpty()) {
            state = recovery.state
            saveState()
            message = "Récompense restaurée après interruption, sans duplication."
        }
        services.lifecycle.addObserver(lifecycle)
        Gdx.input.inputProcessor = input
    }

    override fun hide() {
        saveState()
        services.lifecycle.removeObserver(lifecycle)
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) = viewport.update(width.coerceAtLeast(1), height.coerceAtLeast(1), true)

    override fun render(delta: Float) {
        ScreenUtils.clear(BACKGROUND)
        viewport.apply(); camera.update()
        val layout = layout(); current = layout
        val offer = offers[selected]
        val consent = services.consent.currentState()
        val sdkAvailable = services.rewardedAds.isAvailable(offer.id.value)
        val availability = engine.availability(state, offer.id, now(), context(offer, consent, sdkAvailable))

        shapes.projectionMatrix = camera.combined
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = ORBIT; shapes.rect(0f, 0f, viewport.worldWidth, viewport.worldHeight)
        shapes.color = PANEL; shapes.rect(layout.panel.x, layout.panel.y, layout.panel.width, layout.panel.height)
        shapes.color = ACCENT; shapes.rect(layout.panel.x, layout.panel.y + layout.panel.height - 5f, layout.panel.width, 5f)
        layout.buttons.forEachIndexed { index, rect ->
            val enabled = !busy && (index != 2 || availability.available)
            shapes.color = if (enabled) BUTTON else DISABLED
            shapes.rect(rect.x, rect.y, rect.width, rect.height)
            shapes.color = when (index) { 2 -> REWARD; 3 -> PRIVACY; else -> ACCENT }
            shapes.rect(rect.x, rect.y, rect.width, 4f)
        }
        shapes.end()

        batch.projectionMatrix = camera.combined
        batch.begin()
        title.color = TEXT
        title.draw(batch, "TRANSMISSION COMMERCIALE ORBITALE", layout.panel.x + 16f, layout.panel.y + layout.panel.height - 24f)
        small.color = MUTED
        small.draw(batch, "PUBLICITÉ RÉCOMPENSÉE · choix facultatif · refus sans pénalité", layout.panel.x + 16f, layout.panel.y + layout.panel.height - 47f)
        font.color = TEXT
        font.draw(batch, offer.title, layout.panel.x + 16f, layout.panel.y + layout.panel.height - 78f)
        small.draw(batch, offer.rewardDescription, layout.panel.x + 16f, layout.panel.y + layout.panel.height - 101f)
        small.draw(batch, "Limite ${availability.offerCommittedToday}/${offer.dailyLimit} · globale ${availability.committedToday}/${definitions.globalDailyLimit}", layout.panel.x + 16f, layout.panel.y + layout.panel.height - 124f)
        small.draw(batch, "Portée ${scopeLabel(offer.scope)} · délai ${cooldown(availability.remainingCooldownMillis)}", layout.panel.x + 16f, layout.panel.y + layout.panel.height - 145f)
        small.color = if (availability.available) REWARD else WARNING
        small.draw(batch, availabilityText(availability, consent), layout.panel.x + 16f, layout.panel.y + layout.panel.height - 168f)
        small.color = MUTED
        small.draw(batch, entitlementSummary(), layout.panel.x + 16f, layout.panel.y + 57f)
        small.draw(batch, message, layout.panel.x + 16f, layout.panel.y + 36f)
        val labels = listOf("RETOUR", "OFFRE", if (busy) "CHARGEMENT" else "REGARDER", "CONFIDENTIALITÉ")
        layout.buttons.forEachIndexed { index, rect ->
            small.color = TEXT
            small.draw(batch, labels[index], rect.x + 8f, rect.y + 29f)
        }
        batch.end()
    }

    private fun touch(point: Vector2) {
        val layout = current ?: return
        when {
            layout.buttons[0].contains(point) -> onBack()
            layout.buttons[1].contains(point) && !busy -> {
                selected = (selected + 1) % offers.size
                message = "Offre suivante. La récompense exacte est affichée avant lecture."
                services.haptic.impact()
            }
            layout.buttons[2].contains(point) && !busy -> startSelectedOffer()
            layout.buttons[3].contains(point) && !busy -> openPrivacyOptions()
        }
    }

    private fun startSelectedOffer() {
        val offer = offers[selected]
        val consent = services.consent.currentState()
        val sdkAvailable = services.rewardedAds.isAvailable(offer.id.value)
        val context = context(offer, consent, sdkAvailable)
        when (val prepared = engine.prepare(state, offer.id, now(), context)) {
            is RewardedAdCommandResult.Rejected -> {
                message = rejection(prepared.code)
                services.haptic.warning()
            }
            is RewardedAdCommandResult.Applied -> {
                state = prepared.state
                if (!saveState()) {
                    state = (engine.cancel(state, prepared.transaction.requestId, now()) as? RewardedAdCommandResult.Applied)?.state ?: state
                    message = "Sauvegarde indisponible : aucune publicité lancée."
                    services.haptic.warning()
                    return
                }
                busy = true
                services.analytics.event("rewarded_ad_started", mapOf("offer" to offer.id.value))
                services.rewardedAds.show(
                    RewardedAdRequest(offer.id.value, prepared.transaction.requestId),
                ) { result -> Gdx.app.postRunnable { finishSdkResult(prepared.transaction.requestId, result) } }
            }
        }
    }

    private fun finishSdkResult(requestId: String, result: RewardedAdResult) {
        busy = false
        when (result) {
            is RewardedAdResult.Granted -> {
                if (result.rewardId != requestId) {
                    message = "Réponse publicitaire invalide : aucune récompense appliquée."
                    services.haptic.warning()
                    return
                }
                val rewarded = engine.markSdkRewarded(state, requestId, now())
                if (rewarded !is RewardedAdCommandResult.Applied) {
                    message = "Ce callback a déjà été traité."
                    return
                }
                state = rewarded.state
                if (!saveState()) {
                    message = "Récompense reçue, engagement différé jusqu’à la prochaine sauvegarde."
                    services.haptic.warning()
                    return
                }
                when (val committed = engine.commit(state, requestId, now())) {
                    is RewardedAdCommandResult.Rejected -> message = rejection(committed.code)
                    is RewardedAdCommandResult.Applied -> {
                        state = committed.state
                        saveState()
                        message = "Récompense engagée une seule fois."
                        services.analytics.event("rewarded_ad_committed", mapOf("offer" to committed.transaction.offerId.value))
                        services.haptic.success()
                    }
                }
            }
            RewardedAdResult.Cancelled -> cancelPrepared(requestId, "Publicité fermée : aucune limite consommée.")
            RewardedAdResult.Unavailable -> cancelPrepared(requestId, "Aucune publicité disponible. Le jeu normal reste accessible.")
            is RewardedAdResult.Failed -> cancelPrepared(requestId, "Échec réseau : aucune limite consommée.")
        }
    }

    private fun cancelPrepared(requestId: String, text: String) {
        when (val cancelled = engine.cancel(state, requestId, now())) {
            is RewardedAdCommandResult.Applied -> state = cancelled.state
            is RewardedAdCommandResult.Rejected -> state = cancelled.state
        }
        saveState()
        message = text
        services.analytics.event("rewarded_ad_not_committed")
    }

    private fun openPrivacyOptions() {
        if (!services.consent.privacyOptionsRequired()) {
            services.consent.requestIfNeeded { state ->
                Gdx.app.postRunnable { message = "Consentement : ${consentLabel(state)}" }
            }
            return
        }
        services.consent.showPrivacyOptions { state ->
            Gdx.app.postRunnable { message = "Préférences publicitaires : ${consentLabel(state)}" }
        }
    }

    private fun context(
        offer: RewardedOfferDefinition,
        consent: ConsentState,
        sdkAvailable: Boolean,
    ) = AdPlacementContext(
        adsAllowed = consent == ConsentState.GRANTED || consent == ConsentState.NOT_REQUIRED,
        sdkAvailable = sdkAvailable,
        scopeId = when (offer.scope) {
            RewardScope.DAILY -> null
            RewardScope.RETURN, RewardScope.EVENT -> null
        },
    )

    private fun loadState(): RewardedAdvertisingState {
        val initial = engine.initialState(RewardedAdvertisingEngine.dayIndex(now()))
        val payload = services.save.loadLatest(RewardedAdvertisingStateCodec.SLOT_ID) ?: return initial
        return runCatching { engine.normalize(codec.decode(payload), now()) }.getOrElse { initial }
    }

    private fun saveState(): Boolean = services.save.save(codec.encode(state, now())) == SaveWriteStatus.WRITTEN
    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)

    private fun entitlementSummary(): String {
        val e = state.entitlements
        return "Crédits : relais ${e.timeRelayTokens} · hors ligne ${e.offlineDoubleTokens} · matériaux ${e.standardMaterialMinutes} min · contrats ${e.premiumContractTokens} · analyses ${e.analysisTokens}"
    }

    private fun availabilityText(value: OfferAvailability, consent: ConsentState): String = when {
        value.available -> "Disponible maintenant"
        value.reason == "consent_required" -> "Consentement requis ou publicités refusées (${consentLabel(consent)})"
        value.reason == "scope_required" -> "Disponible uniquement dans le contexte correspondant"
        value.reason == "sdk_unavailable" -> "Annonce non chargée ou réseau indisponible"
        else -> rejection(value.reason.orEmpty())
    }

    private fun rejection(code: String): String = when (code) {
        "consent_required" -> "Le consentement ne permet pas de demander une publicité."
        "sdk_unavailable" -> "Aucune publicité disponible. Réessayez plus tard."
        "tutorial_active" -> "Aucune publicité pendant le tutoriel."
        "narrative_active" -> "Aucune publicité pendant une transmission narrative."
        "major_animation_active" -> "Aucune publicité pendant une grande animation."
        "global_daily_limit" -> "Plafond global quotidien atteint."
        "offer_daily_limit" -> "Limite quotidienne de cette offre atteinte."
        "cooldown_active" -> "Délai minimal encore actif."
        "scope_required" -> "Cette offre apparaît seulement au retour ou pendant l’événement concerné."
        "scope_already_used" -> "Offre déjà utilisée pour ce retour ou cet événement."
        "boost_already_active" -> "Un boost orbital est déjà actif."
        "offer_request_pending" -> "Une demande pour cette offre est déjà en attente."
        "request_already_committed" -> "Récompense déjà attribuée."
        else -> code.ifBlank { "Offre indisponible" }
    }

    private fun cooldown(millis: Long): String = if (millis <= 0L) "prêt" else "${(millis + 59_999L) / 60_000L} min"
    private fun scopeLabel(scope: RewardScope) = when (scope) { RewardScope.DAILY -> "quotidienne"; RewardScope.RETURN -> "par retour"; RewardScope.EVENT -> "par événement" }
    private fun consentLabel(state: ConsentState) = when (state) {
        ConsentState.GRANTED -> "accordé"
        ConsentState.NOT_REQUIRED -> "non requis"
        ConsentState.DENIED -> "refusé"
        ConsentState.REQUIRED -> "requis"
        ConsentState.UNKNOWN -> "inconnu"
    }

    private fun layout(): Layout {
        val width = viewport.worldWidth; val height = viewport.worldHeight
        val sx = width / Gdx.graphics.width.coerceAtLeast(1); val sy = height / Gdx.graphics.height.coerceAtLeast(1)
        val left = Gdx.graphics.safeInsetLeft * sx + 8f
        val right = max(left + 1f, width - Gdx.graphics.safeInsetRight * sx - 8f)
        val bottom = Gdx.graphics.safeInsetBottom * sy + 8f
        val top = max(bottom + 1f, height - Gdx.graphics.safeInsetTop * sy - 8f)
        val panel = Rectangle(left, bottom, right - left, top - bottom)
        val gap = 6f
        val buttonWidth = (panel.width - 32f - gap * 3f) / 4f
        val buttons = (0..3).map { index -> Rectangle(panel.x + 16f + index * (buttonWidth + gap), panel.y + 8f, buttonWidth, 48f) }
        return Layout(panel, buttons)
    }

    override fun dispose() {
        hide(); shapes.dispose(); batch.dispose(); title.dispose(); font.dispose(); small.dispose()
    }

    private data class Layout(val panel: Rectangle, val buttons: List<Rectangle>)

    private companion object {
        val BACKGROUND = Color(.004f, .012f, .026f, 1f)
        val ORBIT = Color(.012f, .035f, .070f, 1f)
        val PANEL = Color(.035f, .080f, .125f, .98f)
        val BUTTON = Color(.060f, .150f, .210f, 1f)
        val DISABLED = Color(.028f, .055f, .075f, 1f)
        val ACCENT = Color(.25f, .76f, .96f, 1f)
        val REWARD = Color(.30f, .90f, .64f, 1f)
        val PRIVACY = Color(.72f, .54f, .96f, 1f)
        val WARNING = Color(.96f, .48f, .25f, 1f)
        val TEXT = Color(.94f, .97f, 1f, 1f)
        val MUTED = Color(.64f, .75f, .84f, 1f)
    }
}
