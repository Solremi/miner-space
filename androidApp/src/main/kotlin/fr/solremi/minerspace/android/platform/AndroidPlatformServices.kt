package fr.solremi.minerspace.android.platform

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import fr.solremi.minerspace.data.FileSaveService
import fr.solremi.minerspace.domain.presentation.FeedbackKind
import fr.solremi.minerspace.domain.services.*
import fr.solremi.minerspace.game.presentation.GameFeedbackBus
import fr.solremi.minerspace.shared.GameLogger
import java.util.concurrent.CopyOnWriteArraySet

class AndroidPlatformServices(private val context: Context) {
    private val lifecycle = AndroidLifecycleService()
    private val audio = AndroidAudioService()
    private val haptic = AndroidHapticService(context, audio)

    val services = GameServices(
        clock = AndroidClockService,
        save = FileSaveService(context.filesDir.resolve("saves").toPath()),
        audio = audio,
        haptic = haptic,
        rewardedAds = UnavailableRewardedAdsService,
        consent = PendingConsentService,
        notifications = DisabledNotificationService,
        lifecycle = lifecycle,
        analytics = DisabledAnalyticsService,
        content = AndroidAssetContentRepository(context),
        remoteConfig = LocalRemoteConfigService,
    )

    fun onForeground() { lifecycle.update(LifecycleState.FOREGROUND); services.audio.resume() }
    fun onBackground() { services.audio.pause(); lifecycle.update(LifecycleState.BACKGROUND) }
}

object AndroidGameLogger : GameLogger {
    override fun debug(tag: String, message: String) { Log.d(tag, message) }
    override fun info(tag: String, message: String) { Log.i(tag, message) }
    override fun warning(tag: String, message: String, cause: Throwable?) { Log.w(tag, message, cause) }
    override fun error(tag: String, message: String, cause: Throwable?) { Log.e(tag, message, cause) }
}

private object AndroidClockService : ClockService {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
}

private class AndroidAudioService : AudioService {
    private var soundEnabled = true
    private var paused = false
    private var volume = .75f
    private var generator: ToneGenerator? = null

    override fun setMusicEnabled(enabled: Boolean) = Unit
    override fun setSoundEnabled(enabled: Boolean) { soundEnabled = enabled }

    @Synchronized
    override fun setMasterVolume(volume: Float) {
        val normalized = volume.coerceIn(0f, 1f)
        if (this.volume == normalized) return
        this.volume = normalized
        generator?.release(); generator = null
    }

    @Synchronized
    override fun play(cue: SoundCue) {
        if (!soundEnabled || paused || volume <= 0f) return
        val pair = when (cue) {
            SoundCue.INTERACTION -> ToneGenerator.TONE_PROP_BEEP to 70
            SoundCue.PRODUCTION_COMPLETE -> ToneGenerator.TONE_PROP_ACK to 150
            SoundCue.RARITY -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD to 280
            SoundCue.ERROR -> ToneGenerator.TONE_SUP_ERROR to 190
            SoundCue.SECTOR_OPEN -> ToneGenerator.TONE_CDMA_CONFIRM to 240
            SoundCue.LAUNCH -> ToneGenerator.TONE_PROP_PROMPT to 260
        }
        toneGenerator().startTone(pair.first, pair.second)
    }

    override fun pause() { paused = true }
    override fun resume() { paused = false }

    private fun toneGenerator(): ToneGenerator = generator ?: ToneGenerator(
        AudioManager.STREAM_MUSIC,
        (volume * 100f).toInt().coerceIn(0, 100),
    ).also { generator = it }
}

private class AndroidHapticService(context: Context, private val audio: AudioService) : HapticService {
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private var enabled = true
    override fun setEnabled(enabled: Boolean) { this.enabled = enabled }

    override fun impact() {
        audio.play(SoundCue.INTERACTION)
        GameFeedbackBus.emit(FeedbackKind.INTERACTION, SystemClock.elapsedRealtime())
        vibrate(18L, 80)
    }

    override fun success() {
        audio.play(SoundCue.PRODUCTION_COMPLETE)
        GameFeedbackBus.emit(FeedbackKind.SUCCESS, SystemClock.elapsedRealtime())
        vibrate(35L, 120)
    }

    override fun warning() {
        audio.play(SoundCue.ERROR)
        GameFeedbackBus.emit(FeedbackKind.ERROR, SystemClock.elapsedRealtime())
        vibrate(55L, 150)
    }

    private fun vibrate(durationMillis: Long, amplitude: Int) {
        if (!enabled) return
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMillis, amplitude.coerceIn(1, 255)))
    }
}

private class AndroidLifecycleService : LifecycleService {
    private val observers = CopyOnWriteArraySet<LifecycleObserver>()
    private var state = LifecycleState.FOREGROUND
    override fun currentState(): LifecycleState = state
    override fun addObserver(observer: LifecycleObserver) { observers += observer }
    override fun removeObserver(observer: LifecycleObserver) { observers -= observer }
    fun update(newState: LifecycleState) {
        if (state == newState) return
        state = newState
        observers.forEach { it.onStateChanged(newState) }
    }
}

private class AndroidAssetContentRepository(private val context: Context) : ContentRepository {
    override fun readText(path: String): String? = runCatching {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrNull()
}

private object UnavailableRewardedAdsService : RewardedAdsService {
    override fun isAvailable(offerId: String): Boolean = false
    override fun show(request: RewardedAdRequest, onResult: (RewardedAdResult) -> Unit) { onResult(RewardedAdResult.Unavailable) }
}
private object PendingConsentService : ConsentService {
    override fun currentState(): ConsentState = ConsentState.UNKNOWN
    override fun requestIfNeeded(onComplete: (ConsentState) -> Unit) { onComplete(ConsentState.UNKNOWN) }
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
