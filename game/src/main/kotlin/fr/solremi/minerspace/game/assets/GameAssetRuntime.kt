package fr.solremi.minerspace.game.assets

import fr.solremi.minerspace.game.performance.RuntimePerformanceBudget
import fr.solremi.minerspace.shared.GameLogger
import fr.solremi.minerspace.shared.SilentGameLogger

class GameAssetRuntime(
    val catalog: GameAssetCatalog,
    private val backend: GameAssetBackend,
    private val budget: () -> RuntimePerformanceBudget,
    private val logger: GameLogger = SilentGameLogger,
) {
    private val store = ReferenceCountedAssetStore(
        catalog = catalog,
        backend = backend,
        memoryBudgetBytes = { budget().assetMemoryBudgetBytes },
    )
    private val activeGroups = linkedSetOf<AssetGroup>()

    fun acquire(group: AssetGroup): Boolean {
        if (!activeGroups.add(group)) return true
        return runCatching {
            store.acquireGroup(group)
            true
        }.onFailure {
            activeGroups.remove(group)
            logger.error(TAG, "Unable to acquire asset group $group.", it)
        }.getOrDefault(false)
    }

    fun release(group: AssetGroup) {
        if (!activeGroups.remove(group)) return
        store.releaseGroup(group)
    }

    fun update(): Boolean = store.update()
    fun progress(): Float = store.progress()
    fun loadedEstimatedBytes(): Long = store.loadedEstimatedBytes()
    fun activeGroups(): Set<AssetGroup> = activeGroups.toSet()

    fun dispose() {
        activeGroups.clear()
        store.dispose()
    }

    companion object {
        private const val TAG = "GameAssetRuntime"

        fun empty(
            backend: GameAssetBackend = LibGdxAssetBackend(),
            budget: () -> RuntimePerformanceBudget,
            logger: GameLogger = SilentGameLogger,
        ): GameAssetRuntime = GameAssetRuntime(
            catalog = GameAssetCatalog(emptyList()),
            backend = backend,
            budget = budget,
            logger = logger,
        )
    }
}
