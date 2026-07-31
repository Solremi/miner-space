package fr.solremi.minerspace.domain.frontier

import fr.solremi.minerspace.shared.GameId

class FrontierEngine(
    private val definitions: FrontierDefinitions,
    private val generator: FrontierWorldGenerator = FrontierWorldGenerator(definitions),
) {
    fun initialState(seed: Long): FrontierState = FrontierState(
        seed = seed,
        nextGenerationIndex = 0,
        worlds = emptyMap(),
        activeWorldId = null,
        lastGeneratedSignature = null,
        permanentBonusIds = emptySet(),
        cosmeticIds = emptySet(),
        collectionIds = emptySet(),
        completedWorldCount = 0,
        transactionSequence = 0L,
    )

    fun normalize(source: FrontierState): FrontierState {
        val validWorlds = source.worlds.filterValues { generator.validationErrors(it.definition).isEmpty() }
        val active = source.activeWorldId?.takeIf(validWorlds::containsKey)
        return source.copy(
            nextGenerationIndex = maxOf(source.nextGenerationIndex, validWorlds.values.maxOfOrNull { it.definition.generationIndex + 1 } ?: 0),
            worlds = validWorlds,
            activeWorldId = active,
            completedWorldCount = validWorlds.values.count { it.status == FrontierWorldStatus.COMPLETED },
            transactionSequence = source.transactionSequence.coerceAtLeast(0L),
        )
    }

    fun discoverWorld(state: FrontierState, difficulty: FrontierDifficulty, nowEpochMillis: Long): FrontierCommandResult {
        require(nowEpochMillis >= 0L)
        if (state.worlds.values.count { it.status == FrontierWorldStatus.ACTIVE } >= 3) {
            return FrontierCommandResult.Rejected(state, "active_world_limit")
        }
        val definition = generator.generate(state.seed, state.nextGenerationIndex, difficulty, state.lastGeneratedSignature)
        val progress = FrontierWorldProgress(
            definition = definition,
            progress = 0L,
            actionCount = 0,
            status = FrontierWorldStatus.ACTIVE,
            startedAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
            completedAtEpochMillis = null,
        )
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            nextGenerationIndex = state.nextGenerationIndex + 1,
            worlds = state.worlds + (definition.id to progress),
            activeWorldId = definition.id,
            lastGeneratedSignature = definition.signature,
            transactionSequence = sequence,
        )
        return FrontierCommandResult.Applied(next, FrontierTransaction(sequence, "discover_frontier_world", definition.id))
    }

    fun selectWorld(state: FrontierState, worldId: GameId): FrontierCommandResult {
        if (worldId !in state.worlds) return FrontierCommandResult.Rejected(state, "unknown_world")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return FrontierCommandResult.Applied(
            state.copy(activeWorldId = worldId, transactionSequence = sequence),
            FrontierTransaction(sequence, "select_frontier_world", worldId),
        )
    }

    fun performAction(state: FrontierState, nowEpochMillis: Long): FrontierCommandResult {
        require(nowEpochMillis >= 0L)
        val worldId = state.activeWorldId ?: return FrontierCommandResult.Rejected(state, "no_active_world")
        val world = state.worlds[worldId] ?: return FrontierCommandResult.Rejected(state, "unknown_world")
        if (world.status == FrontierWorldStatus.COMPLETED) return FrontierCommandResult.Rejected(state, "world_already_completed")
        val base = when (world.definition.difficulty) {
            FrontierDifficulty.SCOUT -> 18L
            FrontierDifficulty.EXPEDITION -> 14L
            FrontierDifficulty.DEEP -> 11L
        }
        val sectorBonus = world.definition.sectors.size.toLong()
        val gain = base + sectorBonus
        val progress = Math.addExact(world.progress, gain).coerceAtMost(world.definition.targetProgress)
        val completed = progress >= world.definition.targetProgress
        val updated = world.copy(
            progress = progress,
            actionCount = world.actionCount + 1,
            status = if (completed) FrontierWorldStatus.COMPLETED else FrontierWorldStatus.ACTIVE,
            updatedAtEpochMillis = nowEpochMillis,
            completedAtEpochMillis = if (completed) nowEpochMillis else null,
        )
        val rewardId = if (completed) rewardId(world.definition) else null
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val next = state.copy(
            worlds = state.worlds + (worldId to updated),
            permanentBonusIds = if (completed && world.definition.rewardKind == FrontierRewardKind.PERMANENT_BONUS) state.permanentBonusIds + rewardId!! else state.permanentBonusIds,
            cosmeticIds = if (completed && world.definition.rewardKind == FrontierRewardKind.COSMETIC) state.cosmeticIds + rewardId!! else state.cosmeticIds,
            collectionIds = if (completed && world.definition.rewardKind == FrontierRewardKind.COLLECTION) state.collectionIds + rewardId!! else state.collectionIds,
            completedWorldCount = state.completedWorldCount + if (completed) 1 else 0,
            transactionSequence = sequence,
        )
        return FrontierCommandResult.Applied(
            next,
            FrontierTransaction(sequence, if (completed) "complete_frontier_world" else "advance_frontier_world", worldId, rewardId),
        )
    }

    fun validationErrors(state: FrontierState): List<String> {
        val errors = mutableListOf<String>()
        state.worlds.forEach { (id, world) ->
            if (id != world.definition.id) errors += "world_key_mismatch:$id"
            errors += generator.validationErrors(world.definition).map { "$id:$it" }
            if (world.progress > world.definition.targetProgress) errors += "progress_overflow:$id"
        }
        if (state.worlds.values.count { it.status == FrontierWorldStatus.ACTIVE } > 3) errors += "too_many_active_worlds"
        return errors
    }

    private fun rewardId(world: FrontierWorldDefinition): GameId = when (world.rewardKind) {
        FrontierRewardKind.PERMANENT_BONUS -> GameId.of("bonus_frontier_${world.generationIndex}")
        FrontierRewardKind.COSMETIC -> GameId.of("cosmetic_frontier_${world.generationIndex}")
        FrontierRewardKind.COLLECTION -> GameId.of("collection_frontier_${world.generationIndex}")
    }
}
