package fr.solremi.minerspace.data.ads

import fr.solremi.minerspace.data.save.RewardedAdvertisingStateCodec
import fr.solremi.minerspace.domain.ads.*
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RewardedAdvertisingContentAndCodecTest {
    @Test fun `content matches monetization budget`() {
        val definitions = RewardedAdvertisingContentFactory.create()
        assertEquals("1.0.0", definitions.contentVersion)
        assertEquals(10, definitions.globalDailyLimit)
        assertEquals(8, definitions.offers.size)
        assertEquals(RewardType.entries.toSet(), definitions.offers.values.map { it.rewardType }.toSet())
        assertEquals(5, definitions.offers.getValue(GameId.of("ad_time_relay")).dailyLimit)
        assertEquals(1, definitions.offers.getValue(GameId.of("ad_offline_double")).dailyLimit)
        assertTrue(definitions.offers.values.none { it.rewardDescription.contains("Noyau", ignoreCase = true) })
    }

    @Test fun `pending rewarded state and entitlements survive round trip`() {
        val request = PendingAdReward(
            requestId = "request|safe",
            offerId = GameId.of("ad_supply_capsule"),
            scopeId = "return:42",
            status = PendingRewardStatus.SDK_REWARDED,
            preparedAtEpochMillis = 100,
            sdkRewardedAtEpochMillis = 200,
        )
        val state = RewardedAdvertisingState(
            dayIndex = 2,
            committedToday = 3,
            committedByOffer = mapOf(GameId.of("ad_supply_capsule") to 2),
            lastCommittedAtByOffer = mapOf(GameId.of("ad_supply_capsule") to 500),
            committedRequestIds = setOf("old|request"),
            scopeCommittedByOffer = mapOf("EVENT:event:1" to setOf(GameId.of("ad_meteor_extension"))),
            pendingRewards = mapOf(request.requestId to request),
            entitlements = RewardEntitlements(standardMaterialMinutes = 20, analysisTokens = 1),
            transactionSequence = 8,
        )
        val codec = RewardedAdvertisingStateCodec()
        assertEquals(state, codec.decode(codec.encode(state, 1_000)))
    }
}
