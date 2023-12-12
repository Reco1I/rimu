package com.reco1l.framework.graphics

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import androidx.core.graphics.applyCanvas
import com.caverock.androidsvg.SVG
import com.reco1l.framework.kotlin.orCatch
import com.reco1l.framework.support.WrappingTexture
import org.andengine.opengl.texture.ITextureStateListener
import org.andengine.opengl.texture.PixelFormat
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import java.io.InputStream
import kotlin.math.roundToInt


/**
 * Decode an SVG into a [Bitmap].
 */
fun SVG.toBitmap() = {

    val width = documentWidth.roundToInt()
    val height = documentHeight.roundToInt()

    Bitmap.createBitmap(width, height, ARGB_8888).applyCanvas { renderToCanvas(this) }

}.orCatch { null }

/**
 * Decodes the input stream into a [Bitmap].
 *
 * Note: This closes the input stream once the bitmap gets parsed.
 */
fun InputStream.toBitmap(): Bitmap? = use {

    BitmapFactory.decodeStream(
        this,
        null,
        Options().apply { inPreferredConfig = ARGB_8888 }
    )
}

/**
 * Creates a [WrappingTexture] instance using this bitmap as source.
 */
fun Bitmap.toTexture(
    manager: TextureManager,
    options: TextureOptions = TextureOptions.DEFAULT,
    listener: ITextureStateListener? = null

) = WrappingTexture(this, manager, options, listener)

/**
 * Get the [PixelFormat] equivalent of [Bitmap.Config].
 */
fun Bitmap.getPixelFormat(): PixelFormat
{
    return when (config)
    {
        Config.ALPHA_8 -> PixelFormat.A_8
        Config.RGB_565 -> PixelFormat.RGB_565
        Config.ARGB_4444 -> PixelFormat.RGBA_4444

        else -> PixelFormat.RGBA_8888
    }
}
