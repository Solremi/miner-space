package fr.solremi.minerspace.domain.event

import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.FixedPointMath
import fr.solremi.minerspace.shared.GameId

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
            state.copy(
                economy = state.economy.copy(
                    inventory = inventory,
                    transactionSequence = FixedPointMath.addExact(state.economy.transactionSequence, 1L),
                ),
            )
        }
        val completed = event.copy(
            phase = MeteorEventPhase.COMMITTED,
            transactionSequence = Math.addExact(event.transactionSequence, 1L),
        )
        return MeteorRewardResult.Applied(nextState, completed, changed)
    }
}
