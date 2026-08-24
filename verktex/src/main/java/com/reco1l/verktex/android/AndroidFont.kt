package com.reco1l.verktex.android

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.graphics.GraphicsTexture
import androidx.core.graphics.createBitmap

class AndroidFont(
    private val typeface: Typeface,
    override val descriptor: Descriptor,
    override val metrics: Metrics,
    override val texture: GraphicsTexture
) : Font() {

    override val isKerningSupported = false


    private val paint = Paint().also { paint ->
        paint.typeface = typeface
        paint.textSize = descriptor.size.toPixels()
        paint.isAntiAlias = true

        // Oblique style is not directly supported by Android's Typeface, so we apply a skew to
        // simulate it.
        if (descriptor.style == Style.Oblique) {
            paint.textSkewX = -0.25f
        }
    }

    private val canvas = android.graphics.Canvas()
    private val rect = Rect()


    override fun onCreateGlyph(
        codePoint: Int,
        texture: GraphicsTexture
    ): Glyph {
        val characters = Character.toChars(codePoint)

        paint.getTextBounds(characters, 0, characters.size, rect)

        val advance = paint.measureText(characters, 0, characters.size)

        val width = rect.width().toFloat()
        val height = rect.height().toFloat()
        val bearingX = rect.left.toFloat()
        val bearingY = rect.top.toFloat()

        if (rect.isEmpty) {
            // If the character has no visible representation (like a space), we still need to create a glyph
            // with the correct advance width, but we won't create a bitmap for it.
            return Glyph(
                codePoint = codePoint,
                advance = advance,
            )
        }

        val bitmap = createBitmap(rect.width(), rect.height())

        canvas.setBitmap(bitmap)
        canvas.drawText(String(characters), -bearingX, -bearingY, paint)

        return Glyph(
            codePoint = codePoint,
            advance = advance,
            width = width,
            height = height,
            bearingX = bearingX,
            bearingY = bearingY,
            region = texture.addSource(AndroidTextureSource(bitmap)) ?: throw IllegalStateException("Failed to add glyph bitmap to texture atlas."),
        )
    }

    override fun getKerning(first: Int, second: Int): Float {
        // Android's Paint does not provide kerning information, so we return 0.
        return 0f
    }
}