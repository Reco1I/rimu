package com.reco1l.verktex.ui.text

import com.reco1l.verktex.*
import com.reco1l.verktex.Logger
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.math.almostEquals
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.data.px
import com.reco1l.verktex.fonts.Fonts
import com.reco1l.verktex.time.TickingClock


inline fun UIContainer.text(builder: UIText.() -> Unit): UIText {
    return UIText().apply(builder).also(::plusAssign)
}

/**
 * A component that can handle text rendering with various font settings, alignment, wrapping, and
 * auto-scrolling capabilities.
 *
 * This class also allows to add leading and trailing icons to the text.
 */
open class UIText : UICanvas() {

    /**
     * The text to be displayed
     */
    var text: String = ""
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    //region Icons

    /**
     * The leading icon to be displayed before the text. If set to null, no leading icon will be displayed.
     */
    var leadingIcon: IconLookup? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * The trailing icon to be displayed after the text. If set to null, no trailing icon will be displayed.
     */
    var trailingIcon: IconLookup? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    var iconSpacing: Dimension.Fixed = 0f.px
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    //endregion

    //region Font settings

    /**
     * The font family to use for this text.
     */
    var fontFamily: FontFamily = BuiltIn.Fonts.Default
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
        }

    /**
     * The font size to use for this text.
     */
    var fontSize: Dimension.Fixed = Verktex.theme.typography.body.size
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
        }

    /**
     * The font weight to use for this text.
     */
    var fontWeight = Verktex.theme.typography.body.weight
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
        }

    /**
     * The font style to use for this text.
     */
    var fontStyle = Verktex.theme.typography.body.style
        set(value) {
            if (field == value) return
            field = value
            fontSettingsChanged = true
        }

    /**
     * The font settings for this text, represented as a [Theme.FontSettings] object.
     */
    var fontSettings
        get() = Theme.FontSettings(
            family = fontFamily.name,
            size = fontSize,
            weight = fontWeight,
            style = fontStyle
        )
        set(value) {
            fontFamily = Fonts[value.family]
            fontSize = value.size
            fontWeight = value.weight
            fontStyle = value.style
        }


    private var font = fontFamily[fontSize, fontWeight, fontStyle]
    private var fontSettingsChanged = true

    //endregion

    //region Text settings

    /**
     * The alignment of the text.
     */
    var alignment = Anchor.TopLeft

    /**
     * Whether to wrap text that exceeds the width of the component.
     */
    var wrapText = true
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    /**
     * The maximum number of lines to display. If the text exceeds this number, it will be truncated
     * and an ellipsis will be added.
     */
    var maxLines = Int.MAX_VALUE
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    //endregion

    //region Auto scroll

    /**
     * Which axes to scroll the text automatically when it overflows.
     */
    var autoScrollAxes = Axis.None
        set(value) {
            if (field == Axis.Both)
                throw IllegalArgumentException("Auto scroll cannot be enabled for both axes at the same time.")

            field = value
        }

    /**
     * The speed of the auto scroll animation in pixels per second.
     */
    var autoScrollSpeed = 15f

    /**
     * The time to wait before re-starting the auto scroll animation in seconds
     */
    var autoScrollTimeout = 3f


    private var scrollProgress = 0f
    private var timeoutProgress = 0f

    //endregion


    init {
        style = Style {
            fontSettings = Verktex.theme.typography.body
        }
    }


    //region Measurement

    private var lines = listOf<String>()
    private var linesWidth = floatArrayOf()


    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {

        val text = text
        val font = fontFamily[fontSize, fontWeight, fontStyle]

        var lines: List<String>
        var linesWidth: FloatArray

        val processedLines = text
            .split('\n')
            .let { lines ->
                if (lines.size > maxLines) {
                    val truncatedLines = lines.subList(maxLines, lines.size).toMutableList()
                    truncatedLines[truncatedLines.size - 1] = truncatedLines.last() + "..."
                    truncatedLines
                } else {
                    lines
                }
            }

        val widthSpec = width
        if (wrapText && widthSpec is Dimension.ConstraintDriven) {
            val wrappedLines = mutableListOf<String>()
            val wrappedLinesWidth = mutableListOf<Float>()

            processedLines.forEach { line ->
                wrapLine(line, font, contentConstraints.availableWidth, wrappedLines, wrappedLinesWidth)
            }

            lines = wrappedLines
            linesWidth = wrappedLinesWidth.toFloatArray()
        } else {

            lines = processedLines
            linesWidth = FloatArray(processedLines.size) { i ->
                val line = processedLines[i]
                var lineWidth = 0f
                var charIndex = 0

                while (charIndex < line.length) {
                    val codePoint = line.codePointAt(charIndex)
                    val charCount = Character.charCount(codePoint)

                    lineWidth += font[codePoint].advance
                    charIndex += charCount
                }
                lineWidth
            }
        }

        this.lines = lines
        this.linesWidth = linesWidth

        var measuredWidth = linesWidth.maxOrNull() ?: 0f

        val iconSpacing = iconSpacing.toPixels()
        val leadingIcon = leadingIcon
        val trailingIcon = trailingIcon

        if (leadingIcon != null) {
            val leadingIconGlyph = leadingIcon.getGlyph(fontSize, fontWeight, fontStyle)
            measuredWidth += iconSpacing + leadingIconGlyph.advance
        }

        if (trailingIcon != null) {
            val trailingIconGlyph = trailingIcon.getGlyph(fontSize, fontWeight, fontStyle)
            measuredWidth += iconSpacing + trailingIconGlyph.advance
        }

        val measuredHeight = (font.metrics.lineHeight + font.metrics.lineGap) * lines.size - font.metrics.lineGap

        val measuredSize = Vec2(
            x = measuredWidth,
            y = measuredHeight
        )

        return parentConstraints.constrain(measuredSize.expand(padding.toVec4()))
    }

    private fun wrapLine(line: String, font: Font, maxWidth: Float, outputLines: MutableList<String>, outputWidths: MutableList<Float>) {
        if (line.isEmpty()) {
            outputLines.add("")
            outputWidths.add(0f)
            return
        }

        var currentLineStart = 0
        var currentWidth = 0f
        var lastSpaceIndex = -1
        var lastSpaceWidth = 0f
        var charIndex = 0

        while (charIndex < line.length) {
            val codePoint = line.codePointAt(charIndex)
            val charCount = Character.charCount(codePoint)
            val characterString = line.substring(charIndex, charIndex + charCount)

            val letterAdvance = font[codePoint].advance
            val newWidth = currentWidth + letterAdvance

            if (characterString == " ") {
                lastSpaceIndex = charIndex
                lastSpaceWidth = currentWidth
            }

            if (newWidth > maxWidth && currentWidth > 0) {
                if (lastSpaceIndex > currentLineStart) {
                    outputLines.add(line.substring(currentLineStart, lastSpaceIndex))
                    outputWidths.add(lastSpaceWidth)
                    currentLineStart = lastSpaceIndex + 1
                    charIndex = currentLineStart
                    currentWidth = 0f
                    lastSpaceIndex = -1
                    lastSpaceWidth = 0f
                    continue
                } else {
                    outputLines.add(line.substring(currentLineStart, charIndex))
                    outputWidths.add(currentWidth)
                    currentLineStart = charIndex
                    currentWidth = 0f
                    lastSpaceIndex = -1
                    lastSpaceWidth = 0f
                    continue
                }
            }

            currentWidth = newWidth
            charIndex += charCount
        }

        if (currentLineStart < line.length) {
            outputLines.add(line.substring(currentLineStart))
            outputWidths.add(currentWidth)
        }
    }

    //endregion

    //region Draw & Update

    override fun onDraw(renderer: GraphicsRenderer) {

        if (lines.isEmpty()) {
            return
        }

        if (linesWidth.isEmpty()) {
            Logger.w("UIText", "Lines width is empty, this should not happen! Skipping draw.")
            return
        }

        if (fontSettingsChanged) {
            fontSettingsChanged = false

            val newFont = fontFamily[fontSize, fontWeight, fontStyle]

            fontFamily.unsubscribe(font, this)
            fontFamily.subscribe(newFont, this)

            font = newFont
        }

        val font = font

        val leadingIconGlyph = leadingIcon?.let { it.family[fontSize, fontWeight, fontStyle][it.code] }
        val trailingIconGlyph = trailingIcon?.let { it.family[fontSize, fontWeight, fontStyle][it.code] }

        lines.forEachIndexed { index, string ->
            val width = linesWidth[index]

            val x = (bounds.width - width) * alignment.x
            val y = (font.metrics.lineHeight + font.metrics.lineGap) * index * alignment.y

            val offset = if (autoScrollAxes != Axis.None) {
                val scrollableSize = when (autoScrollAxes) {
                    Axis.X -> width - bounds.width
                    Axis.Y -> (font.metrics.lineHeight + font.metrics.lineGap) * lines.size - bounds.height
                    else -> 0f
                }

                scrollableSize * scrollProgress
            } else {
                0f
            }

            if (index == 0 && leadingIconGlyph != null) {
                renderer.drawText(
                    font = leadingIcon!!.family[fontSize, fontWeight, fontStyle],
                    text = String(Character.toChars(leadingIcon!!.code)),
                    x = x - if (autoScrollAxes == Axis.X) offset else 0f,
                    y = y - if (autoScrollAxes == Axis.Y) offset else 0f
                )
            }

            renderer.drawText(
                font = font,
                text = string,
                x = x - if (autoScrollAxes == Axis.X) offset else 0f,
                y = y - if (autoScrollAxes == Axis.Y) offset else 0f
            )

            if (index == lines.lastIndex && trailingIconGlyph != null) {
                renderer.drawText(
                    font = trailingIcon!!.family[fontSize, fontWeight, fontStyle],
                    text = String(Character.toChars(trailingIcon!!.code)),
                    x = x + width + iconSpacing.toPixels() - if (autoScrollAxes == Axis.X) offset else 0f,
                    y = y - if (autoScrollAxes == Axis.Y) offset else 0f
                )
            }
        }

    }

    override fun onUpdate(clock: TickingClock) {

        if (autoScrollAxes != Axis.None) {
            // Do timeout only when scroll progress is 0 or 1f.
            val inTimeout = timeoutProgress < 1f && (scrollProgress.almostEquals(0f) || scrollProgress.almostEquals(1f))

            if (inTimeout) {
                timeoutProgress += clock.elapsedTickTime / autoScrollTimeout
            } else {
                timeoutProgress = 0f
                scrollProgress = (scrollProgress + autoScrollSpeed * clock.elapsedTickTime) % 1f
            }
        }

        super.onUpdate(clock)
    }

    //endregion


    fun finalize() {
        fontFamily.unsubscribe(font, this)
    }


    /**
     * Represents the settings for an icon that can be displayed alongside the text.
     */
    data class IconLookup(
        /**
         * The Unicode code point of the icon character.
         */
        val code: Int,

        /**
         * The font family to use for the icon.
         */
        val family: FontFamily = BuiltIn.Fonts.FontAwesomeIconRegular
    ) {

        fun getGlyph(size: Dimension.Fixed, weight: Font.Weight, style: Font.Style): Font.Glyph {
            return family[size, weight, style][code]
        }
    }
}

fun faRegularIcon(code: Int) = UIText.IconLookup(code, BuiltIn.Fonts.FontAwesomeIconRegular)
fun faSolidIcon(code: Int) = UIText.IconLookup(code, BuiltIn.Fonts.FontAwesomeIconSolid)
fun faBrandsIcon(code: Int) = UIText.IconLookup(code, BuiltIn.Fonts.FontAwesomeIconBrands)


