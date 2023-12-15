package com.reco1l.rimu.ui.views

import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.R
import com.reco1l.framework.graphics.toInt
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnable


class CheckBox(override val ctx: MainContext) :
    AppCompatCheckBox(ctx),
    IWithContext,
    ISkinnable
{

    init
    {
        // Using the check box drawable as background instead of button in order to allow the
        // drawable being affected by view dimensions.
        background = ctx.getDrawableCompat(R.drawable.abc_btn_check_material_anim)
        buttonDrawable = null

        // Disabling scroll bar, this fixes a weird visual bug.
        scrollBarSize = 0
    }


    override fun onApplySkin(skin: WorkingSkin)
    {
        background.setTint(skin.data.colours.accentColor.toInt())
    }
}