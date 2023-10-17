package game.rimu.ui.views

import android.view.Gravity
import android.view.ViewGroup
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext


fun <T> T.IconButton(
    attach: Boolean = true,
    init: IconButton.() -> Unit
) where T : IWithContext, T : ViewGroup = IconButton(ctx) child@{

    if (attach)
        this@IconButton.addView(this@child)

    init()
}

open class IconButton(ctx: RimuContext, init: IconButton.() -> Unit) : ImageView(ctx, {})
{

    override val dimensions = super.dimensions.apply {

        height = 50
        width = 70
        padding(15)
    }

    override val rules = super.rules.apply {

        imageTint = "accentColor"
    }

    init
    {
        scaleType = ScaleType.FIT_CENTER

        init()
    }
}



// TextButton

fun <T> T.TextButton(
    attach: Boolean = true,
    init: TextButton.() -> Unit
) where T : IWithContext,
        T : ViewGroup = TextButton(ctx) child@{

    if (attach)
        this@TextButton.addView(this@child)

    init()
}

open class TextButton(ctx: RimuContext, init: TextButton.() -> Unit) : TextView(ctx, {})
{

    override val dimensions = super.dimensions.apply {

        fontSize = 12
        cornerRadius = 12f
        padding(12, 8)
    }

    override val rules = super.rules.apply {

        backgroundColor = "accentColor"
        fontColor = "accentColor"
        fontColorFactor = 0.1f
    }

    init
    {
        gravity = Gravity.CENTER

        init()
    }
}