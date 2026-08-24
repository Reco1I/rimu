package com.reco1l.verktex.ui.text

import com.reco1l.verktex.old.*
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.ui.UICanvas
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*
import kotlin.text.iterator

/**
 * A text component that uses textures for each character.
 */
open class UITextureText(val characters: MutableMap<Char, GraphicsTexture.Region?>) : UICanvas() {

    /**
     * The spacing between glyphs.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The scale of the textures on the x-axis.
     */
    var textureScaleX = 1f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The scale of the textures on the y-axis.
     */
    var textureScaleY = 1f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
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
                onUpdateText()
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
                onUpdateText()
            }
        }

    /**
     * Horizontal alignment of the rendered text within the content bounds.
     */
    var textAlign = TextAlign.Left
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    private val textureRegions = mutableListOf<Pair<Char, GraphicsTexture.Region>>()
    private var textContentWidth = 0f


    init {
        width = Dimension.WrapContent
        height = Dimension.WrapContent
    }


    fun setTextureScale(scale: Float) {
        textureScaleX = scale
        textureScaleY = scale
    }


    private fun onUpdateText() {
        textureRegions.clear()
        textContentWidth = 0f

        for (char in text) {
            val textureRegion = characters[char] ?: continue
            val cellWidth = (fixedCharWidths?.get(char) ?: textureRegion.width) * textureScaleX
            textureRegions.add(char to textureRegion)
            textContentWidth += cellWidth + spacing
        }

        if (textureRegions.isNotEmpty()) {
            textContentWidth -= spacing
        }

        var contentWidth = 0f
        var contentHeight = 0f

        for (char in measureText ?: text) {
            val textureRegion = characters[char] ?: continue
            val cellWidth = (fixedCharWidths?.get(char) ?: textureRegion.width.toFloat()) * textureScaleX

            contentWidth += cellWidth + spacing
            contentHeight = max(contentHeight, textureRegion.height * textureScaleY)
        }

        contentWidth -= spacing

        super.contentWidth = contentWidth
        super.contentHeight = contentHeight
    }

    override fun onDraw() {
        super.onDraw()

        var offsetX = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Center -> 0.5f
            TextAlign.Right -> 1f
        } * (contentWidth - textContentWidth)

        for (i in textureRegions.indices) {

            val (char, texture) = textureRegions[i]
            val textureWidth = texture.width * textureScaleX
            val textureHeight = texture.height * textureScaleY
            val cellWidth = (fixedCharWidths?.get(char) ?: texture.width.toFloat()) * textureScaleX

            gl.glPushMatrix()
            gl.glTranslatef(offsetX + (cellWidth - textureWidth) / 2f, 0f, 0f)

            texture.onApply(gl)

            QuadRenderer.renderQuad(0f, 0f, textureWidth, textureHeight)

            gl.glPopMatrix()

            offsetX += cellWidth + spacing
        }
    }


    override fun onDraw(renderer: GraphicsRenderer) {
        super.onDraw(renderer)



    }
}

