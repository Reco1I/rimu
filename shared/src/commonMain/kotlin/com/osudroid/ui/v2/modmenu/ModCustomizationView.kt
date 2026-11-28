package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.*
import com.osudroid.mods.settings.*
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.ui.v2.SkinSounds
import com.reco1l.framework.*
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.themed
import kotlin.reflect.KClass

/**
 * The customization tab of the [ModMenu]: the selected mods that have settings on the left, and the settings
 * of the one picked on the right, as the settings menu lays out its sections.
 */
class ModCustomizationView(
    private val onSettingChanged: (Mod) -> Unit
) : LinearContainer() {

    /**
     * The mod whose settings are shown, if any.
     */
    var selectedMod: Mod? = null
        private set

    private val entries = mutableMapOf<KClass<out Mod>, ModEntry>()
    private val sections = mutableMapOf<KClass<out Mod>, ModSettingsSection>()

    private lateinit var entriesContainer: LinearContainer
    private lateinit var settingsScroll: ScrollableContainer
    private lateinit var sectionsContainer: LinearContainer


    init {
        orientation = Orientation.Horizontal
        width = fill()
        height = fill()

        // Mods
        scrollableContainer {
            scrollAxes = Axis.Y
            height = fill()
            clipping = ClipMode.Bounds
            padding = Padding(16f.dip)
            background = Background.Color(themed { palette.inset.alpha(0.5f) })
            border = Border(1f.dip, top = Color4.Transparent, right = Color4.White.alpha(0.15f), bottom = Color4.Transparent, left = Color4.Transparent)

            entriesContainer = linearContainer {
                width = SIDEBAR_WIDTH
                orientation = Orientation.Vertical
                spacing = themed { gap.xs }
            }
        }

        // Settings
        settingsScroll = scrollableContainer {
            scrollAxes = Axis.Y
            width = fill()
            height = fill()
            clipping = ClipMode.Bounds

            sectionsContainer = linearContainer {
                width = fill()
                orientation = Orientation.Vertical
                padding = themed { Padding(gap.xl, gap.lg) }
            }
        }
    }


    //region Mods

    fun onModAdded(mod: Mod) {
        if (mod.settings.isEmpty()) {
            return
        }

        removeMod(mod::class)

        val entry = ModEntry(mod)
        entries[mod::class] = entry
        entriesContainer += entry

        val section = ModSettingsSection(mod)
        section.isVisible = false
        sections[mod::class] = section
        sectionsContainer += section

        if (selectedMod == null) {
            select(mod)
        }
    }

    fun onModRemoved(mod: Mod) {
        val wasSelected = selectedMod?.let { it::class } == mod::class
        val index = entriesContainer.indexOfFirst { it === entries[mod::class] }

        removeMod(mod::class)

        if (wasSelected) {
            selectedMod = null

            val next = entries.values.toList().let { remaining ->
                if (remaining.isEmpty()) null else remaining[index.coerceIn(0, remaining.lastIndex)]
            }

            next?.let { select(it.mod) }
        }
    }

    private fun removeMod(modClass: KClass<out Mod>) {
        entries.remove(modClass)?.removeSelf()
        sections.remove(modClass)?.removeSelf()
    }

    /**
     * Shows the settings of [mod], one of the mods listed.
     */
    fun select(mod: Mod) {
        val section = sections[mod::class] ?: return

        if (selectedMod === mod && section.isVisible) {
            return
        }

        selectedMod = mod
        sections.values.forEach { it.isVisible = it === section }
        entries.values.forEach { it.restyle() }
        settingsScroll.scrollY = 0f

        section.clearTransforms()
        section.alpha = 0f
        section.fadeIn(0.2f, Easing.OutQuint)
    }

    fun isEmpty() = sections.isEmpty()

    //endregion

    //region Component lifecycle

    private val modSettingComponents
        get() = sections.values.flatMap { it.components }

    fun updateComponents() {
        modSettingComponents.forEach { it.update() }
    }

    fun updateComponentEnabledStates() {
        modSettingComponents.forEach { it.isEnabled = isEnabledFor(it.mod) }
    }

    private fun isEnabledFor(mod: Mod): Boolean {
        val room = Multiplayer.room ?: return true

        return mod.isValidForMultiplayer &&
            (Multiplayer.isRoomHost || (room.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
    }

    @Suppress("UNCHECKED_CAST")
    private fun ModSettingComponent(mod: Mod, setting: ModSetting<*>): ModSettingComponent<*, *> {

        val component = when (setting) {
            is FloatModSetting ->
                if (setting.useManualInput) FloatModSettingTextInput(mod, setting)
                else FloatModSettingSlider(mod, setting)

            is IntegerModSetting ->
                if (setting.useManualInput) IntegerModSettingTextInput(mod, setting)
                else IntegerModSettingSlider(mod, setting)

            is NullableFloatModSetting ->
                if (setting.useManualInput) NullableFloatModSettingTextInput(mod, setting)
                else NullableFloatModSettingSlider(mod, setting)

            is NullableIntegerModSetting ->
                if (setting.useManualInput) NullableIntegerModSettingTextInput(mod, setting)
                else NullableIntegerModSettingSlider(mod, setting)

            is BooleanModSetting -> ModSettingCheckbox(mod, setting)

            is EnumModSetting<*> -> ModSettingEnum(mod, setting as EnumModSetting<Enum<*>>)

            else -> throw IllegalArgumentException("Unsupported mod setting type: ${setting::class.simpleName}")
        }

        component.onChanged = onSettingChanged
        component.update()
        component.isEnabled = isEnabledFor(mod)

        return component
    }

    //endregion


    /**
     * A row of the list of mods, which shows the settings of its mod when pressed.
     */
    private inner class ModEntry(val mod: Mod) : Button() {

        // Highlighted neutral rather than in the accent, as its selection.
        override val highlightColor
            get() = Color4.White

        init {
            width = fill()
            variant = Variant.Ghost

            // A selection marked by a light background rather than a selected button's accent one.
            background = themed {
                Background.Color(if (selectedMod === mod) Color4.White.alpha(0.12f) else Color4.Transparent)
            }

            linearContainer {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = Orientation.Horizontal
                spacing = 10f.dip

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = 22f.dip
                    height = 22f.dip
                }

                text {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    text = mod.name
                }
            }

            onActionUp = {
                SkinSounds["click-short-confirm"].play()
                select(mod)
            }
            onActionCancel = { SkinSounds["click-short"].play() }
        }

    }


    private inner class ModSettingsSection(val mod: Mod) : LinearContainer() {

        val components = mutableListOf<ModSettingComponent<*, *>>()

        init {
            orientation = Orientation.Vertical
            width = fill()
            spacing = themed { gap.sm }

            linearContainer {
                orientation = Orientation.Horizontal
                width = fill()
                height = wrap()
                spacing = 12f.dip
                padding = themed { Padding(gap.sm) }

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = 32f.dip
                    height = 32f.dip
                }

                linearContainer {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = fill()
                    orientation = Orientation.Vertical
                    spacing = 2f.dip

                    text {
                        text = mod.name
                        fontSettings = themed { typography.h3 }
                    }

                    text {
                        width = fill()
                        wrapText = true
                        text = mod.description
                        alpha = 0.6f
                        fontSettings = themed { typography.smallBody }
                    }
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                width = fill()

                mod.settings.forEachTolerant {
                    val component = ModSettingComponent(mod, it)
                    components += component
                    +component
                }
            }
        }
    }


    private companion object {
        val SIDEBAR_WIDTH = 200f.dip
    }
}
