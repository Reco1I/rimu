package com.reco1l.verktex.ui.control

import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.allPadding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.TickingClock
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
                if (value.isEmpty()) {
                    button.text = value
                }
            }
        }


    private var buttons = mapOf<Option<T>, UITextButton>()

    private var listChanged = true


    init {
        +button.apply {
            text = placeholder
        }

    }

    override fun onUpdate(clock: TickingClock) {

        val targetChevronRotation = if (dropdown.isExpanded) 180f else 0f
        chevronIcon.rotationZ = clock.interpolateFloat(0.4f, chevronIcon.rotationZ, targetChevronRotation, Easing.OutBounce)

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
    }

    override fun onValueChanged() {
        super.onValueChanged()

        if (value.isEmpty()) {
            button.text = placeholder
            dropdown.forEachButton { it.isSelected = false }
            return
        }

        when (selectionMode) {

            SelectionMode.Single -> {
                val selectedOption = options.firstOrNull { it.value == value.firstOrNull() }
                button.text = selectedOption?.text ?: placeholder
            }

            SelectionMode.Multiple -> {
                button.text = options.filter { o -> o.value in value }.joinToString { it.text }
            }
        }

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

