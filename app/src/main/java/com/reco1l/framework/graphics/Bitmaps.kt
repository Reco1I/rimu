package com.reco1l.framework.graphics

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import androidx.core.graphics.applyCanvas
import com.caverock.androidsvg.SVG
import com.reco1l.framework.lang.orCatch
import java.io.InputStream
import kotlin.math.roundToInt
import kotlin.text.Charsets.UTF_8


/**
 * Decode an SVG into a [Bitmap].
 */
fun SVG.toBitmap() = {

    val width = documentWidth.roundToInt()
    val height = documentHeight.roundToInt()

    Bitmap.createBitmap(width, height, Config.ARGB_8888).applyCanvas { renderToCanvas(this) }

}.orCatch { null }

/**
 * Decodes the input stream into a [Bitmap].
 *
 * Note: This closes the input stream once the bitmap gets parsed.
 */
fun InputStream.toBitmap(): Bitmap?
{
    // Determining if it's an SVG to use properly parsing.
    val isSVG = {

        // Reading first 5 bytes to determine if the stream comes from an SVG file, this will check the
        // first characters that should equal to the "<svg>" or "<?xml" tag in case it's a SVG file.
        val parsed = ByteArray(5).takeIf { read(it) == 5 }?.toString(UTF_8)

        parsed?.startsWith("<svg") == true || parsed?.startsWith("<?xml") == true

    }.orCatch { false }

    // Resetting the InputStream so we can utilize it below.
    reset()

    use {
        if (isSVG)
            return SVG.getFromInputStream(this).toBitmap()

        return BitmapFactory.decodeStream(this)
    }
}

