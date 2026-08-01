package fr.solremi.minerspace.game.assets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AssetBudgetStoreTest {
    @Test
    fun `group acquisition is rejected before queuing beyond the memory budget`() {
        val first = GameAssetDescriptor(
            id = "ferrum_ground",
            runtimePath = "models/ferrum_ground.glb",
            kind = AssetKind.MODEL,
            group = AssetGroup.FERRUM,
            estimatedBytes = 80L,
        )
        val second = GameAssetDescriptor(
            id = "ferrum_base",
            runtimePath = "models/ferrum_base.glb",
            kind = AssetKind.MODEL,
            group = AssetGroup.FERRUM,
            estimatedBytes = 40L,
        )
        val backend = RecordingBackend()
        val store = ReferenceCountedAssetStore(
            catalog = GameAssetCatalog(listOf(first, second)),
            backend = backend,
            memoryBudgetBytes = { 100L },
        )

        assertThrows(AssetBudgetExceededException::class.java) {
            store.acquireGroup(AssetGroup.FERRUM)
        }
        assertEquals(emptyList<String>(), backend.queued)
        assertEquals(0L, store.loadedEstimatedBytes())
    }

    private class RecordingBackend : GameAssetBackend {
        val queued = mutableListOf<String>()
        override fun queue(descriptor: GameAssetDescriptor) { queued += descriptor.id }
        override fun isLoaded(descriptor: GameAssetDescriptor): Boolean = false
        override fun unload(descriptor: GameAssetDescriptor) = Unit
        override fun update(): Boolean = true
        override fun dispose() = Unit
    }
}
