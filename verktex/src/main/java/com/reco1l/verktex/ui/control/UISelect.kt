package com.reco1l.verktex.ui.control

import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatAnimation
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.textButton

/**
 * A dropdown menu that allows the user to select an option from a list.
 */
@Suppress("LeakingThis")
open class UISelect<T : Any>(initialValues: List<T> = emptyList()) : UIControl<List<T>>(initialValues) {

    private val dropdown = UIDropdown(this)

    /**
     * The button that toggles the dropdown menu.
     */
    val button = textButton {
        width = fill()
        alignment = Anchor.CenterLeft
        onActionUp = {
            if (dropdown.isExpanded) {
                dropdown.hide()
            } else {
                dropdown.show()
            }
        }
    }

    val chevronIcon = UIIcon(FAIcon.ChevronDown).apply {
        width = 32f.dip
        height = 32f.dip
        padding = Padding(8f.dip)
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
    }

    /**
     * The options available in the dropdown menu.
     */
    var options = listOf<Option<T>>()
        set(value) {
            if (field != value) {
                field = value
                listChanged = true
            }
        }


    /**
     * The mode of selection for the dropdown menu.
     *
     * @see SelectionMode
     */
    var selectionMode = SelectionMode.Single

    /**
     * The text displayed on the button when no option is selected.
     */
    var placeholder = "Choose an option"
        set(value) {
            if (field != value) {
                field = value
                updateTrigger()
            }
        }


    private val chevronRotation = FloatAnimation(0f, 0.6f, Easing.OutBounce)

    private var buttons = mapOf<Option<T>, UITextButton>()

    private var listChanged = true


    init {
        // The button is already attached by its builder.
        updateTrigger()
        +chevronIcon
    }

    override fun onUpdate(clock: FrameClock) {

        val targetChevronRotation = if (dropdown.isExpanded) 180f else 0f
        chevronRotation.target = targetChevronRotation
        chevronIcon.rotationZ = chevronRotation.update(clock)

        super.onUpdate(clock)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        if (listChanged) {
            listChanged = false
            onOptionsChanged()
        }

        super.onDraw(renderer)
    }

    open fun onOptionsChanged() {
        dropdown.clearButtons()
        buttons = mapOf()

        options.forEach { option ->

            dropdown.addButton {
                text = option.text
                if (option.color != null) color = option.color
                leadingIcon = option.leadingIcon
                trailingIcon = option.trailingIcon
                isSelected = option.value in value
                buttons += option to this

                onActionUp = {
                    when (selectionMode) {

                        SelectionMode.Single -> {
                            value = listOf(option.value)

                            dropdown.forEachButton { it.isSelected = it == this }
                            dropdown.hide()
                        }

                        SelectionMode.Multiple -> {
                            value = if (option.value in value) value - option.value else value + option.value
                        }
                    }
                }

            }

        }

        updateTrigger()
    }

    // Shows the selected options on the button, or the placeholder when there are none.
    private fun updateTrigger() {
        val selectedOptions = when (selectionMode) {
            SelectionMode.Single -> listOfNotNull(options.firstOrNull { it.value == value.firstOrNull() })
            SelectionMode.Multiple -> options.filter { it.value in value }
        }

        button.text = if (selectedOptions.isEmpty()) placeholder else selectedOptions.joinToString { it.text }
    }

    override fun onValueChanged() {
        super.onValueChanged()

        updateTrigger()

        buttons.forEach { (option, button) ->
            button.isSelected = option.value in value
        }
    }

    /**
     * Represents an option in the dropdown menu.
     */
    data class Option<T : Any>(

        /**
         * The value associated with this option.
         */
        val value: T,

        /**
         * The text displayed for this option.
         */
        val text: String,

        /**
         * The color of the option text and icons.
         */
        val color: Color4? = null,

        /**
         * An optional icon displayed before the text.
         */
        val leadingIcon: UIText.IconLookup? = null,

        /**
         * An optional icon displayed after the text.
         */
        val trailingIcon: UIText.IconLookup? = null,
    )

    enum class SelectionMode {

        /**
         * Only one option can be selected at a time.
         */
        Single,

        /**
         * Multiple options can be selected at a time.
         */
        Multiple
    }
}
