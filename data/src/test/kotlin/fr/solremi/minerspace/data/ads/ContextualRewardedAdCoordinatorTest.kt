package fr.solremi.minerspace.data.ads

import fr.solremi.minerspace.data.save.RewardedAdvertisingStateCodec
import fr.solremi.minerspace.domain.ads.RewardType
import fr.solremi.minerspace.domain.services.*
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.SilentGameLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContextualRewardedAdCoordinatorTest {
    @Test
    fun `event scope can be used once and a different event remains available`() {
        val fixture = Fixture()
        val coordinator = ContextualRewardedAdCoordinator(fixture.services)
        val offer = GameId.of("ad_meteor_drone")
        var result: ContextualRewardedResult? = null

        coordinator.watch(offer, "meteor_a") { result = it }

        assertTrue(result is ContextualRewardedResult.Granted)
        assertFalse(coordinator.availability(offer, "meteor_a").available)
        assertEquals("scope_already_used", coordinator.availability(offer, "meteor_a").reason)
        assertTrue(coordinator.availability(offer, "meteor_b").available)
    }

    @Test
    fun `entitlement and external payload are consumed atomically`() {
        val fixture = Fixture()
        val coordinator = ContextualRewardedAdCoordinator(fixture.services)
        var result: ContextualRewardedResult? = null
        coordinator.watch(
            ContextualRewardedAdCoordinator.METEOR_EXTENSION_OFFER,
            "meteor_a",
        ) { result = it }
        assertTrue(result is ContextualRewardedResult.Granted)
        assertTrue(coordinator.hasEntitlement(RewardType.METEOR_EXTENSION, 15L))

        val external = SavePayload(
            slotId = "meteor_event",
            schemaVersion = 1,
            contentVersion = "test",
            bytes = "extended".toByteArray(),
            savedAtEpochMillis = fixture.clock.nowEpochMillis(),
        )
        val consumed = coordinator.consumeWithPayload(
            rewardType = RewardType.METEOR_EXTENSION,
            amount = 15L,
            transactionId = "consume_meteor_extension_a",
            externalPayload = external,
        )

        assertEquals(EntitlementConsumptionResult.Committed, consumed)
        assertEquals("extended", fixture.save.loadLatest("meteor_event")!!.bytes.toString(Charsets.UTF_8))
        val rewarded = RewardedAdvertisingStateCodec().decode(
            fixture.save.loadLatest(RewardedAdvertisingStateCodec.SLOT_ID)!!,
        )
        assertEquals(0L, rewarded.entitlements.meteorExtensionSeconds)
    }

    private class Fixture {
        val clock = object : ClockService {
            override fun nowEpochMillis(): Long = 100_000L
            override fun monotonicMillis(): Long = 100_000L
        }
        val save = MemorySaveService()
        private val ads = object : RewardedAdsService {
            override fun isAvailable(offerId: String): Boolean = true
            override fun show(request: RewardedAdRequest, onResult: (RewardedAdResult) -> Unit) {
                onResult(RewardedAdResult.Granted(request.requestId))
            }
        }
        val services = GameServices(
            clock = clock,
            save = save,
            audio = NoAudio,
            haptic = NoHaptic,
            rewardedAds = ads,
            consent = GrantedConsent,
            notifications = NoNotifications,
            lifecycle = NoLifecycle,
            analytics = NoAnalytics,
            content = EmptyContent,
            remoteConfig = EmptyRemoteConfig,
            logger = SilentGameLogger,
        )
    }

    private class MemorySaveService : SaveService {
        private val values = linkedMapOf<String, SavePayload>()
        override fun loadLatest(slotId: String): SavePayload? = values[slotId]
        override fun save(payload: SavePayload): SaveWriteStatus {
            values[payload.slotId] = payload.copy(sequence = (values[payload.slotId]?.sequence ?: 0L) + 1L)
            return SaveWriteStatus.WRITTEN
        }
        override fun clear(slotId: String) { values.remove(slotId) }
    }

    private object NoAudio : AudioService {
        override fun setMusicEnabled(enabled: Boolean) = Unit
        override fun setSoundEnabled(enabled: Boolean) = Unit
        override fun pause() = Unit
        override fun resume() = Unit
    }
    private object NoHaptic : HapticService {
        override fun impact() = Unit
        override fun success() = Unit
        override fun warning() = Unit
    }
    private object GrantedConsent : ConsentService {
        override fun currentState(): ConsentState = ConsentState.GRANTED
        override fun requestIfNeeded(onComplete: (ConsentState) -> Unit) = onComplete(currentState())
    }
    private object NoNotifications : NotificationService {
        override fun schedule(request: NotificationRequest): Boolean = false
        override fun cancel(id: String) = Unit
        override fun cancelAll() = Unit
    }
    private object NoLifecycle : LifecycleService {
        override fun currentState(): LifecycleState = LifecycleState.FOREGROUND
        override fun addObserver(observer: LifecycleObserver) = Unit
        override fun removeObserver(observer: LifecycleObserver) = Unit
    }
    private object NoAnalytics : AnalyticsService {
        override fun event(name: String, attributes: Map<String, String>) = Unit
        override fun setEnabled(enabled: Boolean) = Unit
    }
    private object EmptyContent : ContentRepository {
        override fun readText(path: String): String? = null
    }
    private object EmptyRemoteConfig : RemoteConfigService {
        override fun boolean(key: String, defaultValue: Boolean): Boolean = defaultValue
        override fun long(key: String, defaultValue: Long): Long = defaultValue
        override fun string(key: String, defaultValue: String): String = defaultValue
    }
}
