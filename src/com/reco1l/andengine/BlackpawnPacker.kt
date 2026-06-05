package com.reco1l.andengine

import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.atlas.buildable.BuildableTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureBuilder

object BlackpawnPacker :
    BlackPawnTextureBuilder<IBitmapTextureAtlasSource, DynamicTextureAtlas>(0) {

    override fun pack(
        atlas: DynamicTextureAtlas,
        sources: ArrayList<BuildableTextureAtlas.TextureAtlasSourceWithWithLocationCallback<IBitmapTextureAtlasSource>>
    ) {
        throw UnsupportedOperationException("This method should not be called directly. Use the pack method that takes a list of sources instead.")
    }


    fun fits(atlas: DynamicTextureAtlas, source: IBitmapTextureAtlasSource): Boolean {
        return insertToRoot(atlas, (atlas.futureSources + source).sortBySize())
    }

    fun placeSources(atlas: DynamicTextureAtlas, sources: List<IBitmapTextureAtlasSource>) {
        insertToRoot(atlas, sources.sortBySize(), true)
    }


    private fun List<IBitmapTextureAtlasSource>.sortBySize(): List<IBitmapTextureAtlasSource> {
        return this.sortedWith { a, b ->
            val deltaWidth = b.width - a.width
            if (deltaWidth != 0) deltaWidth else b.height - a.height
        }
    }

    private fun insertToRoot(
        atlas: DynamicTextureAtlas,
        sources: List<IBitmapTextureAtlasSource>,
        applyCoordinates: Boolean = false
    ): Boolean {
        val root = Node(Rect(0, 0, atlas.width, atlas.height))

        sources.forEach { source ->
            val inserted = root.insert(source, atlas.width, atlas.height, 0) ?: return false

            if (applyCoordinates) {
                source.texturePositionX = inserted.rect.left
                source.texturePositionY = inserted.rect.top
            }
        }
        return true
    }



}