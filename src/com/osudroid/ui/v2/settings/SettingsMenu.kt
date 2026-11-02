package com.osudroid.ui.v2.settings

import androidx.annotation.ArrayRes
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.transformSequence
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.control.UISelect
import com.reco1l.verktex.ui.form.FormControl
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import kotlin.math.ceil
import kotlin.math.log10
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager


enum class Section {
    General,
    Gameplay,
    Graphics,
    Audio,
    Library,
    Input,
    Advanced
}

data class CategoryInfo(

    /**
     * The title of the category.
     */
    val title: String,

    /**
     * The options that will be displayed in the category.
     */
    val options: List<OptionInfo>,
)

enum class OptionType {
    Checkbox,
    Input,
    Select,
    Button,
    Slider,
}

data class OptionInfo(
    /**
     * The type of the option.
     */
    val type: OptionType,

    // General props

    /**
     * The key that will be used to store the value of this option.
     */
    val key: String,

    /**
     * The title of the option.
     */
    val title: Any,

    /**
     * The summary of the option.
     */
    val summary: Any? = null,

    /**
     * The default value that will be used if no value is stored for this option.
     */
    val defaultValue: Any? = null,

    /**
     * The action that will be performed when the value of this option changes.
     */
    val onChange: ((value: Any) -> Unit)? = null,

    // Button props

    /**
     * The action that will be performed when the button is clicked.
     *
     * This property is only used in [OptionType.Button] preferences.
     *
     */
    val onClick: (() -> Unit)? = null,


    // FormSelect props

    /**
     * The entries of the select.
     *
     * This property is only used in [OptionType.Select] preferences.
     */
    @param:ArrayRes val entries: Int? = null,

    /**
     * The entry values of the select.
     *
     * This property is only used in [OptionType.Select] preferences.
     */
    @param:ArrayRes val entryValues: Int? = null,

    // FormSlider props

    /**
     * The minimum value of the slider.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val min: Float? = null,

    /**
     * The maximum value of the slider.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val max: Float? = null,

    /**
     * The step of the slider.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val step: Float? = 0.01f,

    /**
     * Whether the slider stores its value as a float instead of an integer.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val isFloat: Boolean = false,

    /**
     * The value formatter that will be used to format the value.
     *
     * This property is only used in [OptionType.Slider] preferences.
     */
    val valueFormatter: ((value: Float) -> String)? = null,


    /**
     * Called when the component is attached to the scene.
     */
    val onAttach: (component: UIComponent) -> Unit = { },
)

class SettingsMenu : UIScene() {

    private var selectedSection = Section.General

    private lateinit var sectionScroll: UIScrollableContainer
    private lateinit var sectionContainer: UILinearContainer

    private val playfieldAreaDisplay = PlayfieldAreaDisplay()


    init {
        style = Style { theme -> backgroundColor = theme.palette.surface }

        linearContainer {
            width = fill()
            height = fill()
            orientation = UILinearContainer.Orientation.Horizontal
            style = Style { theme -> spacing = theme.gap.sm }

            // Sections
            scrollableContainer {
                scrollAxes = Axis.Y
                height = fill()
                style = Style { theme ->
                    padding = Padding(theme.gap.lg)
                    backgroundColor = theme.palette.buttonBackgroundSurface
                }

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    style = Style { theme -> spacing = theme.gap.sm }

                    +UITextButton().apply {
                        text = "Back"
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        density = UIButton.Density.Large
                        variant = UIButton.Variant.Ghost
                        width = SIDEBAR_WIDTH
                        alignment = Anchor.CenterLeft
                        onActionUp = { back() }
                    }

                    box {
                        width = SIDEBAR_WIDTH
                        height = 2f.dip
                        style = Style { theme -> backgroundColor = theme.palette.divider }
                    }

                    +SectionButton(Section.General, FAIcon.Gears, generalSection)
                    +SectionButton(Section.Gameplay, FAIcon.Gamepad, gameplaySection)
                    +SectionButton(Section.Graphics, FAIcon.Display, graphicsSection)
                    +SectionButton(Section.Audio, FAIcon.Headphones, audioSection)
                    +SectionButton(Section.Library, FAIcon.Book, librarySection)
                    +SectionButton(Section.Input, FAIcon.ComputerMouse, inputSection)
                    +SectionButton(Section.Advanced, FAIcon.ScrewdriverWrench, advancedSection)
                }
            }

            sectionScroll = scrollableContainer {
                scrollAxes = Axis.Y
                width = fill()
                height = fill()

                sectionContainer = linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    width = fill()
                    style = Style { theme ->
                        spacing = theme.gap.lg
                        padding = Padding(theme.gap.xl, theme.gap.lg)
                    }
                }
            }
        }

        +playfieldAreaDisplay
        onPlayfieldChanged = { playfieldAreaDisplay.update() }

        generateSection(generalSection)
    }


    /**
     * Leaves the menu, applying the changed preferences the same way the legacy settings screen does.
     */
    fun back() {
        val global = GlobalManager.getInstance()

        Config.loadConfig(global.mainActivity)
        onPlayfieldChanged = null

        // The player button and the beat tracking of the main menu keep themselves up to date.
        if (!Multiplayer.isMultiplayer) {
            global.songService.isGaming = false
        }

        Verktex.withTreeLock { Verktex.popScene() }
    }


    fun generateSection(content: List<CategoryInfo>) {
        sectionContainer.alpha = 0f
        sectionContainer.clearChildren()
        sectionScroll.scrollY = 0f

        sectionContainer.apply {
            content.forEachIndexed { index, category ->

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    width = fill()

                    text {
                        width = fill()
                        text = category.title
                        style = Style { theme ->
                            fontSettings = theme.typography.h3
                            padding = Padding(theme.gap.sm)
                        }
                    }

                    category.options.forEach { option ->
                        val title = when (option.title) {
                            is Int -> GlobalManager.getInstance().mainActivity.getString(option.title)
                            else -> option.title.toString()
                        }

                        if (option.type == OptionType.Button) {
                            val button = UITextButton().apply {
                                width = fill()
                                text = title
                                onActionUp = { option.onClick?.invoke() }
                            }

                            container {
                                width = fill()
                                style = Style { theme -> padding = Padding(theme.gap.sm) }
                                +button
                            }

                            option.onAttach(button)
                            addSummary(option)
                        } else {
                            val control = createControl(option)
                            control.key = option.key
                            control.label = title

                            @Suppress("UNCHECKED_CAST")
                            (control as FormControl<Any, *>).onValueChanged = option.onChange

                            +control
                            option.onAttach(control)
                            addSummary(option)
                        }
                    }
                }

                if (index != content.lastIndex) {
                    box {
                        width = fill()
                        height = 2f.dip
                        style = Style { theme -> backgroundColor = theme.palette.divider }
                    }
                }
            }
        }
    }

    private fun UILinearContainer.addSummary(option: OptionInfo) {
        val summary = when (option.summary) {
            null -> return
            is Int -> GlobalManager.getInstance().mainActivity.getString(option.summary)
            else -> option.summary.toString()
        }

        text {
            width = fill()
            wrapText = true
            text = summary
            alpha = 0.6f
            style = Style { theme ->
                fontSettings = theme.typography.smallCaption
                padding = Padding(left = theme.gap.sm, right = theme.gap.sm, bottom = theme.gap.sm)
            }
        }
    }

    private fun createControl(option: OptionInfo): FormControl<*, *> = when (option.type) {

        OptionType.Checkbox -> PreferenceCheckbox(option.key, option.defaultValue as? Boolean ?: false)

        OptionType.Input -> PreferenceInput(option.key, option.defaultValue as? String ?: "")

        OptionType.Select -> PreferenceSelect(option.key, option.defaultValue as? String ?: "").apply {
            if (option.entries != null && option.entryValues != null) {
                val resources = GlobalManager.getInstance().mainActivity.resources
                val entries = resources.getStringArray(option.entries)
                val values = resources.getStringArray(option.entryValues)

                if (entries.size == values.size) {
                    options = entries.mapIndexed { i, entry -> UISelect.Option(values[i], entry) }
                }
            }
        }

        OptionType.Slider -> (
            if (option.isFloat) FloatPreferenceSlider(option.key, (option.defaultValue as? Number)?.toFloat() ?: 0f)
            else IntPreferenceSlider(option.key, (option.defaultValue as? Number)?.toInt() ?: 0)
        ).apply {
            val step = option.step ?: 1f

            control.min = option.min ?: 0f
            control.max = option.max ?: 100f
            control.step = step

            if (step > 0f && step < 1f) {
                control.precision = ceil(-log10(step.toDouble())).toInt()
            }

            valueFormatter = option.valueFormatter ?: { it.toInt().toString() }
        }

        OptionType.Button -> error("Buttons are not form controls")
    }


    override fun onUpdate(clock: FrameClock) {
        sectionContainer.alpha = clock.interpolateFloat(0.2f, sectionContainer.alpha, 1f)
        super.onUpdate(clock)
    }


    /**
     * Shows the area the playfield will take while its size or position are being changed.
     */
    private class PlayfieldAreaDisplay : UIBox() {

        private var previousPlayfieldSize = Config.getPlayfieldSize()

        init {
            backgroundColor = Color4.White
            alpha = 0f
        }

        fun update() {
            val parentBounds = parent?.bounds ?: return
            val playfieldSize = Config.getPlayfieldSize()

            // Nothing to show if the playfield size is 100%, since it will fill the entire screen anyway,
            // making position settings irrelevant.
            if (previousPlayfieldSize == 1f && playfieldSize == 1f) {
                return
            }

            previousPlayfieldSize = playfieldSize

            val areaWidth = playfieldSize * parentBounds.width
            val areaHeight = playfieldSize * parentBounds.height

            width = areaWidth.px
            height = areaHeight.px
            x = ((parentBounds.width - areaWidth) * Config.getPlayfieldHorizontalPosition()).px
            y = ((parentBounds.height - areaHeight) * Config.getPlayfieldVerticalPosition()).px

            clearTransforms()
            transformSequence {
                fadeTo(0.3f, 0.4f, Easing.OutQuint)
                then()
                fadeOut(0.5f)
            }
        }
    }


    inner class SectionButton(private val section: Section, icon: Int, content: List<CategoryInfo>) : UITextButton() {

        init {
            leadingIcon = faSolidIcon(icon)
            density = Density.Large
            variant = Variant.Ghost
            alignment = Anchor.CenterLeft
            text = section.name
            width = SIDEBAR_WIDTH

            onActionUp = {
                if (selectedSection != section) {
                    selectedSection = section
                    generateSection(content)
                }
            }
        }

        override fun onUpdate(clock: FrameClock) {
            isSelected = selectedSection == section
            super.onUpdate(clock)
        }
    }


    private companion object {
        val SIDEBAR_WIDTH = 200f.dip
    }
}
