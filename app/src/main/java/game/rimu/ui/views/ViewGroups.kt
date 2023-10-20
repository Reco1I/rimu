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

fun IWithContext.ConstraintLayout(
    parent: ViewGroup? = this as? ViewGroup,
    init: ConstraintLayout.() -> Unit
) = ConstraintLayout(ctx).apply {
    parent?.addView(this)
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

fun IWithContext.LinearLayout(
    parent: ViewGroup? = this as? ViewGroup,
    init: LinearLayout.() -> Unit
) = LinearLayout(ctx).apply {
    parent?.addView(this)
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