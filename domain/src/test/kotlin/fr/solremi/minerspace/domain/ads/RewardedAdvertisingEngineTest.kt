package fr.solremi.minerspace.domain.ads

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RewardedAdvertisingEngineTest {
    private val definitions = RewardedAdvertisingDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        globalDailyLimit = 10,
        offers = listOf(
            offer("ad_time_relay", RewardType.TIME_RELAY, 5, 10, RewardScope.DAILY),
            offer("ad_offline_double", RewardType.OFFLINE_DOUBLE, 1, 0, RewardScope.RETURN),
            offer("ad_supply_capsule", RewardType.STANDARD_MATERIALS, 3, 20, RewardScope.DAILY, 10),
            offer("ad_premium_contract", RewardType.PREMIUM_CONTRACT, 2, 30, RewardScope.DAILY),
            offer("ad_analysis_beacon", RewardType.ANALYSIS, 2, 30, RewardScope.DAILY),
            offer("ad_meteor_drone", RewardType.METEOR_RECOVERY, 2, 0, RewardScope.EVENT, 25),
            offer("ad_meteor_extension", RewardType.METEOR_EXTENSION, 1, 0, RewardScope.EVENT, 15),
            RewardedOfferDefinition(GameId.of("ad_orbital_boost"), "boost", "boost", RewardType.ORBITAL_BOOST, 25, 900_000, 2, 3_600_000, RewardScope.DAILY),
        ).associateBy { it.id },
    )
    private val engine = RewardedAdvertisingEngine(definitions)
    private val capsule = GameId.of("ad_supply_capsule")
    private val context = AdPlacementContext(adsAllowed = true, sdkAvailable = true)

    private fun offer(id: String, type: RewardType, limit: Int, cooldownMinutes: Long, scope: RewardScope, value: Long = 1) =
        RewardedOfferDefinition(GameId.of(id), id, id, type, value, 0, limit, cooldownMinutes * 60_000, scope)

    @Test fun `reward callback is committed exactly once`() {
        val prepared = engine.prepare(engine.initialState(0), capsule, 1_000, context) as RewardedAdCommandResult.Applied
        val request = prepared.transaction.requestId
        val rewarded = engine.markSdkRewarded(prepared.state, request, 2_000) as RewardedAdCommandResult.Applied
        val committed = engine.commit(rewarded.state, request, 3_000) as RewardedAdCommandResult.Applied
        assertEquals(10L, committed.state.entitlements.standardMaterialMinutes)
        assertEquals(1, committed.state.committedToday)
        assertTrue(engine.commit(committed.state, request, 4_000) is RewardedAdCommandResult.Rejected)
        assertEquals(10L, committed.state.entitlements.standardMaterialMinutes)
    }

    @Test fun `cancel and network failure consume no quota`() {
        val prepared = engine.prepare(engine.initialState(0), capsule, 1_000, context) as RewardedAdCommandResult.Applied
        val cancelled = engine.cancel(prepared.state, prepared.transaction.requestId, 2_000) as RewardedAdCommandResult.Applied
        assertEquals(0, cancelled.state.committedToday)
        assertTrue(cancelled.state.pendingRewards.isEmpty())
    }

    @Test fun `daily and global limits are enforced`() {
        var state = engine.initialState(0)
        var now = 1_000L
        fun commit(offerId: String, advanceMillis: Long) {
            now += advanceMillis
            val prepared = engine.prepare(state, GameId.of(offerId), now, context) as RewardedAdCommandResult.Applied
            val rewarded = engine.markSdkRewarded(prepared.state, prepared.transaction.requestId, now + 1) as RewardedAdCommandResult.Applied
            state = (engine.commit(rewarded.state, prepared.transaction.requestId, now + 2) as RewardedAdCommandResult.Applied).state
        }
        repeat(5) { commit("ad_time_relay", 10 * 60_000L + 10L) }
        repeat(3) { commit("ad_supply_capsule", 20 * 60_000L + 10L) }
        repeat(2) { commit("ad_premium_contract", 30 * 60_000L + 10L) }
        assertEquals(10, state.committedToday)
        val blocked = engine.prepare(state, GameId.of("ad_analysis_beacon"), now + 31 * 60_000L, context) as RewardedAdCommandResult.Rejected
        assertEquals("global_daily_limit", blocked.code)
    }

    @Test fun `tutorial narration and major animation block all offers`() {
        val state = engine.initialState(0)
        assertEquals("tutorial_active", (engine.prepare(state, capsule, 1_000, context.copy(tutorialActive = true)) as RewardedAdCommandResult.Rejected).code)
        assertEquals("narrative_active", (engine.prepare(state, capsule, 1_000, context.copy(narrativeActive = true)) as RewardedAdCommandResult.Rejected).code)
        assertEquals("major_animation_active", (engine.prepare(state, capsule, 1_000, context.copy(majorAnimationActive = true)) as RewardedAdCommandResult.Rejected).code)
    }

    @Test fun `sdk rewarded transaction is recovered after restart`() {
        val prepared = engine.prepare(engine.initialState(0), capsule, 1_000, context) as RewardedAdCommandResult.Applied
        val rewarded = engine.markSdkRewarded(prepared.state, prepared.transaction.requestId, 2_000) as RewardedAdCommandResult.Applied
        val recovered = engine.recoverRewarded(rewarded.state, 3_000)
        assertEquals(listOf(prepared.transaction.requestId), recovered.committedRequestIds)
        assertEquals(10L, recovered.state.entitlements.standardMaterialMinutes)
        assertTrue(recovered.state.pendingRewards.isEmpty())
    }

    @Test fun `boost cannot stack and never exceeds twenty five percent`() {
        val boost = GameId.of("ad_orbital_boost")
        val prepared = engine.prepare(engine.initialState(0), boost, 1_000, context) as RewardedAdCommandResult.Applied
        val rewarded = engine.markSdkRewarded(prepared.state, prepared.transaction.requestId, 2_000) as RewardedAdCommandResult.Applied
        val state = (engine.commit(rewarded.state, prepared.transaction.requestId, 3_000) as RewardedAdCommandResult.Applied).state
        assertEquals(25, state.entitlements.orbitalBoostPercent)
        assertEquals("boost_already_active", (engine.prepare(state, boost, 4_000, context) as RewardedAdCommandResult.Rejected).code)
    }
}
