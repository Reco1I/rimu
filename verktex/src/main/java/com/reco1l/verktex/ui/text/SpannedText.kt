package com.reco1l.verktex.ui.text

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.fonts.FontFamily
import com.reco1l.verktex.fonts.Fonts
import com.reco1l.verktex.ui.Theme

/**
 * How a [Span] looks. Every null property is taken from the [UIText] showing it, so styles can be
 * nested, see [plus].
 */
data class SpanStyle(

    /**
     * The color of the text.
     */
    val color: Color4? = null,

    /**
     * The color drawn behind the text, covering the height of its line.
     */
    val backgroundColor: Color4? = null,

    /**
     * The font family, e.g. an icon font.
     */
    val fontFamily: FontFamily? = null,

    /**
     * The font size.
     */
    val fontSize: Dimension.Fixed? = null,

    /**
     * The font weight.
     */
    val fontWeight: Font.Weight? = null,

    /**
     * The font style.
     */
    val fontStyle: Font.Style? = null,

    /**
     * How the text is placed vertically in its line.
     */
    val verticalAlign: VerticalAlign? = null,
) {

    /**
     * Returns this style with the properties set in [other] on top.
     */
    operator fun plus(other: SpanStyle) = SpanStyle(
        color = other.color ?: color,
        backgroundColor = other.backgroundColor ?: backgroundColor,
        fontFamily = other.fontFamily ?: fontFamily,
        fontSize = other.fontSize ?: fontSize,
        fontWeight = other.fontWeight ?: fontWeight,
        fontStyle = other.fontStyle ?: fontStyle,
        verticalAlign = other.verticalAlign ?: verticalAlign,
    )

    /**
     * How a span is placed vertically in a line containing bigger text.
     */
    enum class VerticalAlign {

        /**
         * On the baseline shared with the rest of the line, as text usually is.
         */
        Baseline,

        /**
         * Centered in the line, as icons usually are.
         */
        Center
    }

    companion object {
        val Default = SpanStyle()
    }
}

/**
 * A piece of a [SpannedText] with a single [style].
 */
data class Span(

    /**
     * The text of the span.
     */
    val text: String,

    /**
     * How the text looks.
     */
    val style: SpanStyle = SpanStyle.Default,

    /**
     * A fixed width for the span, which makes it an empty space of that width instead of text.
     */
    val spaceWidth: Dimension.Fixed? = null,
)

/**
 * A text made of [Span]s with different styles, shown with [UIText.spannedText]. It is usually built
 * with [buildSpannedText] or [UIText.spans].
 */
data class SpannedText(val spans: List<Span>) {

    /**
     * The text of every span, without styles.
     */
    val text: String by lazy { spans.joinToString("") { it.text } }

    /**
     * Whether it is a text without styles nor spaces, as set through [UIText.text].
     */
    val isPlain: Boolean by lazy { spans.all { it.style == SpanStyle.Default && it.spaceWidth == null } }


    /**
     * Returns the spans of this text followed by the ones of [other].
     */
    operator fun plus(other: SpannedText) = SpannedText(spans + other.spans)


    companion object {

        val Empty = SpannedText(emptyList())

        /**
         * Returns a text without styles.
         */
        fun of(text: String) = if (text.isEmpty()) Empty else SpannedText(listOf(Span(text)))
    }
}


/**
 * Builds a [SpannedText], every function taking a block styles what is added inside it, on top of
 * the style of the enclosing blocks.
 *
 * ```
 * buildSpannedText {
 *     icon(FAIcon.Star)
 *     space(4f.dip)
 *     bold { +"5.00" }
 *     color(Color4.Red) { +" Hard" }
 * }
 * ```
 */
class SpannedTextBuilder {

    private val spans = mutableListOf<Span>()

    /**
     * The style of what is added now, merged from the enclosing blocks.
     */
    @PublishedApi
    internal var currentStyle = SpanStyle.Default


    //region Content

    /**
     * Adds [text] with the current style.
     */
    fun append(text: String) {
        if (text.isNotEmpty()) {
            spans += Span(text, currentStyle)
        }
    }

    /**
     * Adds the spans of [text], their styles on top of the current one.
     */
    fun append(text: SpannedText) {
        text.spans.forEach { spans += it.copy(style = currentStyle + it.style) }
    }

    /**
     * Adds this text with the current style.
     */
    operator fun String.unaryPlus() = append(this)

    /**
     * Adds the spans of this text, their styles on top of the current one.
     */
    operator fun SpannedText.unaryPlus() = append(this)

    /**
     * Adds an icon, centered in the line and as big as the current font size.
     */
    fun icon(icon: UIText.IconLookup) {
        style(SpanStyle(fontFamily = icon.family, verticalAlign = SpanStyle.VerticalAlign.Center)) {
            append(String(Character.toChars(icon.code)))
        }
    }

    /**
     * Adds an icon of the given [family], Font Awesome solid icons by default (see
     * [com.reco1l.verktex.ui.FAIcon]).
     */
    fun icon(code: Int, family: FontFamily = BuiltIn.Fonts.FontAwesomeIconSolid) = icon(UIText.IconLookup(code, family))

    /**
     * Adds an empty space of the given width, e.g. between an icon and the text.
     */
    fun space(width: Dimension.Fixed) {
        spans += Span("", currentStyle, width)
    }

    //endregion

    //region Styles

    /**
     * Adds what [block] adds with [style] on top of the current style.
     */
    inline fun style(style: SpanStyle, block: SpannedTextBuilder.() -> Unit) {
        val previousStyle = currentStyle
        currentStyle = previousStyle + style
        try {
            block()
        } finally {
            currentStyle = previousStyle
        }
    }

    inline fun color(color: Color4, block: SpannedTextBuilder.() -> Unit) = style(SpanStyle(color = color), block)

    inline fun background(color: Color4, block: SpannedTextBuilder.() -> Unit) = style(SpanStyle(backgroundColor = color), block)

    inline fun font(family: FontFamily, block: SpannedTextBuilder.() -> Unit) = style(SpanStyle(fontFamily = family), block)

    /**
     * Adds what [block] adds with the font of [settings], e.g. a typography of the theme. Its color
     * is left out, as it always has one.
     */
    inline fun font(settings: Theme.FontSettings, block: SpannedTextBuilder.() -> Unit) = style(
        SpanStyle(
            fontFamily = Fonts[settings.family],
            fontSize = settings.size,
            fontWeight = settings.weight,
            fontStyle = settings.style,
        ),
        block
    )

    inline fun size(size: Dimension.Fixed, block: SpannedTextBuilder.() -> Unit) = style(SpanStyle(fontSize = size), block)

    inline fun weight(weight: Font.Weight, block: SpannedTextBuilder.() -> Unit) = style(SpanStyle(fontWeight = weight), block)

    inline fun bold(block: SpannedTextBuilder.() -> Unit) = weight(Font.Weight.Bold, block)

    inline fun italic(block: SpannedTextBuilder.() -> Unit) = style(SpanStyle(fontStyle = Font.Style.Italic), block)

    //endregion


    fun build() = SpannedText(spans.toList())
}

/**
 * Builds a [SpannedText], see [SpannedTextBuilder].
 */
inline fun buildSpannedText(block: SpannedTextBuilder.() -> Unit) = SpannedTextBuilder().apply(block).build()

/**
 * Sets the content of this text to the one built by [block], see [SpannedTextBuilder].
 */
inline fun UIText.spans(block: SpannedTextBuilder.() -> Unit) {
    spannedText = buildSpannedText(block)
}
