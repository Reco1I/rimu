package com.reco1l.verktex.ui.control

import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.text.isDigitsOnly
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.reco1l.verktex.*
import com.reco1l.verktex.ui.text.*
import com.reco1l.verktex.ui.IFocusable
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.time.TickingClock
import kotlin.math.*
import kotlin.text.substring

open class UITextInput(initialValue: String) : UIControl<String>(initialValue), IFocusable {

    private val placeholderEntity = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        wrapText = true
    }

    private val textComponent = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        wrapText = true
    }

    private val caret = UIBox().apply {
        width = 2f.px
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        isVisible = false
    }

    private val selectionBox = UIBox().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }


    /**
     * Whether to confirm the input on enter key press.
     */
    var confirmOnEnter = true

    /**
     * A callback that is invoked when the input is confirmed by pressing the enter key.
     */
    var onConfirm: (() -> Unit)? = null

    /**
     * The maximum number of characters allowed in the text field. If set to 0, there is no limit.
     */
    var maxCharacters = 0

    /**
     * The placeholder text displayed when the input field is empty.
     */
    var placeholder by placeholderEntity::text


    private var cursorFading = false
    private var elapsedTimeSec = 0f
    private var targetSelectionBoxWidth = 0f
    private var targetCursorPosition = 0f

    private var cursorPositions = intArrayOf(0)


    init {
        style = Style { theme ->
            height = 40f.dip
            padding = Padding(8f.dip)

            backgroundColor = it.accentColor * 0.25f
            borderColor = if (isFocused) it.accentColor else it.accentColor * 0.4f
            borderWidth = 0.2f.srem
            radius = Radius.LG
            textComponent.color = it.accentColor
            placeholderEntity.color = it.accentColor * 0.6f
        }

        +placeholderEntity
        +textComponent
        +caret
        +selectionBox

        updateVisuals()
        style(Theme.current)
    }

    override fun onFocusGained() {
        setKeyboardVisibility(true)
        caret.isVisible = true
        borderColor = Theme.current.accentColor
    }

    override fun onFocusLost() {
        setKeyboardVisibility(false)
        caret.isVisible = false
        borderColor = Theme.current.accentColor * 0.4f
    }

    private fun setKeyboardVisibility(value: Boolean) = mainThread {
        val root = Engine.current.context.window.decorView as ViewGroup

        val inputMethodManager = Engine.current.context.getSystemService<InputMethodManager>()!!

        val windowInsets = ViewCompat.getRootWindowInsets(Engine.current.context.window.decorView)
        val keyboardHeight = windowInsets!!.getInsets(WindowInsetsCompat.Type.ime()).bottom

        // Tricky prevention from opening the keyboard while it should be closed and vice versa.
        if (value && keyboardHeight > 0) {
            return@mainThread
        }

        internalEditText.apply {
            if (value) {
                if (parent == null) {
                    root.addView(internalEditText)
                }

                requestFocus()
                setText(this@UITextInput.value, TextView.BufferType.EDITABLE)

                post {
                    inputMethodManager.showSoftInput(internalEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            } else {
                inputMethodManager.hideSoftInputFromWindow(internalEditText.windowToken, 0)
                clearFocus()

                if (parent != null) {
                    root.removeView(this)
                }
            }
        }
    }

    override fun onUpdate(clock: TickingClock) {

        selectionBox.width = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.3f), selectionBox.width, targetSelectionBoxWidth, 0f, 0.3f, Easing.OutExpo)
        caret.x = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.3f), caret.x, textComponent.x + targetCursorPosition, 0f, 0.3f, Easing.OutExpo)
        selectionBox.x = caret.x

        textComponent.maxWidth = innerWidth
        placeholderEntity.maxWidth = innerWidth

        selectionBox.width = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.3f), selectionBox.width, targetSelectionBoxWidth, 0f, 0.3f, Easing.OutExpo)
        caret.x = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.3f), caret.x, textComponent.x + targetCursorPosition, 0f, 0.3f, Easing.OutExpo)
        selectionBox.x = caret.x

        if (caret.isVisible) {
            targetCursorPosition = cursorPositions.getOrNull(internalEditText.selectionEnd)?.toFloat() ?: (textComponent.x + textComponent.width)
            caret.y = textComponent.y
            caret.height = textComponent.height

            if (elapsedTimeSec >= 0.5f) {
                cursorFading = !cursorFading
                elapsedTimeSec = 0f
            }

            val alphaFactor = elapsedTimeSec / 0.5f
            caret.alpha = if (cursorFading) 1f - alphaFactor else alphaFactor

            elapsedTimeSec += deltaTimeSec

            if (internalEditText.selectionStart != internalEditText.selectionEnd) {
                val startX = cursorPositions.getOrNull(internalEditText.selectionStart)?.toFloat() ?: 0f
                val endX = cursorPositions.getOrNull(internalEditText.selectionEnd)?.toFloat() ?: 0f

                selectionBox.origin = if (startX >= endX) Anchor.CenterLeft else Anchor.CenterRight
                selectionBox.height = textComponent.height
                targetSelectionBoxWidth = abs(endX - startX)
            } else {
                targetSelectionBoxWidth = 0f
            }
        }
        super.onUpdate(clock)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionUp) {
            if (!isFocused) {
                focus()
            } else {
                val x = localX - padding.left

                // Find the closest letter position to the touch
                var distance = 0f
                var position = 0

                for (i in cursorPositions.indices) {
                    val letterX = cursorPositions[i].toFloat()
                    val newDistance = abs(letterX - x)

                    if (newDistance < distance || i == 0) {
                        distance = newDistance
                        position = i
                    }
                }

                mainThread {
                    internalEditText.setSelection(position)
                }
            }
        }
        return true
    }


    override fun onProcessValue(value: String): String {

        val codePointCount = value.codePointCount(0, value.length)
        cursorPositions = IntArray(codePointCount + 1) { cursorIndex ->

            // First position is always the beginning of the line.
            if (cursorIndex <= 0) {
                return@IntArray 0
            }

            var totalAdvance = 0
            var stringIndex = 0
            var codePointIndex = 0

            // Iterar a través de los code points hasta llegar a la posición del cursor
            while (codePointIndex < cursorIndex && stringIndex < value.length) {
                val codePoint = value.codePointAt(stringIndex)
                val charCount = Character.charCount(codePoint)

                // Extraer el carácter completo como String (incluyendo pares sustitutos)
                val characterString = value.substring(stringIndex, stringIndex + charCount)
                totalAdvance += font?.getLetter(characterString)?.mAdvance ?: 0

                stringIndex += charCount
                codePointIndex++
            }

            return@IntArray totalAdvance
        }

        return super.onProcessValue(value)
    }

    /**
     * Checks if a [Char] is allowed to be appended to this [UITextInput].
     *
     * @param char The [Char] to check.
     * @return `true` if [char] is allowed to be appended to this [UITextInput], `false` otherwise.
     */
    open fun isCharacterAllowed(char: String) = true

    /**
     * Checks whether a text is valid as a [value] for this [UITextInput].
     *
     * @param text The text to check.
     * @return `true` if [text] is valid as a [value] for this [UITextInput], `false` otherwise.
     */
    open fun isTextValid(text: String) = true

    internal fun notifyInputError() {
        textComponent.apply {
            clearModifiers(ModifierType.Color)
            color = Color4.Red
            colorTo(Theme.current.accentColor, 0.2f)
        }

        borderColor = Color4.Red
    }

    private fun updateVisuals() {
        textComponent.text = value
        placeholderEntity.isVisible = value.isEmpty()
    }

    override fun onValueChanged() {
        super.onValueChanged()
        updateVisuals()
    }

}

/**
 * A [UITextInput] whose [value] is constrained to a range of values.
 */
sealed class RangeConstrainedTextInput<T : Comparable<T>?>(
    initialValue: T?,

    /**
     * The minimum value that can be entered in this [RangeConstrainedTextInput].
     *
     * If set to `null`, there is no minimum value.
     */
    val minValue: T? = null,

    /**
     * The maximum value that can be entered in this [RangeConstrainedTextInput].
     *
     * If set to `null`, there is no maximum value.
     */
    val maxValue: T? = null,
) : UITextInput(initialValue?.toString() ?: "") {
    override fun isTextValid(text: String): Boolean {
        // Avoid calling convertValue whenever necessary, in case it is expensive
        if (!super.isTextValid(text)) {
            return false
        }

        val minValue = minValue
        val maxValue = maxValue

        if (minValue == null && maxValue == null) {
            return true
        }

        val value = convertValue(text)

        return value == null || (value >= minValue!! && value <= maxValue!!)
    }

    /**
     * Converts a value from a [String] to a [T].
     *
     * @param value The value to convert.
     * @return The converted value. If `null`, [value] is always considered valid.
     */
    protected abstract fun convertValue(value: String): T?
}

/**
 * A [UITextInput] that only allows [Int]s to be entered.
 */
class IntegerTextInput(
    initialValue: Int?,
    minValue: Int? = -Int.MAX_VALUE,
    maxValue: Int? = Int.MAX_VALUE,
) : RangeConstrainedTextInput<Int>(initialValue, minValue, maxValue) {
    override fun isCharacterAllowed(char: String) = super.isCharacterAllowed(char) && (char.isDigitsOnly() || char == "-")

    override fun isTextValid(text: String) =
        // Check for underflow/overflow
        super.isTextValid(text) && (text.isEmpty() || text.toIntOrNull() != null)

    override fun convertValue(value: String) = value.toIntOrNull()
}

/**
 * A [UITextInput] that only allows [Float]s to be entered.
 */
class FloatTextInput(
    initialValue: Float?,
    minValue: Float? = -Float.MAX_VALUE,
    maxValue: Float? = Float.MAX_VALUE,
) : RangeConstrainedTextInput<Float>(initialValue, minValue, maxValue) {
    override fun isCharacterAllowed(char: String) =
        super.isCharacterAllowed(char) && (char.isDigitsOnly() || char == "." || char == "-")

    override fun isTextValid(text: String) =
        // Check for underflow/overflow
        super.isTextValid(text) && (text.isEmpty() || text.toFloatOrNull() != null)

    override fun convertValue(value: String) = value.toFloatOrNull()
}
