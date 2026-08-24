package com.reco1l.verktex.ui.form

import com.reco1l.verktex.*
import com.reco1l.verktex.component.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.ui.text.*
import com.reco1l.verktex.theme.FontSize
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.theme.srem
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.control.UIControl
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.framework.math.*
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.rian.andengine.modifier.ModifierType
import org.anddev.andengine.input.touch.*

/**
 * Represents a form control that is used to change the value of a property.
 */
@Suppress("LeakingThis", "MemberVisibilityCanBePrivate")
abstract class FormControl<V : Any, C: UIControl<V>>(initialValue: V): UILinearContainer() {

    /**
     * The control that is used to change the value.
     */
    abstract val control: C

    /**
     * The key that is used to identify the control.
     */
    var key
        get() = control.key
        set(value) { control.key = value }


    /**
     * The text that is displayed as the value of the control.
     */
    open val valueText: UIText? = null

    /**
     * The text that is displayed as the label of the control.
     */
    open val labelText = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        style = {
            fontSize = FontSize.SM
            color = it.accentColor
        }
    }

    /**
     * The button that is used to reset the value of the control to its default value.
     */
    open val resetButton = UITextButton().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        scaleOrigin = Anchor.Center
        text = "Reset"
        leadingIcon = UIIcon(FAIcon.RotateLeft)
        colorVariant = ColorVariant.Primary
        sizeVariant = SizeVariant.Small

        alpha = 0f
        translationX = -10f

        onActionUp = {
            if (this@FormControl.isVisible) {
                value = defaultValue
            }
        }
    }


    /**
     * The value of the control.
     */
    var value
        get() = control.value
        set(value) { control.value = value }

    /**
     * The default value of the control.
     */
    var defaultValue = initialValue

    /**
     * The callback that is called when the value of the control changes.
     */
    var onValueChanged: ((V) -> Unit)? = null

    /**
     * The label text.
     */
    var label
        get() = labelText.text
        set(value) { labelText.text = value }

    /**
     * Whether the control is enabled or not. If disabled, the control will not process any
     * touch events.
     */
    var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                onEnableStateChange()
            }
        }

    /**
     * The formatter that is used to format the value of the control.
     */
    var valueFormatter: (value: V) -> String = Any::toString
        set(value) {
            if (field != value) {
                field = value
                onControlValueChanged()
            }
        }

    /**
     * Whether to show the reset button or not. If true, the reset button will be shown when the
     * value of the control is not equal to the default value.
     */
    var showResetButton = true
        set(value) {
            if (field != value) {
                field = value
                updateResetButtonVisibility()
            }
        }

    /**
     * Whether the current [value] equals to [defaultValue]. Can be overridden to provide a custom behavior.
     */
    protected open val isDefault
        get() = value == defaultValue

    init {
        width = Dimension.FillAvailable
        style = {
            padding = Vec4(2f.srem)
        }
    }


    //region Callbacks

    /**
     * Called when the value of the control changes.
     */
    open fun onControlValueChanged() {
        valueText?.text = valueFormatter(value)
        updateResetButtonVisibility()
        onValueChanged?.invoke(control.value)
    }

    /**
     * Called when the enable state of the button changes.
     */
    open fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)

        if (isEnabled) {
            fadeTo(1f, 0.2f)
        } else {
            fadeTo(0.25f, 0.2f)
        }
    }

    //endregion

    private fun updateResetButtonVisibility() {
        if (showResetButton) {
            resetButton.apply {
                if (!isVisible && !isDefault) {
                    clearEntityModifiers()
                    isVisible = true
                    translateToX(0f, 0.1f)
                    fadeIn(0.1f)
                } else if (isVisible && isDefault) {
                    clearEntityModifiers()
                    translateToX(-10f, 0.1f)
                    fadeOut(0.1f).after { isVisible = false }
                }
            }
        } else {
            resetButton.isVisible = false
        }
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!isEnabled) {
            return true
        }
        return super.onAreaTouched(event, localX, localY)
    }

}