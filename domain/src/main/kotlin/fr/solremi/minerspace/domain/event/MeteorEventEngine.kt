package fr.solremi.minerspace.domain.event

import fr.solremi.minerspace.shared.GameId
import kotlin.math.abs

class MeteorEventEngine(
    val definition: MeteorEventDefinition,
) {
    fun start(eventId: String, seed: Long, assistanceEnabled: Boolean = true): MeteorEventState =
        MeteorEventState(
            eventId = eventId,
            seed = seed,
            phase = MeteorEventPhase.ACTIVE,
            elapsedActiveMillis = 0L,
            nextSpawnIndex = 0L,
            rareSpawned = false,
            fragments = emptyList(),
            standardCollected = 0L,
            rareCollected = 0L,
            assistanceEnabled = assistanceEnabled,
            lastAssistAtMillis = 0L,
            expectedStandardInventory = null,
            expectedRareInventory = null,
            codexEntryIds = setOf(CODEX_EVENT),
            transactionSequence = 0L,
        )

    fun advance(source: MeteorEventState, deltaActiveMillis: Long): MeteorEventState {
        require(deltaActiveMillis >= 0L)
        if (source.phase != MeteorEventPhase.ACTIVE || deltaActiveMillis == 0L) return source
        val elapsed = Math.addExact(source.elapsedActiveMillis, deltaActiveMillis)
            .coerceAtMost(definition.durationMillis)
        var state = source.copy(elapsedActiveMillis = elapsed)
        state = pruneExpired(state)

        var nextIndex = state.nextSpawnIndex
        var fragments = state.fragments
        while (Math.multiplyExact(nextIndex, definition.spawnIntervalMillis) <= elapsed) {
            val scheduledAt = Math.multiplyExact(nextIndex, definition.spawnIntervalMillis)
            if (scheduledAt > 0L && scheduledAt < definition.durationMillis) {
                val reservedSlots = if (!state.rareSpawned && elapsed < definition.rareSpawnAtMillis) 1 else 0
                if (activeCount(fragments, elapsed) < definition.maxActiveFragments - reservedSlots) {
                    fragments = fragments + standardFragment(state.seed, nextIndex, scheduledAt)
                }
            }
            nextIndex = Math.addExact(nextIndex, 1L)
        }
        state = state.copy(nextSpawnIndex = nextIndex, fragments = fragments)

        if (!state.rareSpawned && elapsed >= definition.rareSpawnAtMillis) {
            var current = state.fragments
            if (activeCount(current, elapsed) >= definition.maxActiveFragments) {
                val oldestStandard = current
                    .filter { it.kind == MeteorFragmentKind.STANDARD }
                    .minByOrNull { it.spawnedAtActiveMillis }
                if (oldestStandard != null) current = current.filterNot { it.id == oldestStandard.id }
            }
            state = state.copy(
                rareSpawned = true,
                fragments = current + rareFragment(state.eventId),
            )
        }

        state = applyAssistance(state)
        state = pruneExpired(state)
        if (elapsed >= definition.durationMillis) {
            state = state.copy(
                phase = MeteorEventPhase.SUMMARY,
                fragments = emptyList(),
                transactionSequence = Math.addExact(state.transactionSequence, 1L),
            )
        }
        return state
    }

    fun toggleAssistance(state: MeteorEventState): MeteorEventState = state.copy(
        assistanceEnabled = !state.assistanceEnabled,
        lastAssistAtMillis = state.elapsedActiveMillis,
        transactionSequence = Math.addExact(state.transactionSequence, 1L),
    )

    fun capture(
        state: MeteorEventState,
        xMillionths: Int,
        yMillionths: Int,
    ): MeteorCaptureResult {
        if (state.phase != MeteorEventPhase.ACTIVE) return MeteorCaptureResult(state, null)
        val radius = if (state.assistanceEnabled) {
            definition.assistedCaptureRadiusMillionths
        } else {
            definition.captureRadiusMillionths
        }
        val radiusSquared = radius.toLong() * radius.toLong()
        val candidate = state.fragments
            .asSequence()
            .map { fragment -> fragment to position(fragment, state.elapsedActiveMillis) }
            .filter { (_, point) -> point.yMillionths in -50_000..1_050_000 }
            .map { (fragment, point) ->
                val dx = point.xMillionths.toLong() - xMillionths
                val dy = point.yMillionths.toLong() - yMillionths
                fragment to (dx * dx + dy * dy)
            }
            .filter { it.second <= radiusSquared }
            .minByOrNull { it.second }
            ?.first
            ?: return MeteorCaptureResult(state, null)
        return MeteorCaptureResult(collect(state, candidate), candidate.kind)
    }

    fun position(fragment: MeteorFragment, elapsedActiveMillis: Long): MeteorPoint {
        val age = (elapsedActiveMillis - fragment.spawnedAtActiveMillis).coerceAtLeast(0L)
        val x = fragment.spawnXMillionths.toLong() +
            fragment.velocityXMillionthsPerSecond.toLong() * age / 1_000L
        val y = fragment.spawnYMillionths.toLong() +
            fragment.velocityYMillionthsPerSecond.toLong() * age / 1_000L
        return MeteorPoint(
            x.coerceIn(-100_000L, 1_100_000L).toInt(),
            y.coerceIn(-100_000L, 1_100_000L).toInt(),
        )
    }

    private fun applyAssistance(source: MeteorEventState): MeteorEventState {
        if (!source.assistanceEnabled) return source
        var state = source
        var nextAssist = Math.addExact(state.lastAssistAtMillis, definition.assistAutoCollectIntervalMillis)
        while (nextAssist <= state.elapsedActiveMillis) {
            val candidate = state.fragments
                .filter { fragment ->
                    val age = state.elapsedActiveMillis - fragment.spawnedAtActiveMillis
                    age >= definition.fragmentLifetimeMillis * 55L / 100L
                }
                .minWithOrNull(
                    compareByDescending<MeteorFragment> { it.kind == MeteorFragmentKind.RARE }
                        .thenBy { it.spawnedAtActiveMillis },
                )
            if (candidate != null) state = collect(state, candidate)
            state = state.copy(lastAssistAtMillis = nextAssist)
            nextAssist = Math.addExact(nextAssist, definition.assistAutoCollectIntervalMillis)
        }
        return state
    }

    private fun collect(source: MeteorEventState, fragment: MeteorFragment): MeteorEventState {
        val standard = source.standardCollected + if (fragment.kind == MeteorFragmentKind.STANDARD) 1L else 0L
        val rare = source.rareCollected + if (fragment.kind == MeteorFragmentKind.RARE) 1L else 0L
        val codex = source.codexEntryIds + when (fragment.kind) {
            MeteorFragmentKind.STANDARD -> CODEX_STANDARD
            MeteorFragmentKind.RARE -> CODEX_RARE
        }
        return source.copy(
            fragments = source.fragments.filterNot { it.id == fragment.id },
            standardCollected = standard,
            rareCollected = rare,
            codexEntryIds = codex,
            transactionSequence = Math.addExact(source.transactionSequence, 1L),
        )
    }

    private fun pruneExpired(source: MeteorEventState): MeteorEventState {
        val fragments = source.fragments.filter { fragment ->
            source.elapsedActiveMillis - fragment.spawnedAtActiveMillis <= definition.fragmentLifetimeMillis
        }
        return if (fragments == source.fragments) source else source.copy(fragments = fragments)
    }

    private fun activeCount(fragments: List<MeteorFragment>, elapsed: Long): Int = fragments.count { fragment ->
        elapsed - fragment.spawnedAtActiveMillis in 0L..definition.fragmentLifetimeMillis
    }

    private fun standardFragment(seed: Long, index: Long, scheduledAt: Long): MeteorFragment {
        val randomA = mix(seed xor index)
        val randomB = mix(randomA xor 0x6A09E667F3BCC909L)
        val x = 90_000 + positiveMod(randomA, 820_001)
        val vx = positiveMod(randomB, 100_001) - 50_000
        val speed = 135_000 + positiveMod(randomA ushr 11, 75_001)
        return MeteorFragment(
            id = "standard_$index",
            kind = MeteorFragmentKind.STANDARD,
            spawnXMillionths = x,
            spawnYMillionths = 1_040_000,
            velocityXMillionthsPerSecond = vx,
            velocityYMillionthsPerSecond = -speed,
            spawnedAtActiveMillis = scheduledAt,
        )
    }

    private fun rareFragment(eventId: String): MeteorFragment = MeteorFragment(
        id = "rare_$eventId",
        kind = MeteorFragmentKind.RARE,
        spawnXMillionths = 500_000,
        spawnYMillionths = 1_040_000,
        velocityXMillionthsPerSecond = 12_000,
        velocityYMillionthsPerSecond = -105_000,
        spawnedAtActiveMillis = definition.rareSpawnAtMillis,
    )

    private fun mix(value: Long): Long {
        var x = value + 0x9E3779B97F4A7C15UL.toLong()
        x = (x xor (x ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        x = (x xor (x ushr 27)) * 0x94D049BB133111EBUL.toLong()
        return x xor (x ushr 31)
    }

    private fun positiveMod(value: Long, modulus: Int): Int =
        abs(value % modulus.toLong()).toInt()

    companion object {
        val CODEX_EVENT = GameId.of("codex_meteor_shower")
        val CODEX_STANDARD = GameId.of("codex_meteor_fragment")
        val CODEX_RARE = GameId.of("codex_meteor_core")
    }
}
