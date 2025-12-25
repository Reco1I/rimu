package com.reco1l.verktex.texture

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.graphics.GraphicsTexture
import kotlin.collections.iterator
import kotlin.math.min

/**
 * The [Textures] class is responsible for managing textures and texture regions in the application.
 * It provides functionality to load, unload, and retrieve textures and their associated regions.
 */
object Textures {

    private val atlases = mutableListOf<GraphicsTexture>()
    private val standaloneTextures = mutableListOf<GraphicsTexture>()
    private val textureRegions = mutableMapOf<String, GraphicsTexture.Region>()


    /**
     * Forgets every texture without releasing it, for when the graphics context they belong to was
     * destroyed along with them.
     */
    internal fun clear() {
        atlases.clear()
        standaloneTextures.clear()
        textureRegions.clear()
    }

    //region Load and Unload

    /**
     * Loads multiple texture frames from the specified file paths and stores them in a [FramedTextureRegion].
     */
    fun load(key: String, paths: List<String>, isInternal: Boolean = false): GraphicsTexture.Region? {
        if (paths.isEmpty()) {
            Logger.w("TextureManager", "No paths provided for loading frames with key: $key")
            return null
        }

        val frames = mutableListOf<GraphicsTexture.Region>()
        val dedicatedAtlas = createNewAtlas()

        for (path in paths) {
            val region = load(key, path, isInternal, dedicatedAtlas)

            if (region == null) {
                Logger.w("TextureManager", "Failed to load texture from path: $path")
                continue
            }

            frames.add(region)
        }

        if (frames.isEmpty()) {
            Logger.w("TextureManager", "No valid frames loaded for key: $key")
            return null
        }

        val source = FramedTextureRegion.Source(frames)

        return FramedTextureRegion(frames, dedicatedAtlas, source)
    }

    /**
     * This function will load a texture from the specified file path and store it in a free texture atlas.
     *
     * @param key A unique key to associate with the loaded texture.
     * @param path The file path of the texture to load.
     * @param isInternal A boolean indicating whether the file path is internal to the application.
     * @return A [GraphicsTexture.Region] representing the loaded texture.
     */
    fun load(key: String, path: String, isInternal: Boolean = false, destinationAtlas: GraphicsTexture? = null): GraphicsTexture.Region? {

        /*val highDefinitionVariantPath = filePath.substringAfterLast('.') + "@2x." + filePath.substringAfterLast('.')

        val path = if (VerktexEngine.platform.sourceProvider.exists(highDefinitionVariantPath, !isInternal)) {
            highDefinitionVariantPath
        } else {
            filePath
        }*/

        val textureSource = Platform.provider.createTextureSource(path, !isInternal)
            ?: run {
                Logger.w("TextureManager", "Failed to create texture source for path: $path")
                return null
            }

        return load(key, textureSource, destinationAtlas = destinationAtlas)
    }

    /**
     * Loads a texture from an already created source and stores it under the specified key, replacing any
     * previous region with the same key.
     *
     * @param key A unique key to associate with the loaded texture.
     * @param source The texture source to upload.
     * @param standalone Whether the source gets its own texture instead of being packed into an atlas. Sources
     * larger than the atlas size are always standalone.
     * @param destinationAtlas The atlas to store the source in, if any.
     * @return A [GraphicsTexture.Region] representing the loaded texture, or null if the source exceeds the
     * maximum texture size.
     */
    fun load(
        key: String,
        source: GraphicsTexture.Source,
        standalone: Boolean = false,
        destinationAtlas: GraphicsTexture? = null
    ): GraphicsTexture.Region? {

        val existingRegion = textureRegions.remove(key)
        if (existingRegion != null) {
            Logger.w("TextureManager", "Texture with key '$key' already exists. Overwriting...")
            release(existingRegion)
        }

        val region = if (destinationAtlas == null && (standalone || source.width > atlasSize() || source.height > atlasSize())) {
            storeStandalone(source) ?: run {
                Logger.w("TextureManager", "Texture '$key' (${source.width}x${source.height}) exceeds the maximum texture size.")
                return null
            }
        } else {
            storeOnTextureAtlas(source, destinationAtlas)
        }
        textureRegions[key] = region
        return region
    }

    /**
     * Unloads a texture region associated with the specified key from the texture manager and removes
     * it from its texture atlas.
     *
     * @param key The key associated with the texture region to unload.
     */
    fun unload(key: String) {
        val region = textureRegions[key] ?: run {
            Logger.w(
                "TextureManager",
                "Texture region with key '$key' not found in store. Cannot unload."
            )
            return
        }

        textureRegions.remove(key)
        release(region)
    }

    /**
     * Detaches [region] from its texture, and disposes the texture if it is a standalone one.
     */
    private fun release(region: GraphicsTexture.Region) {
        val texture = region.texture

        if (standaloneTextures.remove(texture)) {
            // The GPU delete must happen on the draw worker (GL thread). The texture stays registered until
            // then, so a context loss in between still marks it and the old name is not deleted in the new context.
            Verktex.scheduleOnDraw {
                texture.dispose()
                Verktex.graphics.unregisterResource(texture)
            }
        } else {
            texture.removeRegion(region)
        }
    }

    /**
     * Unloads a texture region from the texture manager and removes it from its texture atlas.
     */
    fun unload(region: GraphicsTexture.Region) {
        val key = textureRegions.entries.find { it.value == region }?.key ?: run {
            Logger.w("TextureManager", "Texture region not found in store. Cannot unload.")
            return
        }
        unload(key)
    }

    /**
     * Unloads all texture regions that match the specified predicate from the texture manager and removes
     * them from their respective texture atlases.
     */
    fun unloadAll(predicate: (key: String, region: GraphicsTexture.Region) -> Boolean = { _, _ -> true }) {
        val keysToUnload = textureRegions.filter { predicate(it.key, it.value) }

        for (entry in keysToUnload) {
            unload(entry.key)
        }
    }

    //endregion

    //region Operators

    operator fun contains(key: String): Boolean {
        return textureRegions.containsKey(key)
    }

    operator fun get(key: String): GraphicsTexture.Region? {
        return textureRegions[key]
    }

    //endregion


    /**
     * Returns a set of all texture keys currently stored in the texture manager.
     */
    fun getTextureKeys(): Set<String> {
        return textureRegions.keys
    }


    private fun atlasSize(): Float = min(2048f, Verktex.graphics.device.capabilities.maxTextureSize)

    /**
     * Stores a [GraphicsTexture.Source] larger than an atlas on a texture of exactly its own size.
     * Returns null when the source exceeds the device's maximum texture size.
     */
    private fun storeStandalone(source: GraphicsTexture.Source): GraphicsTexture.Region? {
        val maxSize = Verktex.graphics.device.capabilities.maxTextureSize

        if (source.width > maxSize || source.height > maxSize) {
            return null
        }

        val texture = Verktex.graphics.createTexture(width = source.width, height = source.height)
        val region = texture.addSource(source)

        if (region == null) {
            texture.dispose()
            Verktex.graphics.unregisterResource(texture)
            return null
        }

        standaloneTextures += texture
        return region
    }

    private fun createNewAtlas(): GraphicsTexture {
        val preferedSize = atlasSize()

        val newAtlas = Verktex.graphics.createTexture(
            width = preferedSize,
            height = preferedSize
        )

        atlases += newAtlas
        return newAtlas
    }

    /**
     * Stores a [GraphicsTexture.Source] in a free texture atlas. If no existing atlas can accommodate the
     * source, a new atlas will be created.
     */
    private fun storeOnTextureAtlas(source: GraphicsTexture.Source, destinationAtlas: GraphicsTexture? = null): GraphicsTexture.Region {

        if (destinationAtlas != null) {
            if (destinationAtlas.fitsSource(source)) {
                return destinationAtlas.addSource(source)
                    ?: throw IllegalStateException("Failed to allocate texture source on specified atlas.")
            } else {
                throw IllegalArgumentException("The specified destination atlas cannot accommodate the source.")
            }
        }

        for (atlas in atlases) {
            if (atlas.fitsSource(source)) {
                return atlas.addSource(source) ?: continue
            }
        }

        val newAtlas = createNewAtlas()

        return newAtlas.addSource(source) ?: throw IllegalStateException("Failed to allocate texture source on new atlas.")
    }

}