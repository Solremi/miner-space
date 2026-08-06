package fr.solremi.minerspace.data.manufacturing

import fr.solremi.minerspace.data.save.ManufacturingSnapshotCodec
import fr.solremi.minerspace.domain.assembly.AssemblerRobotDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyDefinitions
import fr.solremi.minerspace.domain.assembly.AssemblyOutputKind
import fr.solremi.minerspace.domain.assembly.AssemblyRecipeDefinition
import fr.solremi.minerspace.domain.assembly.AssemblyState
import fr.solremi.minerspace.domain.assembly.ManufacturingGameState
import fr.solremi.minerspace.domain.assembly.TechnologyDefinition
import fr.solremi.minerspace.domain.economy.CoreEconomyEngine
import fr.solremi.minerspace.domain.economy.DepositDefinition
import fr.solremi.minerspace.domain.economy.EconomyDefinitions
import fr.solremi.minerspace.domain.economy.ResourceDefinition
import fr.solremi.minerspace.domain.refining.RecipeDefinition
import fr.solremi.minerspace.domain.refining.RefinerRobotDefinition
import fr.solremi.minerspace.domain.refining.RefiningDefinitions
import fr.solremi.minerspace.domain.refining.RefiningState
import fr.solremi.minerspace.domain.services.ClockService
import fr.solremi.minerspace.domain.services.SavePayload
import fr.solremi.minerspace.domain.services.SaveService
import fr.solremi.minerspace.domain.services.SaveWriteStatus
import fr.solremi.minerspace.shared.GameId
import fr.solremi.minerspace.shared.SilentGameLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManufacturingCoordinatorTest {
    private val raw = GameId.of("raw_iron")
    private val refined = GameId.of("refined_iron")
    private val technologyItem = GameId.of("technology_item")
    private val deposit = GameId.of("deposit_iron")
    private val refiningRecipe = GameId.of("recipe_iron")
    private val assemblyRecipe = GameId.of("assembly_technology")
    private val technology = GameId.of("technology_extraction")

    private val economyDefinitions = EconomyDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        resources = listOf(
            ResourceDefinition(raw, "resource.raw", 2L, 1_000L, true),
            ResourceDefinition(refined, "resource.refined", 4L, 1_000L, true),
            ResourceDefinition(technologyItem, "resource.technology", 0L, 20L, false),
        ).associateBy { it.id },
        deposits = mapOf(
            deposit to DepositDefinition(
                id = deposit,
                resourceId = raw,
                initialReserve = 1_000L,
                extractionPerSecond = 1L,
                transportCapacity = 100L,
            ),
        ),
    )

    private val refiningDefinitions = RefiningDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        robot = RefinerRobotDefinition(GameId.of("robot_refiner"), "robot.refiner", 2),
        recipes = mapOf(
            refiningRecipe to RecipeDefinition(
                id = refiningRecipe,
                nameKey = "recipe.iron",
                inputs = mapOf(raw to 2L),
                outputResourceId = refined,
                outputQuantity = 1L,
                durationSeconds = 5L,
            ),
        ),
    )

    private val assemblyDefinitions = AssemblyDefinitions(
        schemaVersion = 1,
        contentVersion = "test",
        robot = AssemblerRobotDefinition(GameId.of("robot_assembler"), "robot.assembler", 2),
        recipes = mapOf(
            assemblyRecipe to AssemblyRecipeDefinition(
                id = assemblyRecipe,
                nameKey = "assembly.technology",
                inputs = mapOf(refined to 1L),
                outputResourceId = technologyItem,
                outputQuantity = 1L,
                durationSeconds = 5L,
                outputKind = AssemblyOutputKind.TECHNOLOGY,
            ),
        ),
        technologies = mapOf(
            technology to TechnologyDefinition(
                id = technology,
                nameKey = "technology.extraction",
                itemResourceId = technologyItem,
                requiredTechnologyIds = emptySet(),
                extractionBonusMillionths = 100_000L,
            ),
        ),
    )

    @Test
    fun `failed save keeps the previous visible and persisted state`() {
        val save = MemorySaveService()
        seed(save)
        val coordinator = coordinator(save)
        val before = coordinator.state
        val savedAtBefore = coordinator.lastSuccessfulSaveAtEpochMillis
        save.failWrites = true

        val result = coordinator.sellAll()

        assertTrue(result is ManufacturingActionResult.PersistenceFailed)
        assertEquals(before, coordinator.state)
        assertEquals(savedAtBefore, coordinator.lastSuccessfulSaveAtEpochMillis)
        assertEquals(
            before,
            ManufacturingSnapshotCodec().decode(save.loadLatest()!!),
        )
    }

    @Test
    fun `successful save publishes the candidate state and freshness`() {
        val save = MemorySaveService()
        seed(save)
        val coordinator = coordinator(save)

        val result = coordinator.sellAll()

        assertTrue(result is ManufacturingActionResult.Applied)
        assertEquals(0L, coordinator.state.economy.inventory.getValue(raw))
        assertEquals(20L, coordinator.state.economy.spaceDollars)
        assertEquals(FixedClock.nowEpochMillis(), coordinator.lastSuccessfulSaveAtEpochMillis)
        assertEquals(0L, coordinator.secondsSinceLastSave())
        assertEquals(
            coordinator.state,
            ManufacturingSnapshotCodec().decode(save.loadLatest()!!),
        )
    }

    private fun coordinator(save: SaveService): ManufacturingCoordinator =
        ManufacturingCoordinator(
            save = save,
            clock = FixedClock,
            logger = SilentGameLogger,
            economyDefinitions = economyDefinitions,
            refiningDefinitions = refiningDefinitions,
            assemblyDefinitions = assemblyDefinitions,
        )

    private fun seed(save: SaveService) {
        val economy = CoreEconomyEngine(economyDefinitions).initialState().let { initial ->
            initial.copy(inventory = initial.inventory.toMutableMap().apply { this[raw] = 10L })
        }
        val state = ManufacturingGameState(
            economy = economy,
            refining = RefiningState.empty(),
            assembly = AssemblyState.empty(),
        )
        assertEquals(
            SaveWriteStatus.WRITTEN,
            save.save(
                ManufacturingSnapshotCodec().encode(
                    state = state,
                    contentVersion = "test",
                    savedAtEpochMillis = FixedClock.nowEpochMillis(),
                ),
            ),
        )
    }

    private object FixedClock : ClockService {
        override fun nowEpochMillis(): Long = 1_000L
        override fun monotonicMillis(): Long = 1_000L
    }

    private class MemorySaveService : SaveService {
        private var payload: SavePayload? = null
        var failWrites: Boolean = false

        override fun loadLatest(slotId: String): SavePayload? =
            payload?.takeIf { it.slotId == slotId }

        override fun save(payload: SavePayload): SaveWriteStatus {
            if (failWrites) return SaveWriteStatus.FAILED
            this.payload = payload.copy(sequence = (this.payload?.sequence ?: 0L) + 1L)
            return SaveWriteStatus.WRITTEN
        }

        override fun clear(slotId: String) {
            if (payload?.slotId == slotId) payload = null
        }
    }
}
