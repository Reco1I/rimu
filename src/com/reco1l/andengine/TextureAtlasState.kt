package com.reco1l.andengine

import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.atlas.buildable.BuildableTextureAtlas.TextureAtlasSourceWithWithLocationCallback
import org.anddev.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureBuilder

class TextureAtlasState {

    /**
     * The texture atlas used for storing all texture sources.
     */
    var atlas: BitmapTextureAtlas = BitmapTextureAtlas(MaxTextureSize, MaxTextureSize)

    /**
     * Indicates whether there is still space available in the atlas for new sources. This is set to
     * false when an attempt to add a source fails due to insufficient space.
     */
    var spaceAvailable: Boolean = true
        private set



    //private val subscribers = mutableListOf<WeakReference<UIComponent>>()


    fun addSource(source: IBitmapTextureAtlasSource): Boolean {
        val sources = atlas.textureAtlasSources.toMutableList()
        sources.add(source)

        return buildSources(sources)
    }

    fun removeSource(source: IBitmapTextureAtlasSource) {
        if (source in atlas.textureAtlasSources) {
            val sources = atlas.textureAtlasSources.toMutableList()
            sources.remove(source)

            buildSources(sources)
        }
    }


    private fun buildSources(sources: List<IBitmapTextureAtlasSource>): Boolean {
        val builder = BlackPawnTextureBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(0)
        val oldAtlas = atlas

        try {
            val newAtlas = BitmapTextureAtlas(MaxTextureSize, MaxTextureSize)

            val sourcesWithCallback = sources.map {
                TextureAtlasSourceWithWithLocationCallback(it, {})
            }

            builder.pack(newAtlas, ArrayList(sourcesWithCallback))

            atlas = newAtlas
        } catch (_: Exception) {
            spaceAvailable = false
        }

        val textureManager = UIEngine.current.textureManager
        textureManager.loadTexture(atlas)
        textureManager.unloadTexture(oldAtlas)

        return spaceAvailable
    }

}