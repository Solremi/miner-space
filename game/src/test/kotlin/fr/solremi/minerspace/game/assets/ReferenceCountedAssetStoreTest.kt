package fr.solremi.minerspace.game.assets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReferenceCountedAssetStoreTest {
    @Test
    fun `groups queue once and unload after the final release`() {
        val texture = GameAssetDescriptor(
            id = "ui_main_panel",
            runtimePath = "ui/panels/main.webp",
            kind = AssetKind.TEXTURE,
            group = AssetGroup.CORE_UI,
            estimatedBytes = 1_024L,
        )
        val backend = FakeBackend()
        val store = ReferenceCountedAssetStore(GameAssetCatalog(listOf(texture)), backend)

        store.acquireGroup(AssetGroup.CORE_UI)
        store.acquire(texture)
        assertEquals(1, backend.queued.size)
        assertEquals(2, store.referenceCount(texture.id))
        assertEquals(1_024L, store.loadedEstimatedBytes())

        backend.loaded += texture.id
        store.releaseGroup(AssetGroup.CORE_UI)
        assertEquals(0, backend.unloaded.size)
        store.release(texture)
        assertEquals(listOf(texture.id), backend.unloaded)
    }

    @Test
    fun `catalog rejects duplicate ids and paths`() {
        val first = GameAssetDescriptor("robot_ex", "models/robot.glb", AssetKind.MODEL, AssetGroup.ROBOTS)
        assertThrows(IllegalArgumentException::class.java) {
            GameAssetCatalog(
                listOf(
                    first,
                    first.copy(runtimePath = "models/other.glb"),
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameAssetCatalog(
                listOf(
                    first,
                    first.copy(id = "robot_rf"),
                ),
            )
        }
    }

    private class FakeBackend : GameAssetBackend {
        val queued = mutableListOf<String>()
        val loaded = mutableSetOf<String>()
        val unloaded = mutableListOf<String>()
        override fun queue(descriptor: GameAssetDescriptor) { queued += descriptor.id }
        override fun isLoaded(descriptor: GameAssetDescriptor): Boolean = descriptor.id in loaded
        override fun unload(descriptor: GameAssetDescriptor) { unloaded += descriptor.id; loaded -= descriptor.id }
        override fun update(): Boolean = true
        override fun dispose() = Unit
    }
}
