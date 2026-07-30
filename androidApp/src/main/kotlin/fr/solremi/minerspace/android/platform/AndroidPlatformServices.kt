package fr.solremi.minerspace.android.platform

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import fr.solremi.minerspace.data.FileSaveService
import fr.solremi.minerspace.domain.services.AnalyticsService
import fr.solremi.minerspace.domain.services.AudioService
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.ConsentService
import fr.solremi.minerspace.domain.services.ConsentState
import fr.solremi.minerspace.domain.services.ContentRepository
import fr.solremi.minerspace.domain.services.GameServices
import fr.solremi.minerspace.domain.services.HapticService
import fr.solremi.minerspace.domain.services.LifecycleObserver
import fr.solremi.minerspace.domain.services.LifecycleService
import fr.solremi.minerspace.domain.services.LifecycleState
import fr.solremi.minerspace.domain.services.NotificationRequest
import fr.solremi.minerspace.domain.services.NotificationService
import fr.solremi.minerspace.domain.services.RemoteConfigService
import fr.solremi.minerspace.domain.services.RewardedAdRequest
import fr.solremi.minerspace.domain.services.RewardedAdResult
import fr.solremi.minerspace.domain.services.RewardedAdsService
import fr.solremi.minerspace.shared.GameLogger
import java.util.concurrent.CopyOnWriteArraySet

class AndroidPlatformServices(
    private val context: Context,
) {
    private val lifecycle = AndroidLifecycleService()

    val services = GameServices(
        clock = AndroidClockService,
        save = FileSaveService(context.filesDir.resolve("saves").toPath()),
        audio = NoOpAudioService,
        haptic = AndroidHapticService(context),
        rewardedAds = UnavailableRewardedAdsService,
        consent = PendingConsentService,
        notifications = DisabledNotificationService,
        lifecycle = lifecycle,
        analytics = DisabledAnalyticsService,
        content = AndroidAssetContentRepository(context),
        remoteConfig = LocalRemoteConfigService,
    )

    fun onForeground() {
        lifecycle.update(LifecycleState.FOREGROUND)
        services.audio.resume()
    }

    fun onBackground() {
        services.audio.pause()
        lifecycle.update(LifecycleState.BACKGROUND)
    }
}

object AndroidGameLogger : GameLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warning(tag: String, message: String, cause: Throwable?) {
        Log.w(tag, message, cause)
    }

    override fun error(tag: String, message: String, cause: Throwable?) {
        Log.e(tag, message, cause)
    }
}

private object AndroidClockService : ClockService {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()

    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
}

private class AndroidHapticService(
    context: Context,
) : HapticService {
    private val vibrator = context.getSystemService(Vibrator::class.java)

    override fun impact() {
        vibrate(18L, 80)
    }

    override fun success() {
        vibrate(35L, 120)
    }

    override fun warning() {
        vibrate(55L, 150)
    }

    private fun vibrate(durationMillis: Long, amplitude: Int) {
        vibrator?.vibrate(
            VibrationEffect.createOneShot(
                durationMillis,
                amplitude.coerceIn(1, 255),
            ),
        )
    }
}

private class AndroidLifecycleService : LifecycleService {
    private val observers = CopyOnWriteArraySet<LifecycleObserver>()

    @Volatile
    private var state = LifecycleState.FOREGROUND

    override fun currentState(): LifecycleState = state

    override fun addObserver(observer: LifecycleObserver) {
        observers += observer
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observers -= observer
    }

    fun update(newState: LifecycleState) {
        if (state == newState) return
        state = newState
        observers.forEach { observer -> observer.onStateChanged(newState) }
    }
}

private class AndroidAssetContentRepository(
    private val context: Context,
) : ContentRepository {
    override fun readText(path: String): String? =
        runCatching {
            context.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrNull()
}

private object NoOpAudioService : AudioService {
    override fun setMusicEnabled(enabled: Boolean) = Unit
    override fun setSoundEnabled(enabled: Boolean) = Unit
    override fun pause() = Unit
    override fun resume() = Unit
}

private object UnavailableRewardedAdsService : RewardedAdsService {
    override fun isAvailable(offerId: String): Boolean = false

    override fun show(
        request: RewardedAdRequest,
        onResult: (RewardedAdResult) -> Unit,
    ) {
        onResult(RewardedAdResult.Unavailable)
    }
}

private object PendingConsentService : ConsentService {
    override fun currentState(): ConsentState = ConsentState.UNKNOWN

    override fun requestIfNeeded(onComplete: (ConsentState) -> Unit) {
        onComplete(ConsentState.UNKNOWN)
    }
}

private object DisabledNotificationService : NotificationService {
    override fun schedule(request: NotificationRequest): Boolean = false
    override fun cancel(id: String) = Unit
    override fun cancelAll() = Unit
}

private object DisabledAnalyticsService : AnalyticsService {
    override fun event(name: String, attributes: Map<String, String>) = Unit
    override fun setEnabled(enabled: Boolean) = Unit
}

private object LocalRemoteConfigService : RemoteConfigService {
    override fun boolean(key: String, defaultValue: Boolean): Boolean = defaultValue
    override fun long(key: String, defaultValue: Long): Long = defaultValue
    override fun string(key: String, defaultValue: String): String = defaultValue
}
