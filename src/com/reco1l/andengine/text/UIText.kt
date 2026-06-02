package com.reco1l.andengine.text

import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Fonts
import com.reco1l.andengine.theme.Size
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.*
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.*
import kotlin.math.*

/**
 * A text entity that can be displayed on the screen.
 */
open class UIText : UIBufferedComponent() {

    /**
     * The text to be displayed
     */
    var text: String = ""
        set(value) {
            if (field != value) {
                field = value
                currentLength = value.codePointCount(0, value.length)
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The font used to render the text.
     */
    var font: Font? = null
        private set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The font family to use for this text.
     */
    var fontFamily = Fonts.NunitoMedium
        set(value) {
            if (field != value) {
                field = value
                fontSettingsChanged = true
            }
        }

    /**
     * The font size to use for this text.
     */
    var fontSize = FontSize.SM
        set(value) {
            if (field != value) {
                field = value
                fontSettingsChanged = true
            }
        }

    /**
     * Called when the font settings (font size or family) change.
     */
    var onFontSettingsChange: () -> Unit = {
        val oldFont = font

        if (oldFont != null) {
            UIEngine.current.resources.unsubscribeFromFont(oldFont, this)
        }

        val newFont = UIEngine.current.resources.getOrStoreFont(fontSize, fontFamily)
        font = newFont
        UIEngine.current.resources.subscribeToFont(newFont, this)

        invalidate(InvalidationFlag.Content)
    }

    /**
     * The alignment of the text.
     */
    var alignment = Anchor.TopLeft

    /**
     * Which axes to scroll the text automatically when it overflows.
     */
    var autoScrollAxes = Axes.X
        set(value) {
            if (field != value) {
                field = value
                clipToBounds = value != Axes.None
            }
        }

    /**
     * The speed of the auto scroll animation in pixels per second.
     */
    var autoScrollSpeed = 15f

    /**
     * The time to wait before re-starting the auto scroll animation in seconds
     */
    var autoScrollTimeout = 3f

    /**
     * Whether to wrap text that exceeds the width of the component.
     */
    var wrapText = false
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }


    protected open val textViewportWidth: Float
        get() = innerWidth

    protected open val textViewportHeight: Float
        get() = innerHeight

    protected open val textViewportX: Float
        get() = padding.left

    protected open val textViewportY: Float
        get() = padding.top


    private var currentLength = 0
    private var scrollX = 0f
    private var scrollY = 0f
    private var scrollXTimeoutElapsed = 0f
    private var scrollYTimeoutElapsed = 0f
    private var fontSettingsChanged = true

    private var lines: List<String>? = null
    private var linesWidth: IntArray? = null


    init {
        width = Size.Auto
        height = Size.Auto

        clipToBounds = true

        style = {
            fontSize = FontSize.SM
        }
    }


    override fun onContentChanged() {

        val text = text
        val font = font

        if (font == null) {
            lines = emptyList()
            linesWidth = IntArray(0)
            contentWidth = 0f
            contentHeight = 0f
            return
        }

        val originalLines = text.split('\n')

        if (wrapText && width > 0f && rawWidth != Size.Auto) {
            val wrappedLines = mutableListOf<String>()
            val wrappedLinesWidth = mutableListOf<Int>()

            originalLines.forEach { originalLine ->
                wrapLine(originalLine, font, width.toInt(), wrappedLines, wrappedLinesWidth)
            }

            lines = wrappedLines
            linesWidth = wrappedLinesWidth.toIntArray()
        } else {
            lines = originalLines

            linesWidth = IntArray(lines!!.size) { i ->
                val line = lines!![i]
                var width = 0
                var charIndex = 0

                while (charIndex < line.length) {
                    val codePoint = line.codePointAt(charIndex)
                    val charCount = Character.charCount(codePoint)

                    val characterString = line.substring(charIndex, charIndex + charCount)

                    width += font.getLetter(characterString).mAdvance
                    charIndex += charCount
                }
                width
            }
        }

        contentWidth = if (linesWidth!!.isNotEmpty()) linesWidth!!.max().toFloat() else 0f
        contentHeight = (lines!!.size * font.lineHeight + (lines!!.size - 1) * font.lineGap).toFloat()
    }

    override fun onSizeChanged() {
        if (wrapText) {
            invalidate(InvalidationFlag.Content)
        }
        super.onSizeChanged()
    }

    private fun wrapLine(line: String, font: Font, maxWidth: Int, outputLines: MutableList<String>, outputWidths: MutableList<Int>) {
        if (line.isEmpty()) {
            outputLines.add("")
            outputWidths.add(0)
            return
        }

        var currentLineStart = 0
        var currentWidth = 0
        var lastSpaceIndex = -1
        var lastSpaceWidth = 0
        var charIndex = 0

        while (charIndex < line.length) {
            val codePoint = line.codePointAt(charIndex)
            val charCount = Character.charCount(codePoint)
            val characterString = line.substring(charIndex, charIndex + charCount)

            val letterAdvance = font.getLetter(characterString).mAdvance
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
                    currentWidth = 0
                    lastSpaceIndex = -1
                    lastSpaceWidth = 0
                    continue
                } else {
                    outputLines.add(line.substring(currentLineStart, charIndex))
                    outputWidths.add(currentWidth)
                    currentLineStart = charIndex
                    currentWidth = 0
                    lastSpaceIndex = -1
                    lastSpaceWidth = 0
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

    override fun doDraw(gl: GL10, camera: Camera) {
        super.doDraw(gl, camera)

        val font = font
        val lines = lines
        val linesWidth = linesWidth

        if (font == null || lines == null || linesWidth == null) {
            return
        }

        UIRenderer.setState(gl, texture = font.texture as BitmapTextureAtlas)

        TextRenderer.renderLines(
            lines = lines,
            linesWidth = linesWidth,
            font = font,
            viewportX = textViewportX,
            viewportY = textViewportY,
            viewportWidth = textViewportWidth,
            viewportHeight = textViewportHeight,
            alignment = alignment
        )
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        if (fontSettingsChanged) {
            fontSettingsChanged = false
            onFontSettingsChange()
        }

        val scrollTranslationX = if (autoScrollAxes.isHorizontal) scrollX else 0f
        val scrollTranslationY = if (autoScrollAxes.isVertical) scrollY else 0f

        if (scrollTranslationX != 0f || scrollTranslationY != 0f) {
            //TransformationStack.peek()?.postTranslate(-scrollTranslationX, -scrollTranslationY)
        }

        super.onManagedDraw(gl, camera)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (autoScrollAxes != Axes.None) {

            fun processAutoScroll(currentScroll: Float, maxScroll: Float, currentTimeout: Float) : Pair<Float, Float> {

                if (currentScroll == 0f && currentTimeout < autoScrollTimeout) {
                    return currentScroll to currentTimeout + deltaTimeSec
                }

                if (currentScroll >= maxScroll) {
                    if (currentTimeout > autoScrollTimeout) {
                        return 0f to 0f
                    }
                    return currentScroll to currentTimeout + deltaTimeSec
                }

                return min(currentScroll + autoScrollSpeed * deltaTimeSec, maxScroll) to 0f
            }

            val maxScrollX = contentWidth - width

            if (autoScrollAxes.isHorizontal && maxScrollX > 0) {
                val (x, timeout) = processAutoScroll(scrollX, maxScrollX, scrollXTimeoutElapsed)

                scrollX = x
                scrollXTimeoutElapsed = timeout
            } else {
                scrollX = 0f
            }

            val maxScrollY = contentHeight - height

            if (autoScrollAxes.isVertical && maxScrollY > 0) {
                val (y, timeout) = processAutoScroll(scrollY, maxScrollY, scrollYTimeoutElapsed)

                scrollY = y
                scrollYTimeoutElapsed = timeout
            } else {
                scrollY = 0f
            }

        } else {
            scrollX = 0f
            scrollY = 0f
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    fun finalize() {
        val font = font ?: return
        UIEngine.current.resources.unsubscribeFromFont(font, this)
    }

}


/**
 * A compound text entity that can be displayed with leading and trailing icons.
 */
open class CompoundText : UIText() {

    /**
     * The spacing between the icons and the text.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The leading icon.
     */
    var leadingIcon: UIComponent? = null
        set(value) {
            if (field != value) {
                field = value
                value?.setParent(this)
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The trailing icon.
     */
    var trailingIcon: UIComponent? = null
        set(value) {
            if (field != value) {
                field = value
                value?.setParent(this)
                invalidate(InvalidationFlag.Content)
            }
        }

    /**
     * The width of the text without icons.
     */
    var textWidth = 0f
        private set


    override val textViewportX: Float
        get() = padding.left + (leadingIcon?.let { it.width + spacing } ?: 0f)

    override val textViewportWidth: Float
        get() = innerWidth - (leadingIcon?.let { it.width + spacing } ?: 0f) - (trailingIcon?.let { it.width + spacing } ?: 0f)


    override fun onContentChanged() {
        super.onContentChanged()
        textWidth = contentWidth

        val leadingIcon = leadingIcon
        val trailingIcon = trailingIcon

        var totalWidth = 0f

        if (leadingIcon != null) {
            leadingIcon.anchor = Anchor.CenterLeft
            leadingIcon.origin = Anchor.CenterLeft
            leadingIcon.setSize(fontSize, fontSize)

            totalWidth += leadingIcon.width + spacing
        }

        totalWidth += textWidth

        if (trailingIcon != null) {
            trailingIcon.anchor = Anchor.CenterLeft
            trailingIcon.origin = Anchor.CenterLeft
            trailingIcon.x = totalWidth + spacing
            trailingIcon.setSize(fontSize, fontSize)

            totalWidth += spacing + trailingIcon.width
        }

        contentWidth = totalWidth

        // Move trailing icon to the end.
        if (width > intrinsicWidth) {
            trailingIcon?.x = innerWidth - trailingIcon.width
        }
    }

    override fun onDrawChildren(gl: GL10, camera: Camera) {
        leadingIcon?.onDraw(gl, camera)
        trailingIcon?.onDraw(gl, camera)
    }
}