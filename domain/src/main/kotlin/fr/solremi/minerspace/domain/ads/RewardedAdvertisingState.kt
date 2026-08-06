package fr.solremi.minerspace.domain.ads

import fr.solremi.minerspace.shared.GameId

data class PendingAdReward(
    val requestId: String,
    val offerId: GameId,
    val scopeId: String?,
    val status: PendingRewardStatus,
    val preparedAtEpochMillis: Long,
    val sdkRewardedAtEpochMillis: Long = 0L,
) {
    init {
        require(requestId.isNotBlank())
        require(preparedAtEpochMillis >= 0L && sdkRewardedAtEpochMillis >= 0L)
        if (status == PendingRewardStatus.SDK_REWARDED) {
            require(sdkRewardedAtEpochMillis >= preparedAtEpochMillis)
        }
    }
}

data class RewardEntitlements(
    val timeRelayTokens: Int = 0,
    val offlineDoubleTokens: Int = 0,
    val standardMaterialMinutes: Long = 0L,
    val premiumContractTokens: Int = 0,
    val analysisTokens: Int = 0,
    val meteorRecoveryTokens: Int = 0,
    val meteorExtensionSeconds: Long = 0L,
    val orbitalBoostPercent: Int = 0,
    val orbitalBoostUntilEpochMillis: Long = 0L,
) {
    init {
        require(timeRelayTokens >= 0 && offlineDoubleTokens >= 0 && standardMaterialMinutes >= 0L)
        require(premiumContractTokens >= 0 && analysisTokens >= 0 && meteorRecoveryTokens >= 0)
        require(meteorExtensionSeconds >= 0L && orbitalBoostPercent in 0..25 && orbitalBoostUntilEpochMillis >= 0L)
    }
}

data class RewardedAdvertisingState(
    val dayIndex: Long,
    val committedToday: Int,
    val committedByOffer: Map<GameId, Int>,
    val lastCommittedAtByOffer: Map<GameId, Long>,
    val committedRequestIds: Set<String>,
    val scopeCommittedByOffer: Map<String, Set<GameId>>,
    val pendingRewards: Map<String, PendingAdReward>,
    val entitlements: RewardEntitlements,
    val transactionSequence: Long,
) {
    init {
        require(dayIndex >= 0L && committedToday >= 0 && transactionSequence >= 0L)
        require(committedByOffer.values.none { it < 0 })
        require(lastCommittedAtByOffer.values.none { it < 0L })
        require(pendingRewards.keys.all { it.isNotBlank() })
    }
}

data class AdPlacementContext(
    val adsAllowed: Boolean,
    val sdkAvailable: Boolean,
    val tutorialActive: Boolean = false,
    val narrativeActive: Boolean = false,
    val majorAnimationActive: Boolean = false,
    val scopeId: String? = null,
)

data class RewardedAdTransaction(
    val sequence: Long,
    val reason: String,
    val requestId: String,
    val offerId: GameId,
)

sealed interface RewardedAdCommandResult {
    val state: RewardedAdvertisingState

    data class Applied(
        override val state: RewardedAdvertisingState,
        val transaction: RewardedAdTransaction,
    ) : RewardedAdCommandResult

    data class Rejected(
        override val state: RewardedAdvertisingState,
        val code: String,
    ) : RewardedAdCommandResult
}

data class RewardRecoveryResult(
    val state: RewardedAdvertisingState,
    val committedRequestIds: List<String>,
)

data class OfferAvailability(
    val available: Boolean,
    val reason: String?,
    val committedToday: Int,
    val offerCommittedToday: Int,
    val remainingCooldownMillis: Long,
)
