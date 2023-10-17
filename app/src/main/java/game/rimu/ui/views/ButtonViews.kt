package game.rimu.ui.views

import android.view.ViewGroup
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext


fun <T> T.IconButton(
    attach: Boolean = true,
    init: IconButton.() -> Unit
) where T : IWithContext,
        T : ViewGroup = IconButton(ctx, init).also { if (attach) addView(it) }

open class IconButton(ctx: RimuContext, init: IconButton.() -> Unit) : ImageView(ctx, {})
{

    override val dimensions = super.dimensions.apply {

        height = 50
        width = 70
        padding(15)
    }

    override val skinningRules = super.skinningRules.apply {

        imageTint = "accentColor"
    }

    init
    {
        scaleType = ScaleType.FIT_CENTER

        init()
    }
}