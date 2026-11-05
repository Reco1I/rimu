package com.reco1l.verktex.desktop

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack

/**
 * A [FontFamily.Source] over a TrueType or OpenType (CFF included) file, rasterised with stb_truetype.
 * Of a collection (.ttc), the first font is used.
 *
 * stb_truetype can't synthesise weights or italics: a family made of one file always draws with its
 * own weight. [Font.Style.Oblique] is a skew, as on Android.
 */
class DesktopFontSource(
    data: ByteArray,
    // Off when weights are looked up as files of their own, see DesktopSystemFontSource.
    private val warnsSynthesis: Boolean = true,
) : FontFamily.Source {

    // stb_truetype reads the file in place, it must live as long as the font info.
    private val fontData: ByteBuffer = ByteBuffer.allocateDirect(data.size).put(data).flip()

    internal val info: STBTTFontinfo = STBTTFontinfo.create()

    // In font units, scaled per font.
    internal val ascent: Int
    internal val descent: Int
    internal val lineGap: Int

    private var warnedSynthesis = false


    init {
        // The first font of a collection (.ttc), common among system fonts, or 0 for a single font.
        val offset = stbtt_GetFontOffsetForIndex(fontData, 0)
        if (offset < 0 || !stbtt_InitFont(info, fontData, offset)) {
            throw IllegalArgumentException("Not a TrueType or OpenType font.")
        }

        MemoryStack.stackPush().use { stack ->
            val a = stack.mallocInt(1)
            val d = stack.mallocInt(1)
            val g = stack.mallocInt(1)
            stbtt_GetFontVMetrics(info, a, d, g)
            ascent = a[0]
            descent = d[0]
            lineGap = g[0]
        }
    }


    override fun onCreateFont(descriptor: Font.Descriptor): Font {
        if (warnsSynthesis && !warnedSynthesis && (descriptor.weight != Font.Weight.Normal || descriptor.style == Font.Style.Italic)) {
            warnedSynthesis = true
            Logger.w("DesktopFontSource", "Font weights and italics aren't synthesised on desktop, the font's own are used.")
        }

        val size = descriptor.size.toPixels()
        // Like Paint.textSize on Android, the size is the em, not the line height.
        val scale = stbtt_ScaleForMappingEmToPixels(info, size)

        val metrics = Font.Metrics(
            // Above the baseline is negative, as on Android.
            ascent = -ascent * scale,
            descent = -descent * scale,
            leading = lineGap * scale,
        )

        val pageSize = min(PAGE_SIZE, Verktex.graphics.device.capabilities.maxTextureSize)

        return DesktopFont(this, descriptor, metrics, scale, pageSize)
    }


    private companion object {
        // See AndroidFontSource.PAGE_SIZE.
        const val PAGE_SIZE = 1024f
    }
}

/**
 * A font of a [DesktopFontSource] at one size and style.
 */
class DesktopFont(
    private val source: DesktopFontSource,
    override val descriptor: Descriptor,
    override val metrics: Metrics,
    private val scale: Float,
    private val pageSize: Float,
) : Font() {

    override val isKerningSupported = false

    // The horizontal shift per pixel above the baseline, Android's textSkewX of -0.25.
    private val skew = if (descriptor.style == Style.Oblique) OBLIQUE_SKEW else 0f


    override fun onCreatePage(): GraphicsTexture {
        return Verktex.graphics.createTexture(pageSize, pageSize)
    }

    override fun onCreateGlyph(codePoint: Int): Glyph = synchronized(source) {
        MemoryStack.stackPush().use { stack ->
            val info = source.info

            val advanceWidth = stack.mallocInt(1)
            val leftSideBearing = stack.mallocInt(1)
            stbtt_GetCodepointHMetrics(info, codePoint, advanceWidth, leftSideBearing)
            val advance = advanceWidth[0] * scale

            val x0 = stack.mallocInt(1)
            val y0 = stack.mallocInt(1)
            val x1 = stack.mallocInt(1)
            val y1 = stack.mallocInt(1)
            stbtt_GetCodepointBitmapBox(info, codePoint, scale, scale, x0, y0, x1, y1)

            val width = x1[0] - x0[0]
            val height = y1[0] - y0[0]

            if (width <= 0 || height <= 0) {
                // No visible representation (e.g. a space), only its advance.
                return Glyph(codePoint = codePoint, advance = advance)
            }

            val coverage = ByteBuffer.allocateDirect(width * height)
            stbtt_MakeCodepointBitmap(info, coverage, width, height, width, scale, scale, codePoint)

            val glyph = if (skew == 0f) {
                GlyphBitmap(coverage, width, height, x0[0])
            } else {
                skewed(coverage, width, height, x0[0], y0[0])
            }

            // The atlas packs glyphs edge to edge, and text is drawn at fractional positions, where
            // linear filtering reads half a texel past the glyph. A transparent border keeps it from
            // reading the neighbouring glyph, the box grows by it on every side.
            Glyph(
                codePoint = codePoint,
                advance = advance,
                width = (glyph.width + 2 * BORDER).toFloat(),
                height = (height + 2 * BORDER).toFloat(),
                bearingX = (glyph.left - BORDER).toFloat(),
                // y0 is the top of the box relative to the baseline, negative above it, like Rect.top
                // of Paint.getTextBounds.
                bearingY = (y0[0] - BORDER).toFloat(),
                region = addGlyphSource(glyph.toTextureSource()),
            )
        }
    }

    override fun getKerning(first: Int, second: Int): Float {
        // Off as on Android, so text is laid out the same on both.
        return 0f
    }


    private class GlyphBitmap(val coverage: ByteBuffer, val width: Int, val height: Int, val left: Int) {

        // White with the coverage as straight alpha, as the Android platform draws glyphs, inside a
        // transparent border of BORDER pixels.
        fun toTextureSource(): PixelsTextureSource {
            val outerWidth = width + 2 * BORDER
            val outerHeight = height + 2 * BORDER

            // Zeroed: the border is transparent.
            val pixels = ByteBuffer.allocateDirect(outerWidth * outerHeight * 4).order(ByteOrder.nativeOrder())
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val offset = ((y + BORDER) * outerWidth + x + BORDER) * 4
                    pixels.put(offset, 0xFF.toByte())
                    pixels.put(offset + 1, 0xFF.toByte())
                    pixels.put(offset + 2, 0xFF.toByte())
                    pixels.put(offset + 3, coverage.get(y * width + x))
                }
            }
            return PixelsTextureSource(outerWidth, outerHeight, pixels)
        }
    }

    // Shears the bitmap as Paint.textSkewX does, x' = x + skew * y with y relative to the baseline
    // (negative above it), so the top leans right.
    private fun skewed(coverage: ByteBuffer, width: Int, height: Int, left: Int, top: Int): GlyphBitmap {
        fun shift(row: Int) = skew * (top + row + 0.5f)

        val minShift = min(shift(0), shift(height - 1))
        val maxShift = max(shift(0), shift(height - 1))
        val newLeft = floor(left + minShift).toInt()
        val newWidth = ceil(left + width + maxShift).toInt() - newLeft

        val result = ByteBuffer.allocateDirect(newWidth * height)

        for (row in 0 until height) {
            // Where the source's column 0 lands in the result, split into whole and fractional parts.
            val offset = left + shift(row) - newLeft
            val whole = floor(offset).toInt()
            val fraction = offset - whole

            for (x in 0 until newWidth) {
                val source = x - whole
                // Linear interpolation between the two source pixels covering this one.
                val a = sample(coverage, width, row, source)
                val b = sample(coverage, width, row, source - 1)
                val value = a * (1f - fraction) + b * fraction
                result.put(row * newWidth + x, value.toInt().coerceIn(0, 255).toByte())
            }
        }

        return GlyphBitmap(result, newWidth, height, newLeft)
    }

    private fun sample(coverage: ByteBuffer, width: Int, row: Int, column: Int): Float {
        if (column < 0 || column >= width) return 0f
        return (coverage.get(row * width + column).toInt() and 0xFF).toFloat()
    }


    private companion object {
        const val OBLIQUE_SKEW = -0.25f

        // The transparent pixels around every glyph, see onCreateGlyph.
        const val BORDER = 1
    }
}
