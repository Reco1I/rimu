package com.reco1l.andengine

import org.anddev.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.anddev.andengine.opengl.texture.region.TextureRegion
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

/**
 * A dynamic texture region that can change its coordinates and size based on the provided texture
 * atlas state and source.
 */
class DynamicTextureRegion(
    val state: TextureAtlasState,
    val source: IBitmapTextureAtlasSource
): TextureRegion(null, 0, 0 , 0, 0) {

    override fun getTexture() = state.atlas

    override fun getHeight() = source.height
    override fun getWidth() = source.width
    override fun getTexturePositionX() = source.texturePositionX
    override fun getTexturePositionY() = source.texturePositionY


    override fun getTextureCoordinateX1(): Float {
        return source.texturePositionX.toFloat() / state.atlas.width
    }

    override fun getTextureCoordinateX2(): Float {
        return (source.texturePositionX + source.width).toFloat() / state.atlas.width
    }

    override fun getTextureCoordinateY1(): Float {
        return source.texturePositionY.toFloat() / state.atlas.height
    }

    override fun getTextureCoordinateY2(): Float {
        return (source.texturePositionY + source.height).toFloat() / state.atlas.height
    }


    override fun onApply(pGL: GL10?) {
        GLHelper.disableTextures(pGL)
        GLHelper.disableTexCoordArray(pGL)
    }

    override fun initTextureBuffer() = Unit

    override fun deepCopy(): DynamicTextureRegion {
        return DynamicTextureRegion(state, source)
    }
}