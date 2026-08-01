package fr.solremi.minerspace.data.ads

import fr.solremi.minerspace.data.save.RewardedAdvertisingStateCodec
import fr.solremi.minerspace.data.transaction.SaveMutation
import fr.solremi.minerspace.data.transaction.SaveTransactionCoordinator
import fr.solremi.minerspace.data.transaction.SaveTransactionStatus
import fr.solremi.minerspace.domain.ads.AdPlacementContext
import fr.solremi.minerspace.domain.ads.OfferAvailability
import fr.solremi.minerspace.domain.ads.RewardEntitlements
import fr.solremi.minerspace.domain.ads.RewardType
import fr.solremi.minerspace.domain.ads.RewardedAdCommandResult
import fr.solremi.minerspace.domain.ads.RewardedAdvertisingEngine
import fr.solremi.minerspace.domain.ads.RewardedAdvertisingState
import fr.solremi.minerspace.domain.services.ConsentState
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.RewardedAdRequest
import fr.solremi.minerspace.domain.services.RewardedAdResult
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId

sealed interface ContextualRewardedResult {
    data class Granted(
        val offerId: GameId,
        val scopeId: String,
        val entitlements: RewardEntitlements,
    ) : ContextualRewardedResult

    data class Rejected(val code: String) : ContextualRewardedResult
    data class Cancelled(val code: String) : ContextualRewardedResult
    data class PersistenceFailed(val stage: String) : ContextualRewardedResult
}

sealed interface EntitlementConsumptionResult {
    data object Committed : EntitlementConsumptionResult
    data class Rejected(val code: String) : EntitlementConsumptionResult
    data class Pending(val failedSlotId: String?) : EntitlementConsumptionResult
}

class ContextualRewardedAdCoordinator(
    private val services: GameServices,
) {
    private val definitions = RewardedAdvertisingContentFactory.create()
    private val engine = RewardedAdvertisingEngine(definitions)
    private val codec = RewardedAdvertisingStateCodec()

    fun availability(offerId: GameId, scopeId: String): OfferAvailability {
        require(scopeId.isNotBlank())
        val state = loadAndRecover()
        return engine.availability(
            state = state,
            offerId = offerId,
            nowEpochMillis = now(),
            context = placement(offerId, scopeId),
        )
    }

    fun hasEntitlement(type: RewardType, amount: Long = 1L): Boolean {
        val entitlements = loadAndRecover().entitlements
        return when (type) {
            RewardType.TIME_RELAY -> entitlements.timeRelayTokens >= amount
            RewardType.OFFLINE_DOUBLE -> entitlements.offlineDoubleTokens >= amount
            RewardType.STANDARD_MATERIALS -> entitlements.standardMaterialMinutes >= amount
            RewardType.PREMIUM_CONTRACT -> entitlements.premiumContractTokens >= amount
            RewardType.ANALYSIS -> entitlements.analysisTokens >= amount
            RewardType.METEOR_RECOVERY -> entitlements.meteorRecoveryTokens >= amount
            RewardType.METEOR_EXTENSION -> entitlements.meteorExtensionSeconds >= amount
            RewardType.ORBITAL_BOOST -> entitlements.orbitalBoostUntilEpochMillis > now()
        }
    }

    fun watch(
        offerId: GameId,
        scopeId: String,
        onComplete: (ContextualRewardedResult) -> Unit,
    ) {
        require(scopeId.isNotBlank())
        val consent = services.consent.currentState()
        if (consent == ConsentState.UNKNOWN || consent == ConsentState.REQUIRED) {
            services.consent.requestIfNeeded {
                watchPrepared(offerId, scopeId, onComplete)
            }
        } else {
            watchPrepared(offerId, scopeId, onComplete)
        }
    }

    fun consumeWithPayload(
        rewardType: RewardType,
        amount: Long,
        transactionId: String,
        externalPayload: SavePayload,
    ): EntitlementConsumptionResult {
        require(amount > 0L)
        require(transactionId.isNotBlank())
        require(externalPayload.slotId != RewardedAdvertisingStateCodec.SLOT_ID)
        val state = loadAndRecover()
        val consumed = engine.consumeEntitlement(state, rewardType, amount, now())
        if (consumed is RewardedAdCommandResult.Rejected) {
            return EntitlementConsumptionResult.Rejected(consumed.code)
        }
        consumed as RewardedAdCommandResult.Applied
        val savedAt = now()
        val transaction = SaveTransactionCoordinator(services.save).execute(
            transactionId = transactionId,
            mutations = listOf(
                SaveMutation.write(externalPayload),
                SaveMutation.write(codec.encode(consumed.state, savedAt)),
            ),
            nowEpochMillis = savedAt,
        )
        val final = if (
            transaction.status == SaveTransactionStatus.PENDING &&
            transaction.transactionId == transactionId
        ) {
            SaveTransactionCoordinator(services.save).recoverPending()
        } else {
            transaction
        }
        return if (final.status == SaveTransactionStatus.COMMITTED) {
            EntitlementConsumptionResult.Committed
        } else {
            EntitlementConsumptionResult.Pending(final.failedSlotId)
        }
    }

    private fun watchPrepared(
        offerId: GameId,
        scopeId: String,
        onComplete: (ContextualRewardedResult) -> Unit,
    ) {
        var state = loadAndRecover()
        val prepared = engine.prepare(state, offerId, now(), placement(offerId, scopeId))
        if (prepared is RewardedAdCommandResult.Rejected) {
            onComplete(ContextualRewardedResult.Rejected(prepared.code))
            return
        }
        prepared as RewardedAdCommandResult.Applied
        state = prepared.state
        if (!saveState(state)) {
            onComplete(ContextualRewardedResult.PersistenceFailed("prepare"))
            return
        }

        services.rewardedAds.show(
            RewardedAdRequest(offerId.value, prepared.transaction.requestId),
        ) { sdkResult ->
            when (sdkResult) {
                is RewardedAdResult.Granted -> {
                    if (sdkResult.rewardId != prepared.transaction.requestId) {
                        onComplete(ContextualRewardedResult.Rejected("invalid_reward_id"))
                        return@show
                    }
                    val marked = engine.markSdkRewarded(state, prepared.transaction.requestId, now())
                    if (marked !is RewardedAdCommandResult.Applied) {
                        onComplete(ContextualRewardedResult.Rejected("reward_callback_rejected"))
                        return@show
                    }
                    state = marked.state
                    if (!saveState(state)) {
                        onComplete(ContextualRewardedResult.PersistenceFailed("sdk_reward"))
                        return@show
                    }
                    val committed = engine.commit(state, prepared.transaction.requestId, now())
                    if (committed !is RewardedAdCommandResult.Applied) {
                        val code = (committed as RewardedAdCommandResult.Rejected).code
                        onComplete(ContextualRewardedResult.Rejected(code))
                        return@show
                    }
                    state = committed.state
                    if (!saveState(state)) {
                        onComplete(ContextualRewardedResult.PersistenceFailed("commit"))
                        return@show
                    }
                    onComplete(
                        ContextualRewardedResult.Granted(
                            offerId = offerId,
                            scopeId = scopeId,
                            entitlements = state.entitlements,
                        ),
                    )
                }

                RewardedAdResult.Cancelled -> {
                    cancelPrepared(state, prepared.transaction.requestId)
                    onComplete(ContextualRewardedResult.Cancelled("cancelled"))
                }

                RewardedAdResult.Unavailable -> {
                    cancelPrepared(state, prepared.transaction.requestId)
                    onComplete(ContextualRewardedResult.Cancelled("sdk_unavailable"))
                }

                is RewardedAdResult.Failed -> {
                    cancelPrepared(state, prepared.transaction.requestId)
                    onComplete(ContextualRewardedResult.Cancelled("sdk_failed"))
                }
            }
        }
    }

    private fun placement(offerId: GameId, scopeId: String): AdPlacementContext = AdPlacementContext(
        adsAllowed = services.consent.currentState().let {
            it == ConsentState.GRANTED || it == ConsentState.NOT_REQUIRED
        },
        sdkAvailable = services.rewardedAds.isAvailable(offerId.value),
        scopeId = scopeId,
    )

    private fun loadAndRecover(): RewardedAdvertisingState {
        val initial = engine.initialState(RewardedAdvertisingEngine.dayIndex(now()))
        val loaded = services.save.loadLatest(RewardedAdvertisingStateCodec.SLOT_ID)?.let { payload ->
            runCatching { engine.normalize(codec.decode(payload), now()) }
                .onFailure { services.logger.warning(TAG, "Rewarded ad save is invalid.", it) }
                .getOrNull()
        } ?: initial
        val recovery = engine.recoverRewarded(loaded, now())
        if (recovery.state != loaded) saveState(recovery.state)
        return recovery.state
    }

    private fun cancelPrepared(state: RewardedAdvertisingState, requestId: String) {
        val cancelled = engine.cancel(state, requestId, now())
        if (cancelled is RewardedAdCommandResult.Applied) saveState(cancelled.state)
    }

    private fun saveState(state: RewardedAdvertisingState): Boolean = runCatching {
        services.save.save(codec.encode(state, now())) == SaveWriteStatus.WRITTEN
    }.onFailure {
        services.logger.error(TAG, "Unable to persist rewarded ad state.", it)
    }.getOrDefault(false)

    private fun now(): Long = services.clock.nowEpochMillis().coerceAtLeast(0L)

    companion object {
        private const val TAG = "ContextualRewardedAds"
        val OFFLINE_DOUBLE_OFFER: GameId = GameId.of("ad_offline_double")
        val METEOR_EXTENSION_OFFER: GameId = GameId.of("ad_meteor_extension")
    }
}
