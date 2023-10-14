package game.rimu.ui.views

import android.view.ViewGroup
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.dimensions
import game.rimu.ui.skinningRules



fun <T> T.IconButton(
    attach: Boolean = true,
    texture: String,
    block: IconButton.() -> Unit
) where T : ViewGroup, T : IWithContext = IconButton(ctx, texture).also {

    if (attach)
        addView(it)

    it.block()
}

class IconButton(ctx: RimuContext, textureName: String) : ImageView(ctx)
{
    init
    {
        dimensions {
            height = 50
            width = 70

            scaleType = ScaleType.FIT_CENTER
            padding(15)
        }

        skinningRules {
            texture = textureName to 0
            tint = { accentColor.toInt() }
        }
    }
}