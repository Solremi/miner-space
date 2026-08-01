package fr.solremi.minerspace.game.assets

enum class AssetKind { TEXTURE, SOUND, MUSIC, MODEL, VFX_ATLAS, FONT }
enum class AssetGroup { CORE_UI, FERRUM, CRYOS, FRONTIER, ROBOTS, AUDIO, MARKETING_PREVIEW }

data class GameAssetDescriptor(
    val id: String,
    val runtimePath: String,
    val kind: AssetKind,
    val group: AssetGroup,
    val estimatedBytes: Long = 0L,
) {
    init {
        require(ID_PATTERN.matches(id)) { "Invalid asset id: $id" }
        require(runtimePath.isNotBlank() && !runtimePath.startsWith('/'))
        require(".." !in runtimePath.split('/'))
        require(estimatedBytes >= 0L)
    }

    private companion object {
        val ID_PATTERN = Regex("[a-z0-9]+(?:_[a-z0-9]+)*")
    }
}

class GameAssetCatalog(descriptors: Collection<GameAssetDescriptor>) {
    val byId: Map<String, GameAssetDescriptor> = descriptors.associateBy { it.id }
    val byGroup: Map<AssetGroup, List<GameAssetDescriptor>> = descriptors.groupBy { it.group }
    val estimatedBytes: Long = descriptors.sumOf { it.estimatedBytes }

    init {
        require(byId.size == descriptors.size) { "Duplicate asset id" }
        require(descriptors.map { it.runtimePath }.distinct().size == descriptors.size) {
            "Duplicate asset runtime path"
        }
    }

    fun group(group: AssetGroup): List<GameAssetDescriptor> = byGroup[group].orEmpty()
}

interface GameAssetBackend {
    fun queue(descriptor: GameAssetDescriptor)
    fun isLoaded(descriptor: GameAssetDescriptor): Boolean
    fun unload(descriptor: GameAssetDescriptor)
    fun update(): Boolean
    fun progress(): Float = if (update()) 1f else 0f
    fun dispose()
}

class AssetBudgetExceededException(
    val requestedBytes: Long,
    val budgetBytes: Long,
) : IllegalStateException("Asset budget exceeded: requested=$requestedBytes budget=$budgetBytes")

class ReferenceCountedAssetStore(
    private val catalog: GameAssetCatalog,
    private val backend: GameAssetBackend,
    private val memoryBudgetBytes: () -> Long = { Long.MAX_VALUE },
) {
    private val references = linkedMapOf<String, Int>()

    fun acquireGroup(group: AssetGroup) {
        val descriptors = catalog.group(group)
        val newlyReferenced = descriptors.filter { referenceCount(it.id) == 0 }
        val projected = Math.addExact(loadedEstimatedBytes(), newlyReferenced.sumOf { it.estimatedBytes })
        requireWithinBudget(projected)
        descriptors.forEach(::acquire)
    }

    fun releaseGroup(group: AssetGroup) {
        catalog.group(group).forEach { descriptor ->
            if (referenceCount(descriptor.id) > 0) release(descriptor)
        }
    }

    fun acquire(descriptor: GameAssetDescriptor) {
        require(catalog.byId[descriptor.id] == descriptor) { "Asset is not registered: ${descriptor.id}" }
        val count = references[descriptor.id] ?: 0
        if (count == 0) requireWithinBudget(Math.addExact(loadedEstimatedBytes(), descriptor.estimatedBytes))
        references[descriptor.id] = count + 1
        if (count == 0) backend.queue(descriptor)
    }

    fun release(descriptor: GameAssetDescriptor) {
        val count = references[descriptor.id] ?: error("Asset ${descriptor.id} was released without being acquired")
        if (count <= 1) {
            references.remove(descriptor.id)
            backend.unload(descriptor)
        } else {
            references[descriptor.id] = count - 1
        }
    }

    fun update(): Boolean = backend.update()
    fun progress(): Float = backend.progress().coerceIn(0f, 1f)
    fun referenceCount(assetId: String): Int = references[assetId] ?: 0
    fun loadedEstimatedBytes(): Long = references.keys.sumOf { catalog.byId.getValue(it).estimatedBytes }

    fun dispose() {
        references.clear()
        backend.dispose()
    }

    private fun requireWithinBudget(projectedBytes: Long) {
        val budget = memoryBudgetBytes().coerceAtLeast(1L)
        if (projectedBytes > budget) throw AssetBudgetExceededException(projectedBytes, budget)
    }
}
