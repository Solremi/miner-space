package fr.solremi.minerspace.android.platform

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import fr.solremi.minerspace.android.R
import fr.solremi.minerspace.domain.services.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class AndroidRewardedAdsService(
    private val activity: Activity,
    private val canRequestAds: () -> Boolean,
) : RewardedAdsService {
    private val loaded = ConcurrentHashMap<String, RewardedAd>()
    private val loading = ConcurrentHashMap.newKeySet<String>()
    private val initialized = AtomicBoolean(false)
    private val initializing = AtomicBoolean(false)

    fun setConsentAvailable(available: Boolean) {
        if (!available) {
            loaded.clear()
            return
        }
        initialize()
    }

    override fun isAvailable(offerId: String): Boolean {
        if (!canRequestAds()) return false
        initialize()
        if (initialized.get() && offerId !in loaded) load(offerId)
        return offerId in loaded
    }

    override fun show(request: RewardedAdRequest, onResult: (RewardedAdResult) -> Unit) {
        activity.runOnUiThread {
            if (!canRequestAds()) {
                onResult(RewardedAdResult.Unavailable)
                return@runOnUiThread
            }
            val ad = loaded.remove(request.offerId)
            if (ad == null) {
                load(request.offerId)
                onResult(RewardedAdResult.Unavailable)
                return@runOnUiThread
            }
            val completed = AtomicBoolean(false)
            val rewarded = AtomicBoolean(false)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (completed.compareAndSet(false, true)) {
                        onResult(if (rewarded.get()) RewardedAdResult.Granted(request.requestId) else RewardedAdResult.Cancelled)
                    }
                    load(request.offerId)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (completed.compareAndSet(false, true)) onResult(RewardedAdResult.Failed(error.message))
                    load(request.offerId)
                }
            }
            ad.show(activity) {
                rewarded.set(true)
                if (completed.compareAndSet(false, true)) onResult(RewardedAdResult.Granted(request.requestId))
            }
        }
    }

    private fun initialize() {
        if (initialized.get() || !canRequestAds() || !initializing.compareAndSet(false, true)) return
        activity.runOnUiThread {
            MobileAds.initialize(activity) {
                initialized.set(true)
                initializing.set(false)
            }
        }
    }

    private fun load(offerId: String) {
        if (!initialized.get() || !canRequestAds() || offerId in loaded || !loading.add(offerId)) return
        activity.runOnUiThread {
            RewardedAd.load(
                activity,
                activity.getString(R.string.admob_rewarded_unit_id),
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        loading.remove(offerId)
                        loaded[offerId] = ad
                    }

                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        loading.remove(offerId)
                    }
                },
            )
        }
    }
}

internal class AndroidConsentService(
    private val activity: Activity,
    private val onPermissionChanged: (Boolean) -> Unit,
) : ConsentService {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    private val preferences = activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun currentState(): ConsentState {
        val persisted = runCatching {
            ConsentState.valueOf(preferences.getString(KEY_STATE, ConsentState.UNKNOWN.name)!!)
        }.getOrDefault(ConsentState.UNKNOWN)
        val live = mapState(consentInformation.consentStatus)
        return if (live != ConsentState.UNKNOWN) live else persisted
    }

    override fun requestIfNeeded(onComplete: (ConsentState) -> Unit) {
        activity.runOnUiThread {
            val parameters = ConsentRequestParameters.Builder().build()
            consentInformation.requestConsentInfoUpdate(
                activity,
                parameters,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                        finish(onComplete)
                    }
                },
                {
                    val state = currentState()
                    onPermissionChanged(consentInformation.canRequestAds())
                    onComplete(state)
                },
            )
        }
    }

    override fun privacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    override fun showPrivacyOptions(onComplete: (ConsentState) -> Unit) {
        activity.runOnUiThread {
            if (!privacyOptionsRequired()) {
                onComplete(currentState())
                return@runOnUiThread
            }
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { finish(onComplete) }
        }
    }

    private fun finish(onComplete: (ConsentState) -> Unit) {
        val state = if (!consentInformation.canRequestAds() &&
            consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
        ) ConsentState.DENIED else mapState(consentInformation.consentStatus)
        preferences.edit()
            .putString(KEY_STATE, state.name)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
        onPermissionChanged(consentInformation.canRequestAds())
        onComplete(state)
    }

    private fun mapState(status: Int): ConsentState = when (status) {
        ConsentInformation.ConsentStatus.OBTAINED -> ConsentState.GRANTED
        ConsentInformation.ConsentStatus.NOT_REQUIRED -> ConsentState.NOT_REQUIRED
        ConsentInformation.ConsentStatus.REQUIRED -> ConsentState.REQUIRED
        else -> ConsentState.UNKNOWN
    }

    private companion object {
        const val PREFERENCES = "miner_space_ad_consent"
        const val KEY_STATE = "state"
        const val KEY_UPDATED_AT = "updated_at"
    }
}
