package game.rimu.ui.views

import android.view.ViewGroup
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext


fun <T> T.IconButton(
    attach: Boolean = true,
    texture: String,
    block: IconButton.() -> Unit
) where T : ViewGroup, T : IWithContext = IconButton(ctx, texture).also {

    if (attach)
        addView(it)

    it.block()
}

open class IconButton(ctx: RimuContext, initialTexture: String? = null) : ImageView(ctx)
{
    init
    {
        scaleType = ScaleType.FIT_CENTER

        dimensions.apply {
            height = 50
            width = 70

            padding(15)
        }

        skinningRules.apply {

            if (initialTexture != null)
                image = initialTexture

            imageTint = "accentColor"
        }
    }
}