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
    private val textureRegions = mutableMapOf<String, GraphicsTexture.Region>()


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
    open fun load(key: String, path: String, isInternal: Boolean = false, destinationAtlas: GraphicsTexture? = null): GraphicsTexture.Region? {

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

        val existingRegion = textureRegions[key]
        if (existingRegion != null) {
            Logger.w("TextureManager", "Texture with key '$key' already exists. Overwriting...")
            existingRegion.texture.removeRegion(existingRegion)
        }

        val region = storeOnTextureAtlas(textureSource, destinationAtlas)
        textureRegions[key] = region
        return region
    }

    /**
     * Unloads a texture region associated with the specified key from the texture manager and removes
     * it from its texture atlas.
     *
     * @param key The key associated with the texture region to unload.
     */
    open fun unload(key: String) {
        val region = textureRegions[key] ?: run {
            Logger.w(
                "TextureManager",
                "Texture region with key '$key' not found in store. Cannot unload."
            )
            return
        }

        textureRegions.remove(key)
        region.texture.removeRegion(region)
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


    private fun createNewAtlas(): GraphicsTexture {
        val preferedSize = min(2048f, Verktex.graphics.device.capabilities.maxTextureSize)

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