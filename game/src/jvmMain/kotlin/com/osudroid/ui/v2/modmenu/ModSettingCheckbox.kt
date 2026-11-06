package com.osudroid.ui.v2.modmenu

import com.reco1l.verktex.ui.control.UIControl
import com.reco1l.verktex.ui.form.FormCheckbox
import com.reco1l.verktex.ui.form.FormControl
import com.osudroid.mods.Mod
import com.osudroid.mods.settings.ModSetting

class ModSettingCheckbox(mod: Mod, setting: ModSetting<Boolean>) :
    ModSettingComponent<Boolean, Boolean>(mod, setting),
    IModSettingComponent<Boolean> {

    @Suppress("UNCHECKED_CAST")
    override fun createControl() = FormCheckbox(setting.initialValue) as FormControl<Boolean, UIControl<Boolean>>

    override fun convertSettingValue(value: Boolean) = value
    override fun convertControlValue(value: Boolean) = value
}
