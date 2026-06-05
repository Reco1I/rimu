package com.reco1l.andengine

import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import javax.microedition.khronos.opengles.GL10

class DynamicTextureAtlas : BitmapTextureAtlas(MaxTextureSize, MaxTextureSize) {

    val currentSources: ArrayList<IBitmapTextureAtlasSource>
        get() = mTextureAtlasSources

    val futureSources
        get() = mTextureAtlasSources + sourcesToAdd - sourcesToRemove.toSet()


    private val sourcesToAdd = mutableListOf<IBitmapTextureAtlasSource>()
    private val sourcesToRemove = mutableListOf<IBitmapTextureAtlasSource>()


    init {
        UIEngine.current.textureManager.loadTexture(this)
    }


    @Synchronized
    fun addSource(source: IBitmapTextureAtlasSource): Boolean {
        if (BlackpawnPacker.fits(this, source)) {
            sourcesToAdd.add(source)
            isUpdateOnHardwareNeeded = true
            return true
        }
        return false
    }

    @Synchronized
    fun removeSource(source: IBitmapTextureAtlasSource) {
        sourcesToRemove.add(source)
        isUpdateOnHardwareNeeded = true
    }


    override fun writeTextureToHardware(gl: GL10) {

        if (sourcesToAdd.isNotEmpty() || sourcesToRemove.isNotEmpty()) {
            mTextureAtlasSources.addAll(sourcesToAdd)
            mTextureAtlasSources.removeAll(sourcesToRemove.toSet())
        }

        BlackpawnPacker.placeSources(this, mTextureAtlasSources)
        super.writeTextureToHardware(gl)
    }
}

