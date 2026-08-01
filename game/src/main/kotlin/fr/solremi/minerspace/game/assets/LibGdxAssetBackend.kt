package fr.solremi.minerspace.game.assets

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g3d.Model

fun interface GlbLoaderRegistrar {
    fun register(assetManager: AssetManager)
}

class LibGdxAssetBackend(
    private val manager: AssetManager = AssetManager(),
    glbLoaderRegistrar: GlbLoaderRegistrar? = null,
) : GameAssetBackend {
    private val glbLoaderRegistered = glbLoaderRegistrar != null

    init {
        glbLoaderRegistrar?.register(manager)
    }

    override fun queue(descriptor: GameAssetDescriptor) {
        if (manager.isLoaded(descriptor.runtimePath) || manager.contains(descriptor.runtimePath)) return
        if (
            descriptor.kind == AssetKind.MODEL &&
            descriptor.runtimePath.endsWith(".glb", ignoreCase = true) &&
            !glbLoaderRegistered
        ) {
            error(
                "No GLB loader registered for ${descriptor.runtimePath}. " +
                    "Provide GlbLoaderRegistrar when final model assets are added.",
            )
        }
        loadUntyped(descriptor.runtimePath, typeFor(descriptor.kind))
    }

    override fun isLoaded(descriptor: GameAssetDescriptor): Boolean =
        manager.isLoaded(descriptor.runtimePath)

    override fun unload(descriptor: GameAssetDescriptor) {
        if (manager.contains(descriptor.runtimePath)) manager.unload(descriptor.runtimePath)
    }

    override fun update(): Boolean = manager.update()
    override fun progress(): Float = manager.progress.coerceIn(0f, 1f)

    fun <T : Any> get(descriptor: GameAssetDescriptor, type: Class<T>): T {
        require(isLoaded(descriptor)) { "Asset is not loaded: ${descriptor.id}" }
        return manager.get(descriptor.runtimePath, type)
    }

    override fun dispose() {
        manager.dispose()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadUntyped(path: String, type: Class<*>) {
        manager.load(path, type as Class<Any>)
    }

    private fun typeFor(kind: AssetKind): Class<*> = when (kind) {
        AssetKind.TEXTURE -> Texture::class.java
        AssetKind.SOUND -> Sound::class.java
        AssetKind.MUSIC -> Music::class.java
        AssetKind.MODEL -> Model::class.java
        AssetKind.VFX_ATLAS -> TextureAtlas::class.java
        AssetKind.FONT -> BitmapFont::class.java
    }
}
