package game.rimu.ui.views

import android.annotation.SuppressLint
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.R
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.views.addons.ISkinnable


class CheckBox(override val ctx: RimuContext) :
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
        background.setTint(skin.data.colours.accentColor.hexInt)
    }
}