package com.osudroid.ui.v2.settings

import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring
import com.osudroid.resources.StringArrayResource
import com.osudroid.resources.StringResource
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
import com.reco1l.verktex.ui.form.FormSlider
import com.reco1l.verktex.ui.form.FormSelect
import com.reco1l.verktex.ui.form.FormInput
import com.reco1l.verktex.ui.form.FormCheckbox
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import kotlin.math.ceil
import kotlin.math.log10
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import com.reco1l.verktex.ui.themed


enum class Section {
    Player,
    Room,
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

    /**
     * Whether the category is shown, false for features the platform doesn't have.
     */
    val isVisible: Boolean = true,
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
    val entries: StringArrayResource? = null,

    /**
     * The entry values of the select.
     *
     * This property is only used in [OptionType.Select] preferences.
     */
    val entryValues: StringArrayResource? = null,

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

    /**
     * The current value of an option that isn't a preference, e.g. a setting of the multiplayer
     * room. When set the value is taken from it and not stored, [onChange] applies it.
     */
    val initialValue: (() -> Any)? = null,
)

class SettingsMenu : UIScene() {

    private var selectedSection = Section.General

    private lateinit var sectionScroll: UIScrollableContainer
    private lateinit var sectionContainer: UILinearContainer

    private val playfieldAreaDisplay = PlayfieldAreaDisplay()


    init {
        backgroundColor = themed { palette.surface }

        linearContainer {
            width = fill()
            height = fill()
            orientation = UILinearContainer.Orientation.Horizontal
            spacing = themed { gap.sm }

            // Sections
            scrollableContainer {
                scrollAxes = Axis.Y
                height = fill()
                padding = themed { Padding(gap.lg) }
                backgroundColor = themed { palette.buttonBackgroundSurface }

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    spacing = themed { gap.xs }

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
                        backgroundColor = themed { palette.divider }
                    }

                    // In a room its settings come first, and the ones only meant for the menu are hidden.
                    if (Multiplayer.isMultiplayer) {
                        sidebarCaption("Multiplayer")
                        +SectionButton(Section.Player, FAIcon.User)
                        if (Multiplayer.isRoomHost) {
                            +SectionButton(Section.Room, FAIcon.UserGroup)
                        }
                        sidebarCaption("Game")
                    } else {
                        +SectionButton(Section.General, FAIcon.Gears)
                    }

                    +SectionButton(Section.Gameplay, FAIcon.Gamepad)
                    +SectionButton(Section.Graphics, FAIcon.Display)
                    +SectionButton(Section.Audio, FAIcon.Headphones)
                    if (!Multiplayer.isMultiplayer) {
                        +SectionButton(Section.Library, FAIcon.Book)
                    }
                    +SectionButton(Section.Input, FAIcon.ComputerMouse)
                    +SectionButton(Section.Advanced, FAIcon.ScrewdriverWrench)
                }
            }

            sectionScroll = scrollableContainer {
                scrollAxes = Axis.Y
                width = fill()
                height = fill()

                sectionContainer = linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    width = fill()
                    spacing = themed { gap.lg }
                    padding = themed { Padding(gap.xl, gap.lg) }
                }
            }
        }

        +playfieldAreaDisplay
        onPlayfieldChanged = { playfieldAreaDisplay.update() }

        selectedSection = when {
            Multiplayer.isRoomHost -> Section.Room
            Multiplayer.isMultiplayer -> Section.Player
            else -> Section.General
        }
        // The section is generated in onEnter.
    }


    // Values may have changed elsewhere meanwhile, e.g. the offset in its calibration.
    override fun onEnter() {
        super.onEnter()
        generateSection(contentOf(selectedSection))
    }

    private fun contentOf(section: Section) = when (section) {
        Section.Player -> playerSection
        Section.Room -> roomSection
        Section.General -> generalSection
        Section.Gameplay -> gameplaySection
        Section.Graphics -> graphicsSection
        Section.Audio -> audioSection
        Section.Library -> librarySection
        Section.Input -> inputSection
        Section.Advanced -> advancedSection
    }

    private fun UILinearContainer.sidebarCaption(caption: String) = text {
        text = caption
        alpha = 0.6f
        fontSettings = themed { typography.caption }
        padding = themed { Padding(gap.sm, gap.xs) }
    }


    /**
     * Leaves the menu, applying the changed preferences the same way the legacy settings screen does.
     */
    fun back() {
        val global = GlobalManager.getInstance()

        Config.loadConfig()
        onPlayfieldChanged = null

        if (Multiplayer.isMultiplayer) {
            if (Multiplayer.isConnected) {
                Multiplayer.roomScene?.chat?.show()
                Multiplayer.roomScene?.updatePlayerList()
            }
        } else {
            global.songService.isGaming = false

            // The account may have changed (Config.loadConfig above reloads it), the player button
            // follows the login by itself. Headless (e.g. in the UI test browser) it never logs in.
            if (!global.isHeadless) {
                OnlineManager.getInstance().init()
                OnlineScoring.getInstance().login()
            }
        }

        Verktex.withTreeLock { Verktex.popScene() }
    }


    fun generateSection(content: List<CategoryInfo>) {
        sectionContainer.alpha = 0f
        sectionContainer.clearChildren()
        sectionScroll.scrollY = 0f

        sectionContainer.apply {
            content.filter { it.isVisible }.forEachIndexed { index, category ->

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    width = fill()

                    text {
                        width = fill()
                        text = category.title
                        fontSettings = themed { typography.h3 }
                        padding = themed { Padding(gap.sm) }
                    }

                    category.options.forEach { option ->
                        val title = when (option.title) {
                            is StringResource -> StringTable.get(option.title)
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
                                padding = themed { Padding(gap.sm) }
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
                        backgroundColor = themed { palette.divider }
                    }
                }
            }
        }
    }

    private fun UILinearContainer.addSummary(option: OptionInfo) {
        val summary = when (option.summary) {
            null -> return
            is StringResource -> StringTable.get(option.summary)
            else -> option.summary.toString()
        }

        text {
            width = fill()
            wrapText = true
            text = summary
            alpha = 0.6f
            fontSettings = themed { typography.smallCaption }
            padding = themed { Padding(left = gap.sm, right = gap.sm, bottom = gap.sm) }
        }
    }

    private fun createControl(option: OptionInfo): FormControl<*, *> {
        val initialValue = option.initialValue?.invoke()

        // Options that aren't preferences show the current value and aren't stored.
        val control = if (initialValue != null) {
            when (option.type) {
                OptionType.Checkbox -> FormCheckbox(initialValue as Boolean)
                OptionType.Input -> FormInput(initialValue as String)
                OptionType.Select -> FormSelect(listOf(initialValue as String))
                OptionType.Slider -> FormSlider((initialValue as Number).toFloat())
                OptionType.Button -> error("Buttons are not form controls")
            }.apply { showResetButton = false }
        } else {
            when (option.type) {
                OptionType.Checkbox -> PreferenceCheckbox(option.key, option.defaultValue as? Boolean ?: false)
                OptionType.Input -> PreferenceInput(option.key, option.defaultValue as? String ?: "")
                OptionType.Select -> PreferenceSelect(option.key, option.defaultValue as? String ?: "")
                OptionType.Slider ->
                    if (option.isFloat) FloatPreferenceSlider(option.key, (option.defaultValue as? Number)?.toFloat() ?: 0f)
                    else IntPreferenceSlider(option.key, (option.defaultValue as? Number)?.toInt() ?: 0)
                OptionType.Button -> error("Buttons are not form controls")
            }
        }

        if (control is FormSelect<*> && option.entries != null && option.entryValues != null) {
            val entries = StringTable.getArray(option.entries)
            val values = StringTable.getArray(option.entryValues)

            if (entries.size == values.size) {
                @Suppress("UNCHECKED_CAST")
                (control as FormSelect<String>).options = entries.mapIndexed { i, entry -> UISelect.Option(values[i], entry) }
            }
        }

        if (control is FormSlider) {
            val step = option.step ?: 1f

            // The maximum first, the range starts at 0..1 and a minimum above it is rejected.
            control.control.max = option.max ?: 100f
            control.control.min = option.min ?: 0f
            control.control.step = step

            if (step > 0f && step < 1f) {
                control.control.precision = ceil(-log10(step.toDouble())).toInt()
            }

            control.valueFormatter = option.valueFormatter ?: { it.toInt().toString() }
        }

        return control
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


    inner class SectionButton(private val section: Section, icon: Int) : UITextButton() {

        init {
            leadingIcon = faSolidIcon(icon)
            variant = Variant.Ghost
            alignment = Anchor.CenterLeft
            text = section.name
            width = SIDEBAR_WIDTH

            onActionUp = {
                if (selectedSection != section) {
                    selectedSection = section
                    generateSection(contentOf(section))
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
