package com.reco1l.verktex.ui.text

import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.ui.UICanvas
import kotlin.math.*

/**
 * A text component that uses textures for each character.
 */
open class UITextureText(val characters: Map<Char, GraphicsTexture.Region?>) : UICanvas() {

    /**
     * The spacing between glyphs.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    /**
     * The scale of the textures on the x-axis.
     */
    var textureScaleX = 1f
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    /**
     * The scale of the textures on the y-axis.
     */
    var textureScaleY = 1f
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    /**
     * When set, each character is placed in a fixed-width cell (unscaled pixels) looked up by
     * character. Characters absent from the map use their natural texture width. Useful for
     * preventing layout shifts when glyphs in the same role have varying widths.
     */
    var fixedCharWidths: Map<Char, Float>? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    /**
     * When set, content size (width/height) is measured from this string instead of [text].
     * The rendered text is still [text]. Use this to give the component a stable bounding box
     * sized for the widest value it can display, so surrounding elements don't shift.
     */
    var measureText: String? = null
        set(value) {
            if (field != value) {
                field = value
                invalidateMeasure()
            }
        }

    /**
     * Horizontal alignment of the rendered text within the component bounds.
     */
    var textAlign = TextAlign.Left


    init {
        width = Dimension.WrapContent
        height = Dimension.WrapContent
    }


    fun setTextureScale(scale: Float) {
        textureScaleX = scale
        textureScaleY = scale
    }


    private fun cellWidth(char: Char, region: GraphicsTexture.Region) =
        (fixedCharWidths?.get(char) ?: region.width) * textureScaleX

    private fun measureWidth(text: String): Float {
        var width = 0f
        var glyphs = 0

        for (char in text) {
            val region = characters[char] ?: continue
            width += cellWidth(char, region)
            glyphs++
        }

        return width + spacing * max(0, glyphs - 1)
    }


    override fun onMeasure(parentConstraints: LayoutConstraints, contentConstraints: LayoutConstraints): Vec2 {
        val measured = measureText ?: text
        val height = measured.maxOfOrNull { char -> characters[char]?.height ?: 0f } ?: 0f

        return parentConstraints.constrain(Vec2(measureWidth(measured), height * textureScaleY))
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        var offsetX = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Center -> 0.5f
            TextAlign.Right -> 1f
        } * (bounds.width - measureWidth(text))

        for (char in text) {
            val region = characters[char] ?: continue
            val textureWidth = region.width * textureScaleX
            val cellWidth = cellWidth(char, region)

            renderer.drawTexture(
                region.texture,
                region,
                offsetX + (cellWidth - textureWidth) / 2f,
                0f,
                textureWidth,
                region.height * textureScaleY,
                color = contentColor
            )

            offsetX += cellWidth + spacing
        }
    }
}
