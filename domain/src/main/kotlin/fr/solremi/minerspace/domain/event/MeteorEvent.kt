package fr.solremi.minerspace.domain.event

import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.FixedPointMath
import fr.solremi.minerspace.shared.GameId
import kotlin.math.abs

private const val NORMALIZED_SCALE = 1_000_000L

enum class MeteorFragmentKind { STANDARD, RARE }
enum class MeteorEventPhase { ACTIVE, SUMMARY, COMMITTING, COMMITTED }

data class MeteorEventDefinition(
    val schemaVersion: Int,
    val contentVersion: String,
    val durationMillis: Long,
    val spawnIntervalMillis: Long,
    val maxActiveFragments: Int,
    val fragmentLifetimeMillis: Long,
    val rareSpawnAtMillis: Long,
    val standardResourceId: GameId,
    val rareResourceId: GameId,
    val standardRewardPerFragment: Long,
    val rareRewardQuantity: Long,
    val captureRadiusMillionths: Int,
    val assistedCaptureRadiusMillionths: Int,
    val assistAutoCollectIntervalMillis: Long,
) {
    init {
        require(schemaVersion > 0)
        require(contentVersion.isNotBlank())
        require(durationMillis in 45_000L..90_000L)
        require(spawnIntervalMillis in 250L..10_000L)
        require(maxActiveFragments in 1..64)
        require(fragmentLifetimeMillis in 1_500L..20_000L)
        require(rareSpawnAtMillis in 1L until durationMillis)
        require(standardRewardPerFragment > 0L)
        require(rareRewardQuantity > 0L)
        require(captureRadiusMillionths in 10_000..250_000)
        require(assistedCaptureRadiusMillionths >= captureRadiusMillionths)
        require(assistAutoCollectIntervalMillis in 500L..20_000L)
    }
}

data class MeteorFragment(
    val id: String,
    val kind: MeteorFragmentKind,
    val spawnXMillionths: Int,
    val spawnYMillionths: Int,
    val velocityXMillionthsPerSecond: Int,
    val velocityYMillionthsPerSecond: Int,
    val spawnedAtActiveMillis: Long,
) {
    init {
        require(id.isNotBlank())
        require(spawnXMillionths in -250_000..1_250_000)
        require(spawnYMillionths in -250_000..1_250_000)
        require(velocityXMillionthsPerSecond in -500_000..500_000)
        require(velocityYMillionthsPerSecond in -500_000..500_000)
        require(spawnedAtActiveMillis >= 0L)
    }
}

data class MeteorPoint(val xMillionths: Int, val yMillionths: Int)

data class MeteorEventState(
    val eventId: String,
    val seed: Long,
    val phase: MeteorEventPhase,
    val elapsedActiveMillis: Long,
    val nextSpawnIndex: Long,
    val rareSpawned: Boolean,
    val fragments: List<MeteorFragment>,
    val standardCollected: Long,
    val rareCollected: Long,
    val assistanceEnabled: Boolean,
    val lastAssistAtMillis: Long,
    val expectedStandardInventory: Long?,
    val expectedRareInventory: Long?,
    val codexEntryIds: Set<GameId>,
    val transactionSequence: Long,
) {
    init {
        require(eventId.isNotBlank())
        require(elapsedActiveMillis >= 0L)
        require(nextSpawnIndex >= 0L)
        require(standardCollected >= 0L)
        require(rareCollected >= 0L)
        require(lastAssistAtMillis >= 0L)
        require(transactionSequence >= 0L)
        require(fragments.map { it.id }.distinct().size == fragments.size)
        require(expectedStandardInventory == null || expectedStandardInventory >= 0L)
        require(expectedRareInventory == null || expectedRareInventory >= 0L)
        if (phase == MeteorEventPhase.COMMITTING || phase == MeteorEventPhase.COMMITTED) {
            require(expectedStandardInventory != null && expectedRareInventory != null)
        }
    }
}

data class MeteorCaptureResult(
    val state: MeteorEventState,
    val captured: MeteorFragmentKind?,
)

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

    private fun positiveMod(value: Long, modulus: Int): Int = (abs(value % modulus.toLong())).toInt()

    companion object {
        val CODEX_EVENT = GameId.of("codex_meteor_shower")
        val CODEX_STANDARD = GameId.of("codex_meteor_fragment")
        val CODEX_RARE = GameId.of("codex_meteor_core")
    }
}

data class MeteorRewardPreparation(
    val event: MeteorEventState,
    val state: ManufacturingGameState,
    val standardGranted: Long,
    val rareGranted: Long,
)

sealed interface MeteorRewardResult {
    val state: ManufacturingGameState
    val event: MeteorEventState

    data class Applied(
        override val state: ManufacturingGameState,
        override val event: MeteorEventState,
        val stateChanged: Boolean,
    ) : MeteorRewardResult

    data class Rejected(
        override val state: ManufacturingGameState,
        override val event: MeteorEventState,
        val code: String,
    ) : MeteorRewardResult
}

class MeteorRewardEngine(
    private val definition: MeteorEventDefinition,
    private val storageCapacities: Map<GameId, Long>,
) {
    fun prepare(state: ManufacturingGameState, event: MeteorEventState): MeteorRewardPreparation? {
        if (event.phase != MeteorEventPhase.SUMMARY) return null
        val standard = Math.multiplyExact(event.standardCollected, definition.standardRewardPerFragment)
        val rare = Math.multiplyExact(event.rareCollected, definition.rareRewardQuantity)
        val currentStandard = state.economy.inventory[definition.standardResourceId] ?: 0L
        val currentRare = state.economy.inventory[definition.rareResourceId] ?: 0L
        val expectedStandard = FixedPointMath.addExact(currentStandard, standard)
        val expectedRare = FixedPointMath.addExact(currentRare, rare)
        val standardCapacity = storageCapacities[definition.standardResourceId] ?: return null
        val rareCapacity = storageCapacities[definition.rareResourceId] ?: return null
        if (expectedStandard > standardCapacity || expectedRare > rareCapacity) return null
        val preparedEvent = event.copy(
            phase = MeteorEventPhase.COMMITTING,
            expectedStandardInventory = expectedStandard,
            expectedRareInventory = expectedRare,
            transactionSequence = Math.addExact(event.transactionSequence, 1L),
        )
        val inventory = state.economy.inventory.toMutableMap().apply {
            this[definition.standardResourceId] = expectedStandard
            this[definition.rareResourceId] = expectedRare
        }
        val preparedState = state.copy(
            economy = state.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(state.economy.transactionSequence, 1L),
            ),
        )
        return MeteorRewardPreparation(preparedEvent, preparedState, standard, rare)
    }

    fun reconcile(state: ManufacturingGameState, event: MeteorEventState): MeteorRewardResult {
        if (event.phase == MeteorEventPhase.COMMITTED) {
            return MeteorRewardResult.Applied(state, event, false)
        }
        if (event.phase != MeteorEventPhase.COMMITTING) {
            return MeteorRewardResult.Rejected(state, event, "meteor_reward_not_prepared")
        }
        val expectedStandard = event.expectedStandardInventory
            ?: return MeteorRewardResult.Rejected(state, event, "missing_expected_standard")
        val expectedRare = event.expectedRareInventory
            ?: return MeteorRewardResult.Rejected(state, event, "missing_expected_rare")
        val currentStandard = state.economy.inventory[definition.standardResourceId] ?: 0L
        val currentRare = state.economy.inventory[definition.rareResourceId] ?: 0L
        val standardCapacity = storageCapacities[definition.standardResourceId]
            ?: return MeteorRewardResult.Rejected(state, event, "unknown_standard_storage")
        val rareCapacity = storageCapacities[definition.rareResourceId]
            ?: return MeteorRewardResult.Rejected(state, event, "unknown_rare_storage")
        if (expectedStandard > standardCapacity || expectedRare > rareCapacity) {
            return MeteorRewardResult.Rejected(state, event, "meteor_reward_storage_full")
        }
        val changed = currentStandard < expectedStandard || currentRare < expectedRare
        val nextState = if (!changed) state else {
            val inventory = state.economy.inventory.toMutableMap().apply {
                this[definition.standardResourceId] = maxOf(currentStandard, expectedStandard)
                this[definition.rareResourceId] = maxOf(currentRare, expectedRare)
            }
            state.copy(economy = state.economy.copy(
                inventory = inventory,
                transactionSequence = FixedPointMath.addExact(state.economy.transactionSequence, 1L),
            ))
        }
        val completed = event.copy(
            phase = MeteorEventPhase.COMMITTED,
            transactionSequence = Math.addExact(event.transactionSequence, 1L),
        )
        return MeteorRewardResult.Applied(nextState, completed, changed)
    }
}
