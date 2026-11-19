package com.osudroid.ui.v2.skin

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.TextAlign
import kotlin.math.max

/**
 * A number of the HUD in the default skin: Torus, as tall as the digits of the font it replaces ([digitHeight], the
 * height of the default `score-0`…), so it takes the space of a texture font in the HUD's layout.
 *
 * Each character is its own text in a cell, so digits can all have the width of the widest, as a texture font's do
 * with [useFixedDigitWidth].
 */
class DefaultNumberText(override val digitHeight: Float) : NumberText() {

    // The font as rasterised, and how much smaller it's shown.
    private val font: Font
    private val resolution = DefaultSkin.TEXT_RESOLUTION

    // Where the top of the digits is in a line of text, in this number's units.
    private val digitTop: Float

    private val texts = ArrayList<Text>()

    private var isFixedDigitWidth = false

    init {
        // The size whose digits are digitHeight tall, from their height at 100 pixels.
        val probe = BuiltIn.Fonts.Torus[100f.px]
        // The bearing is negative upwards from the baseline.
        val digitRatio = -probe['0'.code].bearingY / 100f
        val fontSize = digitHeight / max(digitRatio, 0.01f)

        font = BuiltIn.Fonts.Torus[(fontSize * resolution).px]
        // The baseline is below the top of a line by the ascent, which is negative, and the top of the digits
        // above it by their bearing, negative too.
        digitTop = (-font.metrics.ascent + font['0'.code].bearingY) / resolution
    }

    override var text = ""
        set(value) {
            if (field == value) return
            field = value
            layoutText()
        }

    override var spacing = 0f
        set(value) {
            field = value
            layoutText()
        }

    override var measureText: String? = null
        set(value) {
            field = value
            layoutText()
        }

    override var textAlign = TextAlign.Left
        set(value) {
            field = value
            layoutText()
        }

    override var isAdditive = false
        set(value) {
            field = value
            for (text in texts) text.blendMode = blendMode()
        }

    override fun useFixedDigitWidth() {
        isFixedDigitWidth = true
        layoutText()
    }

    private fun blendMode() = if (isAdditive) BlendMode.Additive else BlendMode.Alpha

    // The widest digit, the width every digit takes with fixed digit widths.
    private val widestDigit by lazy {
        var widest = 0f
        for (digit in '0'..'9') widest = max(widest, font[digit.code].advance)
        widest / resolution
    }

    private fun advanceOf(char: Char): Float =
        if (isFixedDigitWidth && char in '0'..'9') widestDigit else font[char.code].advance / resolution

    private fun widthOf(text: String): Float {
        var width = 0f
        for (char in text) width += advanceOf(char)
        return width + spacing * max(0, text.length - 1)
    }

    private fun layoutText() {
        val text = text

        while (texts.size < text.length) {
            texts += Text().apply {
                origin = Anchor.TopLeft
                scaleOrigin = Anchor.TopLeft
                fontFamily = BuiltIn.Fonts.Torus
                fontSize = font.descriptor.size
                scale = 1f / resolution
                color = Color4.White
                blendMode = blendMode()
                this@DefaultNumberText += this
            }
        }

        val textWidth = widthOf(text)
        val width = max(textWidth, measureText?.let { widthOf(it) } ?: 0f)

        var x = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Center -> (width - textWidth) / 2f
            TextAlign.Right -> width - textWidth
        }

        for (i in texts.indices) {
            val piece = texts[i]

            if (i >= text.length) {
                piece.isVisible = false
                continue
            }

            val char = text[i]
            val cell = advanceOf(char)
            val glyphWidth = font[char.code].advance / resolution

            piece.isVisible = true
            piece.text = char.toString()
            // Centred in its cell.
            piece.x = (x + (cell - glyphWidth) / 2f).px
            piece.y = (-digitTop).px

            x += cell + spacing
        }

        this.width = width.px
        this.height = digitHeight.px
    }
}
