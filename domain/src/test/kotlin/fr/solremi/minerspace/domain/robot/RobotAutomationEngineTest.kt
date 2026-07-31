package fr.solremi.minerspace.domain.robot

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RobotAutomationEngineTest {
    private val definitions = definitions()
    private val engine = RobotAutomationEngine(definitions)

    @Test
    fun `important robots have unique identities and high quality shows fifty units`() {
        val state = engine.initialState(1_000L)
        assertEquals(RobotFamily.entries.toSet(), state.robots.values.map { it.family }.toSet())
        assertEquals(4, state.robots.values.map { it.serialNumber }.distinct().size)
        assertTrue(state.robots.values.all { it.displayName.isNotBlank() })
        assertEquals(50, engine.visibleUnitCount(state.copy(renderQuality = RenderQuality.HIGH)))
    }

    @Test
    fun `levels unlock real parallel queue lanes and visual tiers`() {
        var state = engine.initialState()
        val logisticsId = state.robots.values.first { it.family == RobotFamily.LOGISTICS }.id
        repeat(4) {
            state = (engine.upgrade(state, logisticsId, 100_000L) as RobotCommandResult.Applied).state
        }
        val robot = state.robots.getValue(logisticsId)
        assertEquals(5, robot.level)
        assertEquals(3, engine.queueCount(robot))
        assertEquals(3, engine.visualTier(robot))
        val plan = engine.planQueues(robot, (1..6).map { QueueTask("task_$it", 10L) })
        assertEquals(setOf(0, 1, 2), plan.map { it.laneIndex }.toSet())
        assertEquals(20L, plan.maxOf { it.finishesAtSecond })
    }

    @Test
    fun `logistics conserves resources and respects storage limits`() {
        var state = engine.initialState(1_000L)
        val logisticsId = state.robots.values.first { it.family == RobotFamily.LOGISTICS }.id
        repeat(4) {
            state = (engine.upgrade(state, logisticsId, 100_000L) as RobotCommandResult.Applied).state
        }
        val iron = GameId.of("raw_iron")
        val crystal = GameId.of("raw_crystal")
        val deposits = listOf(
            PendingDeposit(GameId.of("deposit_iron"), iron, 1_000L),
            PendingDeposit(GameId.of("deposit_crystal"), crystal, 500L),
        )
        val result = engine.advanceLogistics(
            state = state,
            deposits = deposits,
            inventory = mapOf(iron to 0L, crystal to 0L),
            storageCapacities = mapOf(iron to 100L, crystal to 80L),
            unitSalePrices = mapOf(iron to 2L, crystal to 6L),
            nowEpochMillis = 11_000L,
        )
        assertEquals(180L, result.totalMoved)
        assertEquals(1_500L, result.pendingByDeposit.values.sum() + result.totalMoved)
        assertEquals(100L, result.inventory.getValue(iron))
        assertEquals(80L, result.inventory.getValue(crystal))
        assertEquals(180L, result.automation.robots.getValue(logisticsId).statistics.transported)
    }

    @Test
    fun `all traits remain beneficial and mastery progresses`() {
        val base = engine.initialState(0L)
        val logistics = base.robots.values.first { it.family == RobotFamily.LOGISTICS }
        RobotTrait.entries.forEach { trait ->
            val state = base.copy(robots = base.robots + (logistics.id to logistics.copy(trait = trait)))
            val result = engine.advanceLogistics(
                state,
                listOf(PendingDeposit(GameId.of("deposit"), GameId.of("raw_iron"), 10_000L)),
                mapOf(GameId.of("raw_iron") to 0L),
                mapOf(GameId.of("raw_iron") to 10_000L),
                mapOf(GameId.of("raw_iron") to 2L),
                10_000L,
            )
            assertTrue(result.totalMoved >= 40L, "$trait must not reduce baseline capacity")
        }
        val worked = engine.recordWork(base, logistics.id, 2_000L, 3_600L)
        assertTrue(engine.masteryTier(worked.robots.getValue(logistics.id)).ordinal >= MasteryTier.EXPERT.ordinal)
    }

    private fun definitions(): RobotDefinitions {
        val costs = listOf(0L, 100L, 300L, 900L, 2_400L)
        return RobotDefinitions(
            schemaVersion = 1,
            contentVersion = "0.8.0",
            families = RobotFamily.entries.associateWith { family ->
                RobotFamilyDefinition(
                    family = family,
                    nameKey = family.name,
                    defaultName = family.name.lowercase().replaceFirstChar(Char::uppercase),
                    serialPrefix = "FD-${family.name.take(2)}",
                    defaultTrait = RobotTrait.STABLE,
                    maxLevel = 5,
                    baseLogisticsPerSecond = if (family == RobotFamily.LOGISTICS) 4L else 0L,
                    upgradeCostsSpaceDollars = costs,
                )
            },
            masteryThresholds = mapOf(
                MasteryTier.NOVICE to 0L,
                MasteryTier.EXPERIENCED to 250L,
                MasteryTier.EXPERT to 1_500L,
                MasteryTier.VETERAN to 6_000L,
            ),
            visibleUnitsByQuality = mapOf(
                RenderQuality.LOW to 18,
                RenderQuality.MEDIUM to 32,
                RenderQuality.HIGH to 50,
            ),
        )
    }
}
