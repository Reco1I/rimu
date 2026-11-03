package com.reco1l.verktex.ui.text

import com.reco1l.verktex.*
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.UICanvas
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.data.px
import com.reco1l.verktex.fonts.Fonts
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.data.Color4
import kotlin.math.max


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
     * The text to be displayed, without styles. Setting it replaces the [spannedText].
     */
    var text: String
        get() = spannedText.text
        set(value) {
            if (spannedText.isPlain && spannedText.text == value) return
            spannedText = SpannedText.of(value)
        }

    /**
     * The text to be displayed, made of spans with their own color, font and background, see
     * [spans] to build one.
     */
    var spannedText: SpannedText = SpannedText.Empty
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    //region Icons

    /**
     * The leading icon to be displayed before the text. If set to null, no leading icon will be displayed.
     */
    var leadingIcon: IconLookup? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The trailing icon to be displayed after the text. If set to null, no trailing icon will be displayed.
     */
    var trailingIcon: IconLookup? = null
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    var iconSpacing: Dimension.Fixed = 0f.px
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
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
            invalidateMeasure()
        }

    /**
     * The font size to use for this text.
     */
    var fontSize: Dimension.Fixed = Verktex.theme.typography.body.size
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The font weight to use for this text.
     */
    var fontWeight = Verktex.theme.typography.body.weight
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * The font style to use for this text.
     */
    var fontStyle = Verktex.theme.typography.body.style
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
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
            invalidateMeasure()
        }

    /**
     * The maximum number of lines to display. If the text exceeds this number, it will be truncated
     * and an ellipsis will be added.
     */
    var maxLines = Int.MAX_VALUE
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    //endregion

    //region Auto scroll

    /**
     * Which axes to scroll the text automatically when it overflows.
     */
    var autoScrollAxes = Axis.None
        set(value) {
            if (value == Axis.Both)
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

    // Overflowing size along the auto scroll axis, updated when drawing.
    private var autoScrollDistance = 0f

    //endregion



    //region Measurement

    /**
     * A run of a span laid out on a line, drawn with a single font and color.
     */
    private class Fragment(
        val text: String,
        val font: Font,
        val color: Color4?,
        val backgroundColor: Color4?,
        val verticalAlign: SpanStyle.VerticalAlign,
        val x: Float,
        val width: Float,
    )

    /**
     * A laid out line, [ascent] is the distance from its top to the baseline shared by its text.
     */
    private class Line(
        val fragments: List<Fragment>,
        val width: Float,
        val height: Float,
        val ascent: Float,
    )

    /**
     * A span with its style resolved against this text's settings.
     */
    private class ResolvedSpan(
        val text: String,
        val font: Font,
        val color: Color4?,
        val backgroundColor: Color4?,
        val verticalAlign: SpanStyle.VerticalAlign,
        val spaceWidth: Float,
        val isSpace: Boolean,
    )

    private var lines = listOf<Line>()
    private var lineGap = 0f
    private var textHeight = 0f


    // The icons around the text are spans as well.
    private fun buildSpans(): List<Span> {
        val leadingIcon = leadingIcon
        val trailingIcon = trailingIcon

        if (leadingIcon == null && trailingIcon == null) {
            return spannedText.spans
        }

        return buildSpannedText {
            if (leadingIcon != null) {
                icon(leadingIcon)
                space(iconSpacing)
            }
            +spannedText
            if (trailingIcon != null) {
                space(iconSpacing)
                icon(trailingIcon)
            }
        }.spans
    }

    private fun resolve(span: Span): ResolvedSpan {
        val style = span.style
        val family = style.fontFamily ?: fontFamily

        return ResolvedSpan(
            text = span.text,
            font = family[style.fontSize ?: fontSize, style.fontWeight ?: fontWeight, style.fontStyle ?: fontStyle],
            color = style.color,
            backgroundColor = style.backgroundColor,
            verticalAlign = style.verticalAlign ?: SpanStyle.VerticalAlign.Baseline,
            spaceWidth = span.spaceWidth?.toPixels() ?: 0f,
            isSpace = span.spaceWidth != null,
        )
    }

    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {

        val baseFont = fontFamily[fontSize, fontWeight, fontStyle]
        val spans = buildSpans().map(::resolve)

        // Every code point is a cell, so lines can break anywhere across spans.
        val cellSpan = mutableListOf<Int>()
        val cellStart = mutableListOf<Int>()
        val cellEnd = mutableListOf<Int>()
        val cellAdvance = mutableListOf<Float>()
        // Kerning with the previous cell of the same span, ignored at the start of a line.
        val cellKerning = mutableListOf<Float>()
        val cellKind = mutableListOf<Int>()

        spans.forEachIndexed { spanIndex, span ->
            if (span.isSpace) {
                cellSpan += spanIndex; cellStart += 0; cellEnd += 0
                cellAdvance += span.spaceWidth; cellKerning += 0f; cellKind += CELL_TEXT
                return@forEachIndexed
            }

            var charIndex = 0
            var previousCodePoint = -1

            while (charIndex < span.text.length) {
                val codePoint = span.text.codePointAt(charIndex)
                val charCount = Character.charCount(codePoint)

                cellSpan += spanIndex
                cellStart += charIndex
                cellEnd += charIndex + charCount

                if (codePoint == '\n'.code) {
                    cellAdvance += 0f; cellKerning += 0f; cellKind += CELL_NEWLINE
                    previousCodePoint = -1
                } else {
                    cellAdvance += span.font[codePoint].advance
                    cellKerning += if (previousCodePoint != -1) span.font.getKerning(previousCodePoint, codePoint) else 0f
                    cellKind += if (codePoint == ' '.code) CELL_SPACE else CELL_TEXT
                    previousCodePoint = codePoint
                }

                charIndex += charCount
            }
        }

        val cellCount = cellSpan.size
        val maxWidth = if (wrapText && width is Dimension.ConstraintDriven) contentConstraints.availableWidth else Float.POSITIVE_INFINITY

        // Breaks into lines of cells, [start, end) each, at spaces when possible.
        val lineRanges = mutableListOf<IntArray>()
        var lineStart = 0
        var lineWidth = 0f
        var lastSpace = -1
        var index = 0

        while (index < cellCount) {
            if (cellKind[index] == CELL_NEWLINE) {
                lineRanges += intArrayOf(lineStart, index)
                lineStart = index + 1
                lineWidth = 0f
                lastSpace = -1
                index++
                continue
            }

            if (cellKind[index] == CELL_SPACE) {
                lastSpace = index
            }

            val kerning = if (index > lineStart && cellSpan[index] == cellSpan[index - 1]) cellKerning[index] else 0f
            val newWidth = lineWidth + kerning + cellAdvance[index]

            if (newWidth > maxWidth && index > lineStart) {
                if (lastSpace > lineStart) {
                    // The space itself is dropped.
                    lineRanges += intArrayOf(lineStart, lastSpace)
                    lineStart = lastSpace + 1
                } else {
                    lineRanges += intArrayOf(lineStart, index)
                    lineStart = index
                }
                index = lineStart
                lineWidth = 0f
                lastSpace = -1
                continue
            }

            lineWidth = newWidth
            index++
        }
        lineRanges += intArrayOf(lineStart, cellCount)

        fun buildLine(start: Int, end: Int, ellipsis: Boolean): Line {
            val fragments = mutableListOf<Fragment>()
            var x = 0f
            var cell = start

            while (cell < end) {
                val spanIndex = cellSpan[cell]
                val span = spans[spanIndex]
                val fragmentStart = cell
                var width = 0f

                while (cell < end && cellSpan[cell] == spanIndex) {
                    if (cell > fragmentStart) width += cellKerning[cell]
                    width += cellAdvance[cell]
                    cell++
                }

                val text = if (span.isSpace) "" else span.text.substring(cellStart[fragmentStart], cellEnd[cell - 1])
                fragments += Fragment(text, span.font, span.color, span.backgroundColor, span.verticalAlign, x, width)
                x += width
            }

            if (ellipsis) {
                // Truncated lines end with an ellipsis in the style of their last text.
                val last = fragments.lastOrNull { it.text.isNotEmpty() && it.verticalAlign == SpanStyle.VerticalAlign.Baseline }
                val font = last?.font ?: baseFont
                val dots = "..."
                val dotsWidth = dots.sumOf { font[it.code].advance.toDouble() }.toFloat()

                fragments += Fragment(dots, font, last?.color, last?.backgroundColor, SpanStyle.VerticalAlign.Baseline, x, dotsWidth)
                x += dotsWidth
            }

            // Text shares a baseline, so the line fits the tallest ascent and descent. Centered
            // spans only need the line to be as tall as them.
            var ascent = 0f
            var descent = 0f
            var centeredHeight = 0f
            var hasBaselineText = false

            for (fragment in fragments) {
                if (fragment.text.isEmpty()) continue
                val metrics = fragment.font.metrics

                if (fragment.verticalAlign == SpanStyle.VerticalAlign.Center) {
                    centeredHeight = max(centeredHeight, metrics.lineHeight)
                } else {
                    hasBaselineText = true
                    ascent = max(ascent, -metrics.ascent)
                    descent = max(descent, metrics.lineHeight + metrics.ascent)
                }
            }

            if (!hasBaselineText) {
                ascent = -baseFont.metrics.ascent
                descent = baseFont.metrics.lineHeight + baseFont.metrics.ascent
            }

            val height = max(ascent + descent, centeredHeight)
            // Centered spans taller than the text push it down to stay centered too.
            val textOffset = (height - (ascent + descent)) / 2f

            return Line(fragments, x, height, ascent + textOffset)
        }

        val truncated = lineRanges.size > maxLines
        val visibleRanges = if (truncated) lineRanges.take(maxLines) else lineRanges

        lines = visibleRanges.mapIndexed { i, range ->
            var end = range[1]
            val ellipsis = truncated && i == visibleRanges.lastIndex

            // Trailing spaces before the ellipsis are dropped.
            if (ellipsis) {
                while (end > range[0] && cellKind[end - 1] == CELL_SPACE) end--
            }
            buildLine(range[0], end, ellipsis)
        }

        lineGap = baseFont.metrics.lineGap
        textHeight = lines.sumOf { it.height.toDouble() }.toFloat() + lineGap * (lines.size - 1).coerceAtLeast(0)

        val measuredSize = Vec2(
            x = lines.maxOfOrNull { it.width } ?: 0f,
            y = textHeight
        )

        return parentConstraints.constrain(measuredSize.expand(padding.toVec4()))
    }

    //endregion

    //region Draw & Update

    override fun onDraw(renderer: GraphicsRenderer) {

        if (lines.isEmpty()) {
            return
        }

        val contentLeft = paddingLeft.toPixels()
        val contentTop = paddingTop.toPixels()
        val contentWidth = bounds.width - contentLeft - paddingRight.toPixels()
        val contentHeight = bounds.height - contentTop - paddingBottom.toPixels()

        val offset = if (autoScrollAxes != Axis.None) {
            val scrollableSize = when (autoScrollAxes) {
                Axis.X -> (lines.maxOfOrNull { it.width } ?: 0f) - contentWidth
                Axis.Y -> textHeight - contentHeight
                else -> 0f
            }
            autoScrollDistance = scrollableSize.coerceAtLeast(0f)
            autoScrollDistance * scrollProgress
        } else {
            0f
        }

        val offsetX = if (autoScrollAxes == Axis.X) offset else 0f
        val offsetY = if (autoScrollAxes == Axis.Y) offset else 0f

        var lineY = contentTop + (contentHeight - textHeight) * alignment.y - offsetY

        for (line in lines) {
            val lineX = contentLeft + (contentWidth - line.width) * alignment.x - offsetX

            for (fragment in line.fragments) {
                val x = lineX + fragment.x

                if (fragment.backgroundColor != null) {
                    renderer.drawRect(x, lineY, fragment.width, line.height, fragment.backgroundColor)
                }

                if (fragment.text.isEmpty()) {
                    continue
                }

                val metrics = fragment.font.metrics

                val y = when (fragment.verticalAlign) {
                    SpanStyle.VerticalAlign.Center -> lineY + (line.height - metrics.lineHeight) / 2f
                    // The renderer places the baseline at y minus the ascent (which is negative).
                    SpanStyle.VerticalAlign.Baseline -> lineY + line.ascent + metrics.ascent
                }

                renderer.drawText(
                    color = fragment.color ?: contentColor,
                    font = fragment.font,
                    text = fragment.text,
                    x = x,
                    y = y
                )
            }

            lineY += line.height + lineGap
        }
    }

    override fun onUpdate(clock: FrameClock) {

        // Waits at the start, scrolls to the end, waits there and starts over.
        if (autoScrollAxes != Axis.None && autoScrollDistance > 0f) {
            when {
                timeoutProgress < 1f -> {
                    timeoutProgress += clock.elapsedFrameTime / autoScrollTimeout
                }
                scrollProgress < 1f -> {
                    scrollProgress = (scrollProgress + autoScrollSpeed * clock.elapsedFrameTime / autoScrollDistance).coerceAtMost(1f)
                    if (scrollProgress == 1f) {
                        timeoutProgress = 0f
                    }
                }
                else -> {
                    scrollProgress = 0f
                    timeoutProgress = 0f
                }
            }
        }

        super.onUpdate(clock)
    }

    //endregion


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

private const val CELL_TEXT = 0
private const val CELL_SPACE = 1
private const val CELL_NEWLINE = 2

fun faRegularIcon(code: Int) = UIText.IconLookup(code, BuiltIn.Fonts.FontAwesomeIconRegular)
fun faSolidIcon(code: Int) = UIText.IconLookup(code, BuiltIn.Fonts.FontAwesomeIconSolid)
fun faBrandsIcon(code: Int) = UIText.IconLookup(code, BuiltIn.Fonts.FontAwesomeIconBrands)

