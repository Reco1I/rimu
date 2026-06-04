package com.reco1l.andengine

import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

/**
 * A dynamic texture region that can change its coordinates and size based on the provided texture
 * atlas state and source.
 */
class DynamicTextureRegion(

    /**
     * The state of the texture atlas that this region belongs to.
     */
    val atlas: DynamicTextureAtlas,

    /**
     * The source of this texture region, which provides the coordinates and size of the region
     * within the texture atlas.
     */
    val source: IBitmapTextureAtlasSource

): TextureRegion(null, 0, 0 , 0, 0) {

    override fun getTexture(): BitmapTextureAtlas {
        return atlas
    }

    override fun getHeight(): Int {
        return source.height
    }

    override fun getWidth(): Int {
        return source.width
    }


    override fun getTexturePositionX(): Int {
        return source.texturePositionX
    }

    override fun getTexturePositionY(): Int {
        return source.texturePositionY
    }


    override fun getTextureCoordinateX1(): Float {
        return source.texturePositionX.toFloat() / atlas.width
    }

    override fun getTextureCoordinateX2(): Float {
        return (source.texturePositionX + source.width).toFloat() / atlas.width
    }

    override fun getTextureCoordinateY1(): Float {
        return source.texturePositionY.toFloat() / atlas.height
    }

    override fun getTextureCoordinateY2(): Float {
        return (source.texturePositionY + source.height).toFloat() / atlas.height
    }

    override fun initTextureBuffer() {
        // We prevent initialization of the texture region buffer because it will cause an
        // initialization exception, and it is not needed because it will be updated every time we
        // apply the texture region.
    }

    // This function will be only called in legacy sprites components.
    override fun onApply(pGL: GL10) {
        // We don't track changes in the source or atlas, so we need to update the texture region
        // buffer every time we apply the texture region. This is only needed for legacy sprites
        // components that rely on the texture region buffer.
        updateTextureRegionBuffer()

        atlas.bind(pGL)

        if (GLHelper.EXTENSIONS_VERTEXBUFFEROBJECTS) {
            val gl11 = pGL as GL11

            mTextureRegionBuffer.selectOnHardware(gl11)
            GLHelper.texCoordZeroPointer(gl11)
        } else {
            GLHelper.texCoordPointer(pGL, mTextureRegionBuffer.floatBuffer)
        }
    }

    override fun deepCopy(): DynamicTextureRegion {
        return DynamicTextureRegion(atlas, source)
    }
}