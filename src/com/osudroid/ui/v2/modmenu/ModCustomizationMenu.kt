package com.osudroid.ui.v2.modmenu

import com.osudroid.mods.*
import com.osudroid.mods.settings.*
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.framework.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIModal
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.text.UIText
import kotlin.reflect.KClass

class ModCustomizationMenu(private val trigger: UIButton) : UIModal(

    card = UIScrollableContainer().apply {
        scrollAxes = Axis.Y
        width = 0.475f.pct
        height = 0.675f.pct
        scaleOrigin = Anchor.TopCenter
        clipping = ClipMode.Bounds
        scrollPadding = Vec2(0f, 64f.dip.toPixels())
        style = Style { theme ->
            backgroundColor = theme.palette.accent * 0.15f
            radius = theme.radius.lg
        }

        +UILinearContainer().apply {
            width = Dimension.FillAvailable
            orientation = UILinearContainer.Orientation.Vertical
        }
    }

) {

    private val modSettings = mutableMapOf<KClass<out Mod>, ModSettingsSection>()
    private val modSettingsContainer = card[0] as UILinearContainer
    private val modSettingComponents = mutableListOf<IModSettingComponent<*>>()


    override fun onShow() {
        // Placed below the button that opens this menu, which only has a known position when shown.
        val triggerBottom = trigger.toSceneSpace(0f, trigger.bounds.height).y

        card.x = Verktex.graphics.window.insets.safeArea.x.px
        card.y = (triggerBottom + 12f.dip.toPixels()).px

        super.onShow()
    }


    //region Mods

    fun onModAdded(mod: Mod) {
        if (mod.settings.isEmpty()) {
            return
        }

        val section = ModSettingsSection(mod)

        modSettings[mod::class]?.removeSelf()
        modSettings[mod::class] = section

        modSettingsContainer += section
    }

    fun onModRemoved(mod: Mod) {
        modSettings[mod::class]?.removeSelf()
        modSettings.remove(mod::class)
    }

    fun isEmpty() = modSettings.isEmpty()

    //endregion

    //region Component lifecycle

    fun updateComponents() {
        modSettingComponents.forEach { it.update() }
    }

    fun updateComponentEnabledStates() {
        val room = Multiplayer.room
        val isHost = Multiplayer.isRoomHost

        modSettingComponents.forEach {
            it.isEnabled = if (room != null) {
                it.mod.isValidForMultiplayer && (isHost || (room.gameplaySettings.isFreeMod && it.mod.isValidForMultiplayerAsFreeMod))
            } else {
                true
            }
        }
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

            else -> throw IllegalArgumentException("Unsupported mod setting type: ${setting::class.java.simpleName}")
        }

        modSettingComponents.add(component)
        component.update()

        component.isEnabled =
            if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
                mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                    (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
            } else true

        return component
    }

    //endregion


    private inner class ModSettingsSection(val mod: Mod) : UILinearContainer() {

        init {
            orientation = Orientation.Vertical
            width = Dimension.FillAvailable
            padding = Padding(bottom = 16f.dip)

            +UILinearContainer().apply {
                orientation = Orientation.Horizontal
                width = Dimension.FillAvailable
                padding = Padding(12f.dip)
                spacing = 8f.dip
                backgroundColor = Color4.Black.copy(alpha = 0.05f)
                style = Style { theme ->
                    radius = theme.radius.lg
                }

                +ModIcon(mod).apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    width = 14f.dip
                    height = 14f.dip
                }

                +UIText().apply {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    text = mod.name.uppercase()
                    style = Style { theme ->
                        fontSize = theme.typography.smallBody.size
                        color = theme.palette.accent
                        alpha = 0.9f
                    }
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                width = Dimension.FillAvailable
                padding = Padding(8f.dip, 0f.dip)
                mod.settings.forEachTolerant { +ModSettingComponent(mod, it) }
            }
        }
    }
}
