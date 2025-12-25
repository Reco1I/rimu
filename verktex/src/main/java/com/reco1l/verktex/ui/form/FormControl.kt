package com.reco1l.verktex.ui.form

import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.control.UIControl
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.faSolidIcon

/**
 * A labeled row around a [control] used to change the value of a property, with a button to reset
 * it to its [defaultValue].
 */
@Suppress("LeakingThis", "MemberVisibilityCanBePrivate")
abstract class FormControl<V : Any, C : UIControl<V>>(initialValue: V) : UILinearContainer() {

    /**
     * The control that is used to change the value.
     */
    abstract val control: C

    /**
     * The text showing the formatted [value], if the form control has one.
     */
    open val valueText: UIText? = null

    /**
     * The text that is displayed as the label of the control.
     */
    val labelText = UIText().apply {
        // Stretched to the header row so the label is centered next to the reset button.
        height = fill()
        alignment = Anchor.CenterLeft
        style = Style { theme ->
            fontSettings = theme.typography.caption
        }
    }

    /**
     * The button that resets the value of the control to its default value.
     */
    val resetButton = UITextButton().apply {
        text = "Reset"
        leadingIcon = faSolidIcon(FAIcon.RotateLeft)
        density = UIButton.Density.Small
        variant = UIButton.Variant.Soft
        onActionUp = { value = defaultValue }
    }

    // Buttons animate their own alpha, so the reset button is faded through a holder. Hidden
    // components still receive input, so the holder ignores it while hidden.
    private val resetButtonHolder = object : UIContainer() {
        override fun onInputEvent(event: InputEvent) = isVisible && super.onInputEvent(event)
    }.apply {
        isVisible = false
        alpha = 0f
        +resetButton
    }


    /**
     * The key that is used to identify the control in a [FormContainer].
     */
    var key
        get() = control.key
        set(value) { control.key = value }

    /**
     * The value of the control.
     */
    var value
        get() = control.value
        set(value) { control.value = value }

    /**
     * The value the reset button sets the control back to.
     */
    var defaultValue = initialValue

    /**
     * The callback that is called when the value of the control changes.
     */
    var onValueChanged: ((V) -> Unit)? = null

    /**
     * The label text.
     */
    var label by labelText::text

    /**
     * Whether the control can be used. A disabled form control is faded and ignores touches.
     */
    var isEnabled = true

    /**
     * The formatter of the [valueText].
     */
    var valueFormatter: (value: V) -> String = Any::toString
        set(formatter) {
            field = formatter
            valueText?.text = formatter(value)
        }

    /**
     * Whether to show the reset button while the value is not the default one.
     */
    var showResetButton = true

    /**
     * Whether the current [value] equals to [defaultValue].
     */
    protected open val isDefault
        get() = value == defaultValue


    init {
        width = fill()
        style = Style { theme ->
            padding = Padding(theme.gap.sm)
            spacing = theme.gap.sm
        }
    }


    /**
     * Creates the header of the form control: the label and the reset button on the left, and
     * [trailing] on the right.
     */
    protected fun createHeader(trailing: UIContainer.() -> Unit = {}) = UIContainer().apply {
        width = fill()

        +UILinearContainer().apply {
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            style = Style { theme -> spacing = theme.gap.sm }

            +labelText
            +resetButtonHolder
        }

        trailing()
    }


    /**
     * Called when the value of the [control] changes.
     */
    open fun onControlValueChanged() {
        valueText?.text = valueFormatter(value)
        onValueChanged?.invoke(value)
    }

    override fun onUpdate(clock: FrameClock) {
        alpha = clock.interpolateFloat(0.2f, alpha, if (isEnabled) 1f else DISABLED_ALPHA)

        // The reset button slides in from the left while the value differs from the default.
        val showReset = showResetButton && !isDefault
        resetButtonHolder.alpha = clock.interpolateFloat(0.1f, resetButtonHolder.alpha, if (showReset) 1f else 0f)
        resetButtonHolder.translationX = (RESET_BUTTON_OFFSET * (1f - resetButtonHolder.alpha)).dip
        resetButtonHolder.isVisible = showReset || resetButtonHolder.alpha > 0f

        super.onUpdate(clock)
    }

    override fun onInputEvent(event: InputEvent): Boolean {
        // Disabled form controls swallow presses, so nothing below reacts to them either.
        if (!isEnabled) {
            return event is PointerEvent && event.action == PointerEvent.Action.Down
        }
        return super.onInputEvent(event)
    }


    companion object {
        private const val DISABLED_ALPHA = 0.25f
        private const val RESET_BUTTON_OFFSET = -10f
    }
}
