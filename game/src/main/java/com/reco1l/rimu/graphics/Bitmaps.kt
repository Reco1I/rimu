package com.reco1l.rimu.graphics

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import androidx.core.graphics.applyCanvas
import com.caverock.androidsvg.SVG
import com.reco1l.toolkt.kotlin.orCatch
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
