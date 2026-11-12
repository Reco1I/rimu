package com.osudroid.ui.v2.settings

import com.osudroid.OsuGame
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.settings.SettingKey
import com.osudroid.settings.OsuSettings
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.ui.v2.settings.SettingsOption.Type
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton.Variant
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.transformSequence
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
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
import com.reco1l.verktex.ui.textButton
import kotlin.math.ceil
import kotlin.math.log10
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent


class SettingsMenu : UIScene() {

    // Back goes back a section or leaves the settings, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            back()
            return true
        }
        return false
    }

    private var selectedSection = SettingsSection.General

    private lateinit var sectionScroll: UIScrollableContainer
    private lateinit var sectionContainer: UILinearContainer

    private val playfieldAreaDisplay = PlayfieldAreaDisplay()


    init {
        background = Background.Color(themed { palette.surface })

        linearContainer {
            width = fill()
            height = fill()
            orientation = Orientation.Horizontal

            // Sections
            scrollableContainer {
                scrollAxes = Axis.Y
                height = fill()
                padding = Padding(16f.dip)
                background = Background.Color(themed { palette.buttonBackgroundSurface })

                linearContainer {
                    width = fill()
                    orientation = Orientation.Vertical
                    spacing = themed { gap.xs }

                   textButton {
                        text = "Back"
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        variant = Variant.Ghost
                        width = SIDEBAR_WIDTH
                        alignment = Anchor.CenterLeft
                        onActionUp = { back() }
                    }

                    box {
                        width = SIDEBAR_WIDTH
                        height = 2f.dip
                        background = Background.Color(themed { palette.divider })
                    }

                    // In a room its settings come first, and the ones only meant for the menu are hidden.
                    if (Multiplayer.isMultiplayer) {
                        sidebarCaption("Multiplayer")
                        +SectionButton(SettingsSection.Player, FAIcon.User)
                        if (Multiplayer.isRoomHost) {
                            +SectionButton(SettingsSection.Room, FAIcon.UserGroup)
                        }
                        sidebarCaption("Game")
                    } else {
                        +SectionButton(SettingsSection.General, FAIcon.Gears)
                    }

                    +SectionButton(SettingsSection.Gameplay, FAIcon.Gamepad)
                    +SectionButton(SettingsSection.Graphics, FAIcon.Display)
                    +SectionButton(SettingsSection.Audio, FAIcon.Headphones)
                    if (!Multiplayer.isMultiplayer) {
                        +SectionButton(SettingsSection.Library, FAIcon.Book)
                    }
                    +SectionButton(SettingsSection.Input, FAIcon.ComputerMouse)
                    +SectionButton(SettingsSection.Advanced, FAIcon.ScrewdriverWrench)
                }
            }

            sectionScroll = scrollableContainer {
                scrollAxes = Axis.Y
                width = fill()
                height = fill()

                sectionContainer = linearContainer {
                    orientation = Orientation.Vertical
                    width = fill()
                    spacing = themed { gap.lg }
                    padding = themed { Padding(gap.xl, gap.lg) }
                }
            }
        }

        +playfieldAreaDisplay
        onPlayfieldChanged = { playfieldAreaDisplay.update() }

        selectedSection = when {
            Multiplayer.isRoomHost -> SettingsSection.Room
            Multiplayer.isMultiplayer -> SettingsSection.Player
            else -> SettingsSection.General
        }
        // The section is generated in onEnter.
    }


    // Values may have changed elsewhere meanwhile, e.g. the offset in its calibration.
    override fun onEnter() {
        super.onEnter()
        generateSection(contentOf(selectedSection))
    }

    private fun contentOf(section: SettingsSection) = when (section) {
        SettingsSection.Player -> playerSection
        SettingsSection.Room -> roomSection
        SettingsSection.General -> generalSection
        SettingsSection.Gameplay -> gameplaySection
        SettingsSection.Graphics -> graphicsSection
        SettingsSection.Audio -> audioSection
        SettingsSection.Library -> librarySection
        SettingsSection.Input -> inputSection
        SettingsSection.Advanced -> advancedSection
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
        onPlayfieldChanged = null

        if (Multiplayer.isMultiplayer) {
            if (Multiplayer.isConnected) {
                Multiplayer.roomScene?.chat?.show()
                Multiplayer.roomScene?.updatePlayerList()
            }
        } else {
            OsuGame.music.isGaming = false

            // The account may have changed, the player button follows the login by itself. Headless (e.g. in the UI test browser) it never logs in.
            if (!OsuGame.isHeadless) {
                OnlineManager.getInstance().init()
                OnlineScoring.getInstance().login()
            }
        }

        Verktex.withTreeLock { Verktex.popScene() }
    }


    fun generateSection(content: List<SettingsCategory>) {
        sectionContainer.alpha = 0f
        sectionContainer.clearChildren()
        sectionScroll.scrollY = 0f

        sectionContainer.apply {
            content.filter { it.isVisible }.forEachIndexed { index, category ->

                linearContainer {
                    orientation = Orientation.Vertical
                    width = fill()

                    text {
                        width = fill()
                        text = category.title
                        fontSettings = themed { typography.h3 }
                        padding = themed { Padding(gap.sm) }
                    }

                    category.options.forEach { option ->
                        val title = option.title

                        if (option.type == Type.Button) {
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
                        background = Background.Color(themed { palette.divider })
                    }
                }
            }
        }
    }

    private fun UILinearContainer.addSummary(option: SettingsOption) {
        val summary = option.summary ?: return

        text {
            width = fill()
            wrapText = true
            text = summary
            alpha = 0.6f
            fontSettings = themed { typography.smallCaption }
            padding = themed { Padding(left = gap.sm, right = gap.sm, bottom = gap.sm) }
        }
    }

    private fun createControl(option: SettingsOption): FormControl<*, *> {
        val initialValue = option.initialValue?.invoke()

        // Options that aren't preferences show the current value and aren't stored.
        val control = if (initialValue != null) {
            when (option.type) {
                Type.Checkbox -> FormCheckbox(initialValue as Boolean)
                Type.Input -> FormInput(initialValue as String)
                Type.Select -> FormSelect(listOf(initialValue as String))
                Type.Slider -> FormSlider((initialValue as Number).toFloat())
                Type.Button -> error("Buttons are not form controls")
            }.apply { showResetButton = false }
        } else {
            val setting = option.setting ?: error("The option \"${option.key}\" has neither a setting nor an initial value")

            @Suppress("UNCHECKED_CAST")
            when (option.type) {
                Type.Checkbox -> PreferenceCheckbox(setting as SettingKey<Boolean>)
                Type.Input -> PreferenceInput(setting as SettingKey<String>)
                Type.Select -> PreferenceSelect(setting as SettingKey<String>)
                Type.Slider ->
                    if (setting.default is Float) FloatPreferenceSlider(setting as SettingKey<Float>)
                    else IntPreferenceSlider(setting as SettingKey<Int>)
                Type.Button -> error("Buttons are not form controls")
            }
        }

        if (control is FormSelect<*> && option.entries != null && option.entryValues != null) {
            val entries = option.entries
            val values = option.entryValues

            if (entries.size == values.size) {
                @Suppress("UNCHECKED_CAST")
                (control as FormSelect<String>).options = entries.values.mapIndexed { i, entry -> UISelect.Option(values[i], entry) }
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

        private var previousPlayfieldSize = OsuSettings.playfieldSize

        init {
            background = Background.Color(Color4.White)
            alpha = 0f
        }

        fun update() {
            val parentBounds = parent?.bounds ?: return
            val playfieldSize = OsuSettings.playfieldSize

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
            x = ((parentBounds.width - areaWidth) * OsuSettings.playfieldHorizontalPosition).px
            y = ((parentBounds.height - areaHeight) * OsuSettings.playfieldVerticalPosition).px

            clearTransforms()
            transformSequence {
                fadeTo(0.3f, 0.4f, Easing.OutQuint)
                then()
                fadeOut(0.5f)
            }
        }
    }


    inner class SectionButton(private val section: SettingsSection, icon: Int) : UITextButton() {

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
