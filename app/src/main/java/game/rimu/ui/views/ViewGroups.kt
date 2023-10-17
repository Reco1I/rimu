package game.rimu.ui.views

import android.view.ViewGroup
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.ViewDimensions
import game.rimu.ui.ViewSkinningRules
import android.widget.LinearLayout as AndroidLinearLayout
import androidx.constraintlayout.widget.ConstraintLayout as AndroidConstraintLayout


// ConstraintLayout

fun <T> T.ConstraintLayout(
    attach: Boolean = true,
    init: ConstraintLayout.() -> Unit
) where T : IWithContext,
        T : ViewGroup = ConstraintLayout(ctx) child@{

    if (attach)
        this@ConstraintLayout.addView(this@child)

    init()
}

open class ConstraintLayout(override val ctx: RimuContext, init: ConstraintLayout.() -> Unit) :
    AndroidConstraintLayout(ctx),
    IWithContext,
    ISkinnableWithRules<ConstraintLayout>,
    IScalableWithDimensions<ConstraintLayout>
{
    override val dimensions by lazy { ViewDimensions<ConstraintLayout>() }

    override val rules by lazy { ViewSkinningRules<ConstraintLayout>() }

    init { init() }
}


// LinearLayout

fun <T> T.LinearLayout(
    attach: Boolean = true,
    init: LinearLayout.() -> Unit
) where T : IWithContext,
        T : ViewGroup = LinearLayout(ctx) child@{

    if (attach)
        this@LinearLayout.addView(this@child)

    init()
}

open class LinearLayout(override val ctx: RimuContext, init: LinearLayout.() -> Unit) :
    AndroidLinearLayout(ctx),
    IWithContext,
    ISkinnableWithRules<LinearLayout>,
    IScalableWithDimensions<LinearLayout>
{
    override val dimensions by lazy { ViewDimensions<LinearLayout>() }

    override val rules by lazy { ViewSkinningRules<LinearLayout>() }

    init { init() }
}