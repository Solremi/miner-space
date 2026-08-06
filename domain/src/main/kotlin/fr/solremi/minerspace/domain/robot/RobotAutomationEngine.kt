package fr.solremi.minerspace.domain.robot

import fr.solremi.minerspace.domain.economy.FixedPointMath
import fr.solremi.minerspace.domain.economy.MULTIPLIER_SCALE
import fr.solremi.minerspace.shared.GameId

class RobotAutomationEngine(val definitions: RobotDefinitions) {
    fun initialState(nowEpochMillis: Long = 0L): RobotAutomationState {
        require(nowEpochMillis >= 0L)
        val robots = RobotFamily.entries.mapIndexed { index, family ->
            val definition = definitions.families.getValue(family)
            val id = GameId.of("robot_${family.name.lowercase()}_01")
            id to RobotInstance(
                id = id,
                family = family,
                displayName = definition.defaultName,
                serialNumber = "%s-%04d".format(definition.serialPrefix, index + 1),
                level = 1,
                trait = definition.defaultTrait,
                masteryPoints = 0L,
                priority = if (family == RobotFamily.LOGISTICS) AutomationPriority.STORAGE_RELIEF else AutomationPriority.BALANCED,
                statistics = RobotStatistics(),
            )
        }.toMap(linkedMapOf())
        return RobotAutomationState(
            robots = robots,
            lastLogisticsEpochMillis = nowEpochMillis,
            priorityCursor = 0,
            renderQuality = RenderQuality.MEDIUM,
            transactionSequence = 0L,
        )
    }

    fun normalize(source: RobotAutomationState, nowEpochMillis: Long): RobotAutomationState {
        require(nowEpochMillis >= 0L)
        val initial = initialState(nowEpochMillis)
        val normalized = initial.robots.mapValues { (id, fallback) ->
            val previous = source.robots[id] ?: return@mapValues fallback
            val definition = definitions.families.getValue(fallback.family)
            previous.copy(
                family = fallback.family,
                displayName = previous.displayName.ifBlank { fallback.displayName },
                serialNumber = previous.serialNumber.ifBlank { fallback.serialNumber },
                level = previous.level.coerceIn(1, definition.maxLevel),
                masteryPoints = previous.masteryPoints.coerceAtLeast(0L),
            )
        }
        return source.copy(
            robots = normalized,
            lastLogisticsEpochMillis = source.lastLogisticsEpochMillis.coerceIn(0L, nowEpochMillis),
            priorityCursor = source.priorityCursor.coerceAtLeast(0),
            transactionSequence = source.transactionSequence.coerceAtLeast(0L),
        )
    }

    fun queueCount(robot: RobotInstance): Int {
        val maxLevel = definitions.families.getValue(robot.family).maxLevel
        val level = robot.level.coerceIn(1, maxLevel)
        return 1 + (level - 1) / 2
    }

    fun visualTier(robot: RobotInstance): Int = 1 + (robot.level.coerceAtLeast(1) - 1) / 2

    fun masteryTier(robot: RobotInstance): MasteryTier = MasteryTier.entries
        .last { robot.masteryPoints >= definitions.masteryThresholds.getValue(it) }

    fun visibleUnitCount(state: RobotAutomationState): Int =
        definitions.visibleUnitsByQuality.getValue(state.renderQuality)

    fun upgradeCost(robot: RobotInstance): Long? {
        val definition = definitions.families.getValue(robot.family)
        if (robot.level >= definition.maxLevel) return null
        return definition.upgradeCostsSpaceDollars[robot.level]
    }

    fun upgrade(
        state: RobotAutomationState,
        robotId: GameId,
        availableSpaceDollars: Long,
    ): RobotCommandResult {
        require(availableSpaceDollars >= 0L)
        val robot = state.robots[robotId] ?: return RobotCommandResult.Rejected(state, "unknown_robot")
        val cost = upgradeCost(robot) ?: return RobotCommandResult.Rejected(state, "robot_max_level")
        if (availableSpaceDollars < cost) return RobotCommandResult.Rejected(state, "insufficient_space_dollars")
        val sequence = Math.addExact(state.transactionSequence, 1L)
        val upgraded = robot.copy(level = robot.level + 1)
        val next = state.copy(
            robots = state.robots + (robotId to upgraded),
            transactionSequence = sequence,
        )
        return RobotCommandResult.Applied(
            next,
            RobotTransaction(sequence, "upgrade_robot", robotId, cost),
        )
    }

    fun cyclePriority(state: RobotAutomationState, robotId: GameId): RobotCommandResult {
        val robot = state.robots[robotId] ?: return RobotCommandResult.Rejected(state, "unknown_robot")
        val values = AutomationPriority.entries
        val nextPriority = values[(robot.priority.ordinal + 1) % values.size]
        val sequence = Math.addExact(state.transactionSequence, 1L)
        return RobotCommandResult.Applied(
            state.copy(
                robots = state.robots + (robotId to robot.copy(priority = nextPriority)),
                transactionSequence = sequence,
            ),
            RobotTransaction(sequence, "change_robot_priority", robotId),
        )
    }

    fun cycleQuality(state: RobotAutomationState): RobotAutomationState {
        val values = RenderQuality.entries
        return state.copy(renderQuality = values[(state.renderQuality.ordinal + 1) % values.size])
    }

    fun recordWork(
        state: RobotAutomationState,
        robotId: GameId,
        quantity: Long,
        activeSeconds: Long,
    ): RobotAutomationState {
        require(quantity >= 0L)
        require(activeSeconds >= 0L)
        val robot = state.robots[robotId] ?: return state
        val stats = robot.statistics
        val updatedStats = when (robot.family) {
            RobotFamily.EXTRACTOR -> stats.copy(extracted = Math.addExact(stats.extracted, quantity))
            RobotFamily.REFINER -> stats.copy(refined = Math.addExact(stats.refined, quantity))
            RobotFamily.ASSEMBLER -> stats.copy(assembled = Math.addExact(stats.assembled, quantity))
            RobotFamily.LOGISTICS -> stats.copy(transported = Math.addExact(stats.transported, quantity))
        }.copy(activeSeconds = Math.addExact(stats.activeSeconds, activeSeconds))
        val gainedMastery = Math.addExact(quantity, activeSeconds / 60L)
        return state.copy(
            robots = state.robots + (robotId to robot.copy(
                masteryPoints = Math.addExact(robot.masteryPoints, gainedMastery),
                statistics = updatedStats,
            )),
        )
    }

    fun planQueues(robot: RobotInstance, tasks: List<QueueTask>): List<QueueAssignment> {
        val lanes = LongArray(queueCount(robot))
        return tasks.map { task ->
            val lane = lanes.indices.minBy { lanes[it] }
            val start = lanes[lane]
            val finish = Math.addExact(start, task.durationSeconds)
            lanes[lane] = finish
            QueueAssignment(task.id, lane, start, finish)
        }
    }

    fun advanceLogistics(
        state: RobotAutomationState,
        deposits: List<PendingDeposit>,
        inventory: Map<GameId, Long>,
        storageCapacities: Map<GameId, Long>,
        unitSalePrices: Map<GameId, Long>,
        nowEpochMillis: Long,
        maxCatchUpSeconds: Long = 8L * 60L * 60L,
    ): LogisticsResult {
        require(nowEpochMillis >= 0L)
        require(maxCatchUpSeconds >= 0L)
        val elapsed = ((nowEpochMillis - state.lastLogisticsEpochMillis).coerceAtLeast(0L) / 1_000L)
            .coerceAtMost(maxCatchUpSeconds)
        val pending = deposits.associate { it.depositId to it.pendingQuantity }.toMutableMap()
        val stored = inventory.toMutableMap()
        if (elapsed == 0L) {
            return LogisticsResult(state, pending, stored, emptyMap(), 0L)
        }
        val logistics = state.robots.values.firstOrNull { it.family == RobotFamily.LOGISTICS }
            ?: return LogisticsResult(state.copy(lastLogisticsEpochMillis = nowEpochMillis), pending, stored, emptyMap(), elapsed)
        val definition = definitions.families.getValue(RobotFamily.LOGISTICS)
        val baseCapacity = Math.multiplyExact(definition.baseLogisticsPerSecond, logistics.level.toLong())
        val theoretical = Math.multiplyExact(baseCapacity, elapsed)
        var remaining = FixedPointMath.floorMultiply(theoretical, performanceMultiplier(logistics))
        val moved = linkedMapOf<GameId, Long>()
        val ordered = orderDeposits(logistics.priority, deposits, unitSalePrices, state.priorityCursor)
        ordered.forEach { deposit ->
            if (remaining <= 0L) return@forEach
            val available = pending.getValue(deposit.depositId)
            if (available <= 0L) return@forEach
            val capacity = storageCapacities[deposit.resourceId] ?: return@forEach
            val current = stored[deposit.resourceId] ?: 0L
            val free = (capacity - current).coerceAtLeast(0L)
            val transfer = minOf(available, free, remaining)
            if (transfer <= 0L) return@forEach
            pending[deposit.depositId] = available - transfer
            stored[deposit.resourceId] = Math.addExact(current, transfer)
            moved[deposit.resourceId] = Math.addExact(moved[deposit.resourceId] ?: 0L, transfer)
            remaining -= transfer
        }
        val withWork = recordWork(state, logistics.id, moved.values.sum(), elapsed)
        val nextCursor = if (deposits.isEmpty()) 0 else (state.priorityCursor + 1) % deposits.size
        return LogisticsResult(
            automation = withWork.copy(
                lastLogisticsEpochMillis = nowEpochMillis,
                priorityCursor = nextCursor,
            ),
            pendingByDeposit = pending,
            inventory = stored,
            movedByResource = moved,
            elapsedSeconds = elapsed,
        )
    }

    private fun performanceMultiplier(robot: RobotInstance): Long {
        val traitBonus = when (robot.trait) {
            RobotTrait.PRECISE -> 100_000L
            RobotTrait.ENDURING -> 150_000L
            RobotTrait.FAST -> 200_000L
            RobotTrait.STABLE -> 80_000L
            RobotTrait.PROSPECTOR -> 50_000L
        }
        val masteryBonus = when (masteryTier(robot)) {
            MasteryTier.NOVICE -> 0L
            MasteryTier.EXPERIENCED -> 50_000L
            MasteryTier.EXPERT -> 100_000L
            MasteryTier.VETERAN -> 150_000L
        }
        return Math.addExact(MULTIPLIER_SCALE, Math.addExact(traitBonus, masteryBonus))
    }

    private fun orderDeposits(
        priority: AutomationPriority,
        deposits: List<PendingDeposit>,
        prices: Map<GameId, Long>,
        cursor: Int,
    ): List<PendingDeposit> = when (priority) {
        AutomationPriority.STORAGE_RELIEF -> deposits.sortedWith(
            compareByDescending<PendingDeposit> { it.pendingQuantity }.thenBy { it.depositId.value },
        )
        AutomationPriority.RARE_RESOURCE -> deposits.sortedWith(
            compareByDescending<PendingDeposit> { rareScore(it.resourceId) }
                .thenByDescending { it.pendingQuantity }
                .thenBy { it.depositId.value },
        )
        AutomationPriority.PROFIT -> deposits.sortedWith(
            compareByDescending<PendingDeposit> { prices[it.resourceId] ?: 0L }
                .thenByDescending { it.pendingQuantity }
                .thenBy { it.depositId.value },
        )
        AutomationPriority.MISSION -> deposits.sortedWith(
            compareBy<PendingDeposit> { missionRank(it.resourceId) }
                .thenByDescending { it.pendingQuantity }
                .thenBy { it.depositId.value },
        )
        AutomationPriority.BALANCED -> {
            val sorted = deposits.sortedBy { it.depositId.value }
            if (sorted.isEmpty()) sorted else {
                val shift = cursor % sorted.size
                sorted.drop(shift) + sorted.take(shift)
            }
        }
    }

    private fun rareScore(id: GameId): Int = when {
        "rare" in id.value || "xenon" in id.value -> 3
        "crystal" in id.value -> 2
        else -> 1
    }

    private fun missionRank(id: GameId): Int = when (id.value) {
        "raw_crystal" -> 0
        "raw_copper" -> 1
        "raw_iron" -> 2
        else -> 3
    }
}
