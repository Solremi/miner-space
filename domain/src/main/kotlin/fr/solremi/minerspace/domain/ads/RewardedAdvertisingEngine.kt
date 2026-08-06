package fr.solremi.minerspace.domain.ads

import fr.solremi.minerspace.shared.GameId

class RewardedAdvertisingEngine(val definitions: RewardedAdvertisingDefinitions) {
    fun initialState(dayIndex: Long): RewardedAdvertisingState {
        require(dayIndex >= 0L)
        return RewardedAdvertisingState(
            dayIndex = dayIndex,
            committedToday = 0,
            committedByOffer = emptyMap(),
            lastCommittedAtByOffer = emptyMap(),
            committedRequestIds = emptySet(),
            scopeCommittedByOffer = emptyMap(),
            pendingRewards = emptyMap(),
            entitlements = RewardEntitlements(),
            transactionSequence = 0L,
        )
    }

    fun normalize(source: RewardedAdvertisingState, nowEpochMillis: Long): RewardedAdvertisingState {
        require(nowEpochMillis >= 0L)
        val currentDay = dayIndex(nowEpochMillis)
        val dayState = if (source.dayIndex == currentDay) source else source.copy(
            dayIndex = currentDay,
            committedToday = 0,
            committedByOffer = emptyMap(),
        )
        val validPending = dayState.pendingRewards.filterValues { definitions.offers.containsKey(it.offerId) }
        val boostActive = dayState.entitlements.orbitalBoostUntilEpochMillis > nowEpochMillis
        return dayState.copy(
            committedToday = dayState.committedToday.coerceIn(0, definitions.globalDailyLimit),
            committedByOffer = dayState.committedByOffer
                .filterKeys(definitions.offers::containsKey)
                .mapValues { (id, value) -> value.coerceIn(0, definitions.offers.getValue(id).dailyLimit) },
            lastCommittedAtByOffer = dayState.lastCommittedAtByOffer.filterKeys(definitions.offers::containsKey),
            pendingRewards = validPending,
            entitlements = dayState.entitlements.copy(
                orbitalBoostPercent = if (boostActive) dayState.entitlements.orbitalBoostPercent.coerceIn(0, 25) else 0,
                orbitalBoostUntilEpochMillis = if (boostActive) dayState.entitlements.orbitalBoostUntilEpochMillis else 0L,
            ),
            transactionSequence = dayState.transactionSequence.coerceAtLeast(0L),
        )
    }

    fun availability(
        state: RewardedAdvertisingState,
        offerId: GameId,
        nowEpochMillis: Long,
        context: AdPlacementContext,
    ): OfferAvailability {
        val normalized = normalize(state, nowEpochMillis)
        val offer = definitions.offers[offerId]
            ?: return OfferAvailability(false, "unknown_offer", normalized.committedToday, 0, 0L)
        val reason = rejectionReason(normalized, offer, nowEpochMillis, context)
        val lastCommitted = normalized.lastCommittedAtByOffer[offerId]
        val remaining = if (lastCommitted == null) 0L else
            (offer.cooldownMillis - (nowEpochMillis - lastCommitted)).coerceAtLeast(0L)
        return OfferAvailability(
            available = reason == null,
            reason = reason,
            committedToday = normalized.committedToday,
            offerCommittedToday = normalized.committedByOffer[offerId] ?: 0,
            remainingCooldownMillis = remaining,
        )
    }

    fun prepare(
        state: RewardedAdvertisingState,
        offerId: GameId,
        nowEpochMillis: Long,
        context: AdPlacementContext,
    ): RewardedAdCommandResult {
        val normalized = normalize(state, nowEpochMillis)
        val offer = definitions.offers[offerId] ?: return rejected(normalized, "unknown_offer")
        rejectionReason(normalized, offer, nowEpochMillis, context)?.let { return rejected(normalized, it) }
        val sequence = Math.addExact(normalized.transactionSequence, 1L)
        val requestId = "rewarded_${sequence}_${offerId.value}_$nowEpochMillis"
        val pending = PendingAdReward(
            requestId = requestId,
            offerId = offerId,
            scopeId = context.scopeId,
            status = PendingRewardStatus.PREPARED,
            preparedAtEpochMillis = nowEpochMillis,
        )
        return applied(
            normalized.copy(
                pendingRewards = normalized.pendingRewards + (requestId to pending),
                transactionSequence = sequence,
            ),
            "prepare_rewarded_ad",
            requestId,
            offerId,
            sequence,
        )
    }

    fun markSdkRewarded(
        state: RewardedAdvertisingState,
        requestId: String,
        nowEpochMillis: Long,
    ): RewardedAdCommandResult {
        val normalized = normalize(state, nowEpochMillis)
        if (requestId in normalized.committedRequestIds) return rejected(normalized, "request_already_committed")
        val pending = normalized.pendingRewards[requestId] ?: return rejected(normalized, "unknown_request")
        if (pending.status == PendingRewardStatus.SDK_REWARDED) return rejected(normalized, "sdk_reward_already_recorded")
        val sequence = Math.addExact(normalized.transactionSequence, 1L)
        val rewarded = pending.copy(
            status = PendingRewardStatus.SDK_REWARDED,
            sdkRewardedAtEpochMillis = nowEpochMillis,
        )
        return applied(
            normalized.copy(
                pendingRewards = normalized.pendingRewards + (requestId to rewarded),
                transactionSequence = sequence,
            ),
            "record_sdk_reward",
            requestId,
            pending.offerId,
            sequence,
        )
    }

    fun commit(
        state: RewardedAdvertisingState,
        requestId: String,
        nowEpochMillis: Long,
    ): RewardedAdCommandResult {
        val normalized = normalize(state, nowEpochMillis)
        if (requestId in normalized.committedRequestIds) return rejected(normalized, "request_already_committed")
        val pending = normalized.pendingRewards[requestId] ?: return rejected(normalized, "unknown_request")
        if (pending.status != PendingRewardStatus.SDK_REWARDED) return rejected(normalized, "sdk_reward_required")
        val offer = definitions.offers.getValue(pending.offerId)
        if (normalized.committedToday >= definitions.globalDailyLimit) return rejected(normalized, "global_daily_limit")
        if ((normalized.committedByOffer[offer.id] ?: 0) >= offer.dailyLimit) return rejected(normalized, "offer_daily_limit")
        val sequence = Math.addExact(normalized.transactionSequence, 1L)
        val scopeMap = if (offer.scope == RewardScope.DAILY || pending.scopeId == null) {
            normalized.scopeCommittedByOffer
        } else {
            val key = scopeKey(offer.scope, pending.scopeId)
            normalized.scopeCommittedByOffer + (key to ((normalized.scopeCommittedByOffer[key] ?: emptySet()) + offer.id))
        }
        val next = normalized.copy(
            committedToday = normalized.committedToday + 1,
            committedByOffer = normalized.committedByOffer + (offer.id to ((normalized.committedByOffer[offer.id] ?: 0) + 1)),
            lastCommittedAtByOffer = normalized.lastCommittedAtByOffer + (offer.id to nowEpochMillis),
            committedRequestIds = normalized.committedRequestIds + requestId,
            scopeCommittedByOffer = scopeMap,
            pendingRewards = normalized.pendingRewards - requestId,
            entitlements = grant(normalized.entitlements, offer, nowEpochMillis),
            transactionSequence = sequence,
        )
        return applied(next, "commit_rewarded_ad", requestId, offer.id, sequence)
    }

    fun cancel(
        state: RewardedAdvertisingState,
        requestId: String,
        nowEpochMillis: Long,
    ): RewardedAdCommandResult {
        val normalized = normalize(state, nowEpochMillis)
        val pending = normalized.pendingRewards[requestId] ?: return rejected(normalized, "unknown_request")
        if (pending.status == PendingRewardStatus.SDK_REWARDED) return rejected(normalized, "rewarded_request_cannot_cancel")
        val sequence = Math.addExact(normalized.transactionSequence, 1L)
        return applied(
            normalized.copy(pendingRewards = normalized.pendingRewards - requestId, transactionSequence = sequence),
            "cancel_rewarded_ad",
            requestId,
            pending.offerId,
            sequence,
        )
    }

    fun recoverRewarded(state: RewardedAdvertisingState, nowEpochMillis: Long): RewardRecoveryResult {
        var current = normalize(state, nowEpochMillis)
        val committed = mutableListOf<String>()
        current.pendingRewards.values
            .filter { it.status == PendingRewardStatus.SDK_REWARDED }
            .sortedBy { it.preparedAtEpochMillis }
            .forEach { pending ->
                when (val result = commit(current, pending.requestId, nowEpochMillis)) {
                    is RewardedAdCommandResult.Applied -> {
                        current = result.state
                        committed += pending.requestId
                    }
                    is RewardedAdCommandResult.Rejected -> current = result.state
                }
            }
        return RewardRecoveryResult(current, committed)
    }

    fun consumeEntitlement(
        state: RewardedAdvertisingState,
        rewardType: RewardType,
        amount: Long = 1L,
        nowEpochMillis: Long,
    ): RewardedAdCommandResult {
        require(amount > 0L)
        val normalized = normalize(state, nowEpochMillis)
        val e = normalized.entitlements
        val next = when (rewardType) {
            RewardType.TIME_RELAY -> if (e.timeRelayTokens >= amount) e.copy(timeRelayTokens = e.timeRelayTokens - amount.toInt()) else null
            RewardType.OFFLINE_DOUBLE -> if (e.offlineDoubleTokens >= amount) e.copy(offlineDoubleTokens = e.offlineDoubleTokens - amount.toInt()) else null
            RewardType.STANDARD_MATERIALS -> if (e.standardMaterialMinutes >= amount) e.copy(standardMaterialMinutes = e.standardMaterialMinutes - amount) else null
            RewardType.PREMIUM_CONTRACT -> if (e.premiumContractTokens >= amount) e.copy(premiumContractTokens = e.premiumContractTokens - amount.toInt()) else null
            RewardType.ANALYSIS -> if (e.analysisTokens >= amount) e.copy(analysisTokens = e.analysisTokens - amount.toInt()) else null
            RewardType.METEOR_RECOVERY -> if (e.meteorRecoveryTokens >= amount) e.copy(meteorRecoveryTokens = e.meteorRecoveryTokens - amount.toInt()) else null
            RewardType.METEOR_EXTENSION -> if (e.meteorExtensionSeconds >= amount) e.copy(meteorExtensionSeconds = e.meteorExtensionSeconds - amount) else null
            RewardType.ORBITAL_BOOST -> null
        } ?: return rejected(normalized, "entitlement_insufficient")
        val sequence = Math.addExact(normalized.transactionSequence, 1L)
        return applied(
            normalized.copy(entitlements = next, transactionSequence = sequence),
            "consume_reward_entitlement",
            "consume_${sequence}_${rewardType.name.lowercase()}",
            definitions.offers.values.first { it.rewardType == rewardType }.id,
            sequence,
        )
    }

    private fun rejectionReason(
        state: RewardedAdvertisingState,
        offer: RewardedOfferDefinition,
        nowEpochMillis: Long,
        context: AdPlacementContext,
    ): String? {
        if (!context.adsAllowed) return "consent_required"
        if (!context.sdkAvailable) return "sdk_unavailable"
        if (context.tutorialActive) return "tutorial_active"
        if (context.narrativeActive) return "narrative_active"
        if (context.majorAnimationActive) return "major_animation_active"
        if (state.committedToday >= definitions.globalDailyLimit) return "global_daily_limit"
        if ((state.committedByOffer[offer.id] ?: 0) >= offer.dailyLimit) return "offer_daily_limit"
        if (state.pendingRewards.values.any { it.offerId == offer.id }) return "offer_request_pending"
        if (offer.scope != RewardScope.DAILY) {
            val scopeId = context.scopeId ?: return "scope_required"
            val committed = state.scopeCommittedByOffer[scopeKey(offer.scope, scopeId)].orEmpty()
            if (offer.id in committed) return "scope_already_used"
        }
        if (
            offer.rewardType == RewardType.ORBITAL_BOOST &&
            state.entitlements.orbitalBoostPercent > 0 &&
            state.entitlements.orbitalBoostUntilEpochMillis > nowEpochMillis
        ) {
            return "boost_already_active"
        }
        val last = state.lastCommittedAtByOffer[offer.id]
        if (last != null && offer.cooldownMillis > 0L && nowEpochMillis - last < offer.cooldownMillis) {
            return "cooldown_active"
        }
        return null
    }

    private fun grant(
        entitlements: RewardEntitlements,
        offer: RewardedOfferDefinition,
        nowEpochMillis: Long,
    ): RewardEntitlements = when (offer.rewardType) {
        RewardType.TIME_RELAY -> entitlements.copy(
            timeRelayTokens = Math.addExact(entitlements.timeRelayTokens, offer.rewardValue.toInt()),
        )
        RewardType.OFFLINE_DOUBLE -> entitlements.copy(
            offlineDoubleTokens = Math.addExact(entitlements.offlineDoubleTokens, offer.rewardValue.toInt()),
        )
        RewardType.STANDARD_MATERIALS -> entitlements.copy(
            standardMaterialMinutes = Math.addExact(entitlements.standardMaterialMinutes, offer.rewardValue),
        )
        RewardType.PREMIUM_CONTRACT -> entitlements.copy(
            premiumContractTokens = Math.addExact(entitlements.premiumContractTokens, offer.rewardValue.toInt()),
        )
        RewardType.ANALYSIS -> entitlements.copy(
            analysisTokens = Math.addExact(entitlements.analysisTokens, offer.rewardValue.toInt()),
        )
        RewardType.METEOR_RECOVERY -> entitlements.copy(
            meteorRecoveryTokens = Math.addExact(entitlements.meteorRecoveryTokens, 1),
        )
        RewardType.METEOR_EXTENSION -> entitlements.copy(
            meteorExtensionSeconds = Math.addExact(entitlements.meteorExtensionSeconds, offer.rewardValue),
        )
        RewardType.ORBITAL_BOOST -> entitlements.copy(
            orbitalBoostPercent = offer.rewardValue.toInt().coerceAtMost(25),
            orbitalBoostUntilEpochMillis = Math.addExact(nowEpochMillis, offer.rewardDurationMillis),
        )
    }

    private fun scopeKey(scope: RewardScope, scopeId: String): String = "${scope.name}:$scopeId"

    private fun applied(
        state: RewardedAdvertisingState,
        reason: String,
        requestId: String,
        offerId: GameId,
        sequence: Long,
    ) = RewardedAdCommandResult.Applied(
        state,
        RewardedAdTransaction(sequence, reason, requestId, offerId),
    )

    private fun rejected(state: RewardedAdvertisingState, code: String) =
        RewardedAdCommandResult.Rejected(state, code)

    companion object {
        const val DAY_MILLIS = 86_400_000L
        fun dayIndex(nowEpochMillis: Long): Long =
            Math.floorDiv(nowEpochMillis.coerceAtLeast(0L), DAY_MILLIS)
    }
}
