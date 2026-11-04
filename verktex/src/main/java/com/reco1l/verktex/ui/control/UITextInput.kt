package com.reco1l.verktex.ui.control

import androidx.core.text.isDigitsOnly
import com.reco1l.verktex.*
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.math.interpolateColor
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.IFocusable
import com.reco1l.verktex.ui.ITextInput
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText
import kotlin.math.min

inline fun UIContainer.textInput(builder: UITextInput.() -> Unit): UITextInput {
    return UITextInput().apply(builder).also(::plusAssign)
}

/**
 * A single line text field. The text is typed with the platform's IME, which is attached while the
 * field is focused.
 */
open class UITextInput(initialValue: String = "") : UIControl<String>(initialValue), IFocusable {

    private val leadingIconComponent = UIIcon().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        isVisible = false
    }

    private val trailingIconComponent = UIIcon().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        isVisible = false
    }

    // Text scrolled past the start is hidden here, so it's never drawn below the icons.
    private val textArea = UIContainer().apply {
        width = fill()
        height = fill()
        clipping = ClipMode.Bounds
    }

    private val row = UILinearContainer().apply {
        width = fill()
        height = fill()
    }

    private val placeholderText = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        wrapText = false
        maxLines = 1
    }

    private val textComponent = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        wrapText = false
    }

    private val caret = UIBox().apply {
        width = 2f.dip
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
    }


    /**
     * The placeholder text displayed while the field is empty.
     */
    var placeholder by placeholderText::text

    /**
     * The icon displayed before the text. If set to null, no leading icon is displayed.
     */
    var leadingIcon: UIText.IconLookup? = null
        set(value) {
            if (field == value) return
            field = value
            leadingIconComponent.setIcon(value)
        }

    /**
     * The icon displayed after the text. If set to null, no trailing icon is displayed.
     */
    var trailingIcon: UIText.IconLookup? = null
        set(value) {
            if (field == value) return
            field = value
            trailingIconComponent.setIcon(value)
        }

    /**
     * The maximum number of characters allowed in the field. If set to 0, there is no limit.
     */
    var maxCharacters = 0

    /**
     * Whether the IME action (e.g. the enter key) confirms the input, calling [onConfirm] and
     * blurring the field.
     */
    var confirmOnEnter = true

    /**
     * Whether the text is a password, displayed masked and hidden from the IME's suggestions.
     */
    var isPassword = false
        set(value) {
            if (field != value) {
                field = value
                updateVisuals()
            }
        }

    /**
     * A callback that is invoked when the input is confirmed.
     */
    var onConfirm: (() -> Unit)? = null

    /**
     * Whether this field is currently focused and receiving text.
     */
    val isFocused
        get() = Verktex.focusedComponent === this

    override val canFocus = true


    private var isPressed = false
    private var caretTime = 0f
    private var errorColor: Color4? = null


    // A separate object since the value property is inherited from UIControl.
    private val imeTarget = object : ITextInput {

        override var value: String
            get() = this@UITextInput.value
            set(value) { this@UITextInput.value = value }

        override val maxCharacters
            get() = this@UITextInput.maxCharacters

        override val isPassword
            get() = this@UITextInput.isPassword

        override fun isCharacterAllowed(char: String) = this@UITextInput.isCharacterAllowed(char)

        override fun isTextValid(text: String) = this@UITextInput.isTextValid(text)

        override fun notifyInputError() = this@UITextInput.notifyInputError()

        override fun onImeAction() {
            if (confirmOnEnter) {
                onConfirm?.invoke()
                blur()
            }
        }
    }


    init {
        style = Style { theme ->
            height = theme.controlStyle.minimumHeight
            padding = horizontalPadding(theme.gap.md)
            radius = theme.radius.md
            backgroundColor = theme.palette.controlInactiveBackground
            border = Border(1.5f.dip, borderTargetColor(theme))

            textComponent.color = Color4.White
            placeholderText.color = Color4.White.copy(alpha = 0.5f)
            caret.backgroundColor = theme.palette.accent
            leadingIconComponent.color = theme.palette.accent
            trailingIconComponent.color = theme.palette.accent
            row.spacing = theme.controlStyle.iconSpacing
        }

        textArea += placeholderText
        textArea += textComponent
        textArea += caret

        row += leadingIconComponent
        row += textArea
        row += trailingIconComponent
        +row

        updateVisuals()
    }


    //region Focus

    /**
     * Focuses this field, showing the soft keyboard.
     */
    fun focus() = Verktex.requestFocus(this)

    /**
     * Removes the focus from this field, hiding the soft keyboard.
     */
    fun blur() {
        if (isFocused) Verktex.clearFocus()
    }

    override fun onFocusGained() {
        caretTime = 0f
        Verktex.host.attachTextInput(imeTarget)
    }

    override fun onFocusLost() {
        Verktex.host.detachTextInput()
    }

    //endregion

    //region Update

    private fun borderTargetColor(theme: Theme) =
        if (isFocused) theme.palette.accent else theme.palette.accent.copy(alpha = 0f)

    override fun onUpdate(clock: FrameClock) {

        // Errors flash the border red, then it goes back to its normal color.
        val targetBorderColor = errorColor ?: borderTargetColor(resolvedTheme)
        errorColor = null
        val border = border
        if (border != null) {
            val color = clock.interpolateColor(0.2f, border.color, targetBorderColor)
            this.border = border.copy(color = color, colorBottom = color)
        }

        val contentWidth = textArea.bounds.width
        val textWidth = if (value.isEmpty()) 0f else textComponent.measuredSize.x
        val caretWidth = caret.measuredSize.x

        // Keeps the end of the text (where the caret is) visible when it overflows.
        val textX = min(0f, contentWidth - textWidth - caretWidth)
        textComponent.x = textX.px

        caretTime += clock.elapsedFrameTime
        caret.isVisible = isFocused && caretTime % 1f < 0.5f
        caret.x = (textX + textWidth).px
        caret.height = textComponent.measuredSize.y.px

        super.onUpdate(clock)
    }

    //endregion

    //region Input

    override fun onInputEvent(event: InputEvent): Boolean {
        if (event !is PointerEvent) {
            return super.onInputEvent(event)
        }

        return when (event.action) {
            PointerEvent.Action.Down -> {
                isPressed = true
                true
            }
            PointerEvent.Action.Up -> {
                if (!isPressed) return false
                isPressed = false
                if (containsScenePoint(event.x, event.y)) {
                    focus()
                }
                true
            }
            PointerEvent.Action.Unknown, PointerEvent.Action.Cancel -> {
                isPressed = false
                false
            }
            else -> isPressed
        }
    }

    //endregion

    //region Validation

    /**
     * Checks if a character is allowed to be typed in this field.
     */
    open fun isCharacterAllowed(char: String) = true

    /**
     * Checks whether a text is valid as a [value] for this field.
     */
    open fun isTextValid(text: String) = true

    /**
     * Called when the last typed text was rejected.
     */
    open fun notifyInputError() {
        errorColor = ERROR_COLOR
        border = border?.copy(color = ERROR_COLOR, colorBottom = ERROR_COLOR)
    }

    //endregion

    private fun UIIcon.setIcon(icon: UIText.IconLookup?) {
        isVisible = icon != null
        if (icon != null) {
            iconCode = icon.code
            iconFamily = icon.family
        }
    }

    private fun updateVisuals() {
        textComponent.text = if (isPassword) "•".repeat(value.codePointCount(0, value.length)) else value
        placeholderText.isVisible = value.isEmpty()
    }

    override fun onValueChanged() {
        super.onValueChanged()
        updateVisuals()
    }


    companion object {
        private val ERROR_COLOR = Color4(0xFFF38BA8)
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

        return value == null || ((minValue == null || value >= minValue) && (maxValue == null || value <= maxValue))
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
        super.isTextValid(text) && (text.isEmpty() || text == "-" || text.toIntOrNull() != null)

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
        super.isTextValid(text) && (text.isEmpty() || text == "-" || text.toFloatOrNull() != null)

    override fun convertValue(value: String) = value.toFloatOrNull()
}
