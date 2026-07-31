package fr.solremi.minerspace.domain.strategy

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StrategyEngineTest {
    private val iron = GameId.of("refined_iron_ingot")
    private val power = GameId.of("component_power_cell")
    private val robot = GameId.of("robot_refiner_01")
    private val moduleId = GameId.of("module_forge_drill")
    private val definitions = StrategyDefinitions(
        1,
        "test",
        SpecializationId.entries.associateWith { id ->
            val bonuses = when (id) {
                SpecializationId.INDUSTRIAL -> StrategyBonuses(extraction = 250_000, refiningSpeed = 100_000, assemblySpeed = -80_000)
                SpecializationId.LOGISTICS -> StrategyBonuses(extraction = -50_000, logistics = 250_000, storage = 250_000)
                SpecializationId.RESEARCH -> StrategyBonuses(extraction = -100_000, refiningSpeed = 80_000, assemblySpeed = 250_000)
                SpecializationId.PROSPECTOR -> StrategyBonuses(extraction = 120_000, assemblySpeed = -100_000, logistics = 80_000, rareFind = 300_000)
            }
            SpecializationDefinition(id, "s.$id", bonuses, 900, 21_600)
        },
        buildMap {
            put(moduleId, ModuleDefinition(moduleId, "m.drill", ModuleSetId.FORGE, StrategyBonuses(extraction = 90_000), mapOf(iron to 8, power to 1), 180, listOf(0, 260, 520), 3))
            repeat(5) { index ->
                val id = GameId.of("module_extra_$index")
                put(id, ModuleDefinition(id, "m.$index", if (index < 2) ModuleSetId.FORGE else ModuleSetId.SURVEY, StrategyBonuses(logistics = 10_000), mapOf(iron to 1), 1, listOf(0), 1))
            }
        },
        listOf(
            SynergyDefinition(ModuleSetId.FORGE, 2, StrategyBonuses(extraction = 80_000)),
            SynergyDefinition(ModuleSetId.SURVEY, 2, StrategyBonuses(rareFind = 100_000)),
        ),
    )
    private val engine = StrategyEngine(definitions)
    private val access = StrategyAccess(100_000_000, 10_000, mapOf(iron to 100, power to 10), mapOf(robot to 5))

    @Test
    fun `specializations keep meaningful tradeoffs`() {
        SpecializationId.entries.forEach { id ->
            assertTrue(engine.dominantCategoryCount(id) <= 2, "$id dominates too many visible categories")
            val b = definitions.specializations.getValue(id).bonuses
            assertTrue(listOf(b.extraction, b.refiningSpeed, b.assemblySpeed, b.logistics, b.storage, b.rareFind).any { it <= 0 })
        }
    }

    @Test
    fun `trial craft equip upgrade and dismantle are deterministic`() {
        var state = StrategyState.empty()
        val chosen = engine.chooseSpecialization(state, SpecializationId.INDUSTRIAL, access) as StrategyCommandResult.Applied
        assertEquals(0, chosen.transaction.spaceDollarDelta)
        state = chosen.state
        val crafted = engine.craft(state, moduleId, access) as StrategyCommandResult.Applied
        assertEquals(-8, crafted.transaction.inventoryDeltas.getValue(iron))
        state = crafted.state
        val instance = state.modules.values.single()
        state = (engine.equip(state, instance.instanceId, robot, access) as StrategyCommandResult.Applied).state
        assertEquals(robot, state.modules.getValue(instance.instanceId).equippedRobotId)
        state = (engine.upgrade(state, instance.instanceId, access) as StrategyCommandResult.Applied).state
        assertEquals(2, state.modules.getValue(instance.instanceId).level)
        val dismantled = engine.dismantle(state, instance.instanceId) as StrategyCommandResult.Applied
        assertEquals(5, dismantled.transaction.inventoryDeltas.getValue(iron))
        assertTrue(dismantled.state.modules.isEmpty())
    }

    @Test
    fun `slots progress to three and prevent overflow`() {
        assertEquals(1, engine.slotCount(1))
        assertEquals(2, engine.slotCount(3))
        assertEquals(3, engine.slotCount(5))
    }
}
