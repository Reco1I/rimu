package game.rimu.ui.views

import android.view.ViewGroup
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.views.addons.IScalableWithDimensions
import game.rimu.ui.views.addons.ISkinnable
import game.rimu.ui.views.addons.ViewDimensions
import android.widget.LinearLayout as AndroidLinearLayout
import android.widget.RelativeLayout as AndroidRelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout as AndroidConstraintLayout


// ConstraintLayout

open class ConstraintLayout(override val ctx: RimuContext) :
    AndroidConstraintLayout(ctx),
    IWithContext,
    ISkinnable,
    IScalableWithDimensions<ViewDimensions>
{
    final override val dimensions = ViewDimensions()
}

inline fun <T> T.ConstraintLayout(
    attach: Boolean = true,
    block: ConstraintLayout.() -> Unit
) where T : ViewGroup, T : IWithContext = ConstraintLayout(ctx).also {

    if (attach)
        addView(it)

    it.block()
}



// LinearLayout

open class LinearLayout(override val ctx: RimuContext) :
    AndroidLinearLayout(ctx),
    IWithContext,
    ISkinnable,
    IScalableWithDimensions<ViewDimensions>
{
    final override val dimensions = ViewDimensions()
}

inline fun <T> T.LinearLayout(
    attach: Boolean = true,
    block: LinearLayout.() -> Unit
) where T : ViewGroup, T : IWithContext = LinearLayout(ctx).also {

    if (attach)
        addView(it)

    it.block()
}



// RelativeLayout

open class RelativeLayout(override val ctx: RimuContext) :
    AndroidRelativeLayout(ctx),
    IWithContext,
    ISkinnable,
    IScalableWithDimensions<ViewDimensions>
{
    final override val dimensions = ViewDimensions()
}

inline fun <T> T.RelativeLayout(
    attach: Boolean = true,
    block: RelativeLayout.() -> Unit
) where T : ViewGroup, T : IWithContext = RelativeLayout(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

