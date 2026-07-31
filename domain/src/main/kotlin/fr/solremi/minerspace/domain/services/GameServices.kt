package fr.solremi.minerspace.domain.services

data class SavePayload(
    val slotId: String,
    val schemaVersion: Int,
    val contentVersion: String,
    val bytes: ByteArray,
    val savedAtEpochMillis: Long = 0L,
    val sequence: Long = 0L,
    val recoveredFromFallback: Boolean = false,
)

enum class SaveWriteStatus { WRITTEN, REJECTED, FAILED }
data class RewardedAdRequest(val offerId: String, val requestId: String)
sealed interface RewardedAdResult {
    data class Granted(val rewardId: String) : RewardedAdResult
    data object Unavailable : RewardedAdResult
    data object Cancelled : RewardedAdResult
    data class Failed(val reason: String) : RewardedAdResult
}
enum class ConsentState { UNKNOWN, REQUIRED, NOT_REQUIRED, GRANTED, DENIED }
data class NotificationRequest(val id: String, val titleKey: String, val bodyKey: String, val triggerAtEpochMillis: Long)
enum class LifecycleState { FOREGROUND, BACKGROUND }
enum class SoundCue { INTERACTION, PRODUCTION_COMPLETE, RARITY, ERROR, SECTOR_OPEN, LAUNCH }
fun interface LifecycleObserver { fun onStateChanged(state: LifecycleState) }
interface ClockService { fun nowEpochMillis(): Long; fun monotonicMillis(): Long }
interface SaveService { fun loadLatest(slotId: String = "primary"): SavePayload?; fun save(payload: SavePayload): SaveWriteStatus; fun clear(slotId: String = "primary") }
interface AudioService {
    fun setMusicEnabled(enabled: Boolean)
    fun setSoundEnabled(enabled: Boolean)
    fun setMasterVolume(volume: Float) = Unit
    fun play(cue: SoundCue) = Unit
    fun pause()
    fun resume()
}
interface HapticService {
    fun setEnabled(enabled: Boolean) = Unit
    fun impact()
    fun success()
    fun warning()
}
interface RewardedAdsService { fun isAvailable(offerId: String): Boolean; fun show(request: RewardedAdRequest, onResult: (RewardedAdResult) -> Unit) }
interface ConsentService { fun currentState(): ConsentState; fun requestIfNeeded(onComplete: (ConsentState) -> Unit) }
interface NotificationService { fun schedule(request: NotificationRequest): Boolean; fun cancel(id: String); fun cancelAll() }
interface LifecycleService { fun currentState(): LifecycleState; fun addObserver(observer: LifecycleObserver); fun removeObserver(observer: LifecycleObserver) }
interface AnalyticsService { fun event(name: String, attributes: Map<String, String> = emptyMap()); fun setEnabled(enabled: Boolean) }
interface ContentRepository { fun readText(path: String): String? }
interface RemoteConfigService { fun boolean(key: String, defaultValue: Boolean): Boolean; fun long(key: String, defaultValue: Long): Long; fun string(key: String, defaultValue: String): String }
data class GameServices(
    val clock: ClockService,
    val save: SaveService,
    val audio: AudioService,
    val haptic: HapticService,
    val rewardedAds: RewardedAdsService,
    val consent: ConsentService,
    val notifications: NotificationService,
    val lifecycle: LifecycleService,
    val analytics: AnalyticsService,
    val content: ContentRepository,
    val remoteConfig: RemoteConfigService,
)
