package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.domain.assembly.AssemblyJob
import fr.solremi.minerspace.domain.assembly.AssemblyJobStatus
import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ManufacturingPersistenceTest {
    @Test
    fun `assembly queue and installed technology survive snapshot round trip`() {
        val technology = GameId.of("tech_extraction_protocol")
        val state = ManufacturingGameState(
            economy = EconomyState(
                inventory = mapOf(GameId.of("component_power_cell") to 3L),
                deposits = emptyMap(),
                spaceDollars = 90L,
                transactionSequence = 12L,
            ),
            refining = RefiningState.empty(),
            assembly = AssemblyState(
                jobs = listOf(
                    AssemblyJob(
                        id = "as_job_4",
                        recipeId = GameId.of("assembly_sensor_array"),
                        queuedAtEpochMillis = 1_000L,
                        startsAtEpochMillis = 2_000L,
                        finishesAtEpochMillis = 26_000L,
                        reservedInputs = mapOf(GameId.of("raw_crystal") to 4L),
                        outputResourceId = GameId.of("component_sensor_array"),
                        outputQuantity = 1L,
                        outputKind = AssemblyOutputKind.COMPONENT,
                        status = AssemblyJobStatus.QUEUED,
                    ),
                ),
                installedTechnologyIds = setOf(technology),
                nextJobSequence = 5L,
            ),
        )
        val codec = ManufacturingSnapshotCodec()
        val payload = codec.encode(state, "0.4.0")
        val restored = codec.decode(payload)

        assertEquals(state, restored)
        assertEquals("as_job_4", restored.assembly.jobs.single().id)
        assertEquals(setOf(technology), restored.assembly.installedTechnologyIds)
    }
}
