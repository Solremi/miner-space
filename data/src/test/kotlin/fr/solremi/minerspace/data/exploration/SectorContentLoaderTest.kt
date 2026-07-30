package fr.solremi.minerspace.data.exploration

import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SectorContentLoaderTest {
    @Test
    fun `loads Ferrum Delta sectors with strategic reasons`() {
        val content = javaClass.getResource("/data/sectors.json")!!.readText()
        val definitions = SectorContentLoader().parse(content)

        assertEquals("0.6.0", definitions.contentVersion)
        assertEquals(6, definitions.sectors.size)
        assertEquals(3, definitions.sectors.values.count { it.rareDepositId != null })
        assertEquals(1_400L, definitions.sectors.getValue(GameId.of("sector_archive_ruins")).unlockCostSpaceDollars)
    }

    @Test
    fun `rejects decimal sector costs`() {
        val invalid = """{"schemaVersion":1,"contentVersion":"x","sectors":[{"id":"sector_a","nameKey":"a","strategicReason":"a","bounds":{"x":0,"y":0,"width":1,"height":1},"unlockCostSpaceDollars":1.5,"scannerLevelRequired":1,"initiallyUnlocked":true}]}"""
        assertThrows(IllegalArgumentException::class.java) { SectorContentLoader().parse(invalid) }
    }
}
