package com.reco1l.rimu.graphics

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_UNPACK_ALIGNMENT
import android.opengl.GLUtils
import com.reco1l.rimu.extensions.getPixelFormat
import com.reco1l.toolkt.isPowerOfTwo
import org.andengine.opengl.texture.ITextureStateListener
import org.andengine.opengl.texture.PixelFormat.RGBA_8888
import org.andengine.opengl.texture.Texture
import org.andengine.opengl.texture.bitmap.BitmapTexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.util.GLState.GL_UNPACK_ALIGNMENT_DEFAULT


/**
 * Improved version of [BitmapTexture] from AndEngine which allows to reuse an already created bitmap
 * without recycling it (because it's not needed anymore).
 */
class WrappingTexture(

    val bitmap: Bitmap,

    manager: TextureManager,

    options: TextureOptions = TextureOptions.DEFAULT,

    listener: ITextureStateListener? = null

) : Texture(manager, bitmap.getPixelFormat(), options, listener)
{

    /**
     * The shared instance of the region holding this texture.
     * Transformations shouldn't be applied to this instance, use [toTextureRegion] instead.
     */
    val sharedTextureRegion by lazy { toTextureRegion() }


    private val useDefaultAlignment = pixelFormat == RGBA_8888
            && width.isPowerOfTwo()
            && height.isPowerOfTwo()


    override fun getWidth() = bitmap.width

    override fun getHeight() = bitmap.height


    override fun writeTextureToHardware(gl: GLState)
    {
        if (!useDefaultAlignment)
            GLES20.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

        when (mTextureOptions.mPreMultiplyAlpha)
        {
            true -> GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

            false -> gl.glTexImage2D(GL_TEXTURE_2D, 0, bitmap, 0, mPixelFormat)
        }

        if (!useDefaultAlignment)
            GLES20.glPixelStorei(GL_UNPACK_ALIGNMENT, GL_UNPACK_ALIGNMENT_DEFAULT)
    }


}

/**
 * Returns a new [TextureRegion] with this texture as source.
 */
fun WrappingTexture.toTextureRegion() = TextureRegion(this, 0f, 0f, width.toFloat(), height.toFloat())