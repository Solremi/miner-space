package fr.solremi.minerspace.data.strategy

import fr.solremi.minerspace.data.save.StrategyStateCodec
import fr.solremi.minerspace.domain.strategy.SpecializationId
import fr.solremi.minerspace.domain.strategy.StrategyState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class StrategyContentAndCodecTest {
    @Test
    fun `loads versioned strategy content`() {
        val text = Files.readString(java.nio.file.Path.of("../assets/data/specializations-modules.json"))
        val definitions = StrategyContentLoader().parse(text)
        assertEquals("0.9.0", definitions.contentVersion)
        assertEquals(4, definitions.specializations.size)
        assertEquals(8, definitions.modules.size)
        assertEquals(2, definitions.synergies.map { it.setId }.distinct().size)
    }

    @Test
    fun `strategy state round trips`() {
        val codec = StrategyStateCodec()
        val state = StrategyState.empty().copy(activeSpecialization = SpecializationId.LOGISTICS, trialUsed = true, transactionSequence = 2)
        val payload = codec.encode(state, "0.9.0", 1000)
        assertEquals(state, codec.decode(payload))
    }
}
