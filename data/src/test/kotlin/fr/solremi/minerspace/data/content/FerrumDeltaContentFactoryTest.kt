package fr.solremi.minerspace.data.content

import fr.solremi.minerspace.domain.content.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FerrumDeltaContentFactoryTest {
    private val content = FerrumDeltaContentFactory.create()

    @Test
    fun `factory reaches every published Ferrum budget`() {
        assertEquals(14, content.sectors.size)
        assertEquals(34, content.deposits.size)
        assertEquals(9, content.resources.count { it.category == ResourceCategory.RAW })
        assertEquals(9, content.resources.count { it.category == ResourceCategory.REFINED })
        assertEquals(24, content.resources.count { it.category == ResourceCategory.COMPONENT })
        assertEquals(14, content.technologies.size)
        assertEquals(24, content.modules.size)
        assertEquals(42, content.missions.count { it.group == MissionGroup.MAIN })
        assertEquals(36, content.missions.count { it.group == MissionGroup.SECONDARY })
        assertEquals(20, content.missions.count { it.group == MissionGroup.MASTERY_COLLECTION })
        assertEquals(8, content.achievements.size)
        assertEquals(12, content.contracts.size)
        assertEquals(12, content.events.size)
        assertEquals(120, content.codexEntries.size)
        assertEquals(10, content.collections.size)
        assertEquals(5, content.narrativeMilestones.size)
        assertEquals(12, content.transmissions.size)
    }

    @Test
    fun `mandatory resources are guaranteed and events stay optional`() {
        val errors = FerrumDeltaContentValidator().validationErrors(content)
        assertTrue(errors.isEmpty(), errors.joinToString())
        assertTrue(content.resources.filter { it.mandatory }.all { it.guaranteedSourceId.isNotBlank() })
        assertTrue(content.events.none { it.mandatory })
    }

    @Test
    fun `vertical slice identifiers remain stable`() {
        val resources = content.resources.mapTo(linkedSetOf()) { it.id }
        assertTrue(setOf(
            "raw_iron", "raw_copper", "raw_crystal", "refined_iron_ingot", "refined_copper_plate",
            "component_power_cell", "component_sensor_array", "rare_prismatic_ferrite",
            "rare_xenon_crystal", "rare_archive_fragment",
        ).all(resources::contains))
        assertTrue(content.sectors.map { it.id }.containsAll(setOf(
            "sector_core_delta", "sector_copper_ridge", "sector_crystal_flats",
            "sector_logistics_pass", "sector_xenon_depths", "sector_archive_ruins",
        )))
        assertTrue(content.technologies.map { it.id }.containsAll(setOf(
            "tech_extraction_protocol", "tech_quantum_sorting",
        )))
    }
}
