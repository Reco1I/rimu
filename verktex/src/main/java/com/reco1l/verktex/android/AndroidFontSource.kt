package com.reco1l.verktex.android

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import kotlin.math.min

/**
 * An implementation of [FontFamily.Source] that creates [Font] instances using Android's [Typeface].
 */
class AndroidFontSource(
    private val baseTypeface: Typeface,
) : FontFamily.Source {

    private val paint = Paint().apply {
        typeface = baseTypeface
        isAntiAlias = true
    }


    override fun onCreateFont(descriptor: Font.Descriptor): Font {

        val typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(
                baseTypeface,
                descriptor.weight.toAndroidTypefaceWeight(),
                descriptor.style == Font.Style.Italic
            )
        } else {
            Logger.w(
                "AndroidFontSource",
                "Typeface weight is not supported on this Android version. Using default weight."
            )

            Typeface.create(
                baseTypeface,
                descriptor.style.toAndroidTypefaceStyle(descriptor.weight)
            )
        }

        paint.typeface = typeface
        paint.textSize = descriptor.size.toPixels()

        // Apply a skew for oblique style
        if (descriptor.style == Font.Style.Oblique) {
            paint.textSkewX = -0.25f
        }

        val androidMetrics = paint.fontMetrics
        val metrics = Font.Metrics(
            ascent = androidMetrics.ascent,
            descent = androidMetrics.descent,
            leading = androidMetrics.leading,
        )

        val atlasSize = min(1024f, Verktex.graphics.device.capabilities.maxTextureSize)
        val texture = Verktex.graphics.createTexture(atlasSize, atlasSize)

        return AndroidFont(typeface, descriptor, metrics, texture)
    }

}


