package fr.solremi.minerspace.data.save

import fr.solremi.minerspace.data.FileSaveService
import fr.solremi.minerspace.domain.economy.DepositState
import fr.solremi.minerspace.domain.economy.EconomyState
import fr.solremi.minerspace.domain.refining.RefiningGameState
import fr.solremi.minerspace.domain.refining.RefiningJob
import fr.solremi.minerspace.domain.refining.RefiningJobStatus
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class GameStatePersistenceTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `active refining job survives a complete file round trip`() {
        val rawIron = GameId.of("raw_iron")
        val state = RefiningGameState(
            economy = EconomyState(
                inventory = mapOf(rawIron to 18L),
                deposits = mapOf(GameId.of("deposit_iron") to DepositState(90L, 10L)),
                spaceDollars = 42L,
                transactionSequence = 7L,
            ),
            refining = RefiningState(
                jobs = listOf(
                    RefiningJob(
                        id = "rf_job_3",
                        recipeId = GameId.of("recipe_iron_ingot"),
                        queuedAtEpochMillis = 1_000L,
                        startsAtEpochMillis = 1_000L,
                        finishesAtEpochMillis = 13_000L,
                        reservedInputs = mapOf(rawIron to 12L),
                        outputResourceId = GameId.of("refined_iron_ingot"),
                        outputQuantity = 4L,
                        status = RefiningJobStatus.RUNNING,
                    ),
                ),
                refundBuffer = emptyMap(),
                nextJobSequence = 4L,
            ),
        )
        val codec = GameStateSnapshotCodec()
        val service = FileSaveService(temporaryDirectory)
        assertEquals(
            SaveWriteStatus.WRITTEN,
            service.save(codec.encode(state, "0.3.0")),
        )

        val payload = service.loadLatest()
        assertNotNull(payload)
        val restored = codec.decode(payload!!)
        assertEquals(state, restored)
        assertEquals("rf_job_3", restored.refining.jobs.single().id)
        assertEquals(13_000L, restored.refining.jobs.single().finishesAtEpochMillis)
    }
}
