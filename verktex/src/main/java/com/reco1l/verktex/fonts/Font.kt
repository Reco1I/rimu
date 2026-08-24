package com.reco1l.verktex.fonts

import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * Represents a specific font with its associated metrics, texture, and glyphs.
 * This class is responsible for managing the rendering of individual glyphs and their corresponding
 * texture regions.
 */
abstract class Font {

    /**
     * The descriptor that defines the characteristics of this font, including its family, size,
     * weight, and style.
     */
    abstract val descriptor: Descriptor

    /**
     * The metrics of the font, which provide information about the font's vertical layout and spacing.
     */
    abstract val metrics: Metrics

    /**
     * The underlying texture that contains the rendered glyphs of the font.
     */
    abstract val texture: GraphicsTexture

    /**
     * Indicates whether kerning is supported for this font. Kerning is the adjustment of space
     * between specific pairs of characters to improve visual appearance.
     */
    abstract val isKerningSupported: Boolean


    private val glyphs: MutableMap<Int, Glyph> = mutableMapOf()


    operator fun get(codePoint: Int): Glyph {
        return glyphs.getOrPut(codePoint) {
            onCreateGlyph(codePoint, texture)
        }
    }


    /**
     * This function should be implemented to render a single glyph into the texture and return its
     * corresponding texture region.
     */
    abstract fun onCreateGlyph(codePoint: Int, texture: GraphicsTexture): Glyph

    /**
     * Provides the kerning value between two characters, which is the additional adjustment of space
     * between them.
     */
    abstract fun getKerning(first: Int, second: Int): Float


    /**
     * Represents the descriptor of a font, which includes its family, size, weight, and style.
     */
    data class Descriptor(
        /**
         * The font family to which this font belongs.
         */
        val family: FontFamily,

        /**
         * The size of the font in points. This determines how large the text will appear when rendered.
         */
        val size: Dimension.Fixed,

        /**
         * The weight of the font, which indicates the thickness of the characters.
         */
        val weight: Weight = Weight.Normal,

        /**
         * The style of the font, which can be normal, italic, or oblique.
         */
        val style: Style = Style.Normal
    )

    /**
     * Represents the style of a font.
     */
    enum class Style {
        Normal,
        Italic,
        Oblique
    }

    /**
     * Represents the weight of a font, which indicates the thickness of the characters.
     */
    enum class Weight {
        Thin,
        ExtraLight,
        Light,
        Normal,
        Medium,
        SemiBold,
        Bold,
        ExtraBold,
        Black
    }

    /**
     * Represents a single glyph (character) in the font, including its character representation,
     * advance width, and texture region.
     */
    data class Glyph(
        /**
         * The Unicode code point of the character represented by this glyph.
         * This uniquely identifies the character in the Unicode standard.
         */
        val codePoint: Int,

        /**
         * The horizontal distance to advance the cursor position after rendering this glyph, in pixels.
         */
        val advance: Float,

        /**
         * The horizontal offset from the cursor position to the left edge of the glyph's image,
         * in pixels.
         */
        val bearingX: Float = 0f,

        /**
         * The vertical offset from the baseline to the top edge of the glyph's image, in pixels.
         */
        val bearingY: Float = 0f,

        /**
         * The width of the glyph's image in pixels.
         */
        val width: Float = 0f,

        /**
         * The height of the glyph's image in pixels.
         */
        val height: Float = 0f,

        /**
         * The region of the font texture that contains the glyph's image. This is used for rendering the glyph.
         *
         * When the glyph is not yet rendered, this value may be null, indicating that the glyph'
         * s texture region has not been defined.
         */
        val region: GraphicsTexture.Region? = null,

        )

    /**
     * Represents the font metrics, which provide information about the font's vertical layout and spacing.
     */
    data class Metrics(

        /**
         * The recommended distance above the baseline for singled spaced text.
         */
        val ascent: Float,

        /**
         * The recommended distance below the baseline for singled spaced text.
         */
        val descent: Float,

        /**
         * The recommended additional space to add between lines of text.
         */
        val leading: Float,
    ) {

        val lineHeight
            get() = ascent - descent + leading

        val lineGap
            get() = leading
    }
}