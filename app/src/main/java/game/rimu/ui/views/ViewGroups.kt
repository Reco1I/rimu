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

open class ConstraintLayout(override val ctx: RimuContext) :
    AndroidConstraintLayout(ctx),
    IWithContext,
    ISkinnableWithRules<ConstraintLayout, ViewSkinningRules<ConstraintLayout>>,
    IScalableWithDimensions<ConstraintLayout, ViewDimensions<ConstraintLayout>>
{
    override val dimensions by lazy { ViewDimensions<ConstraintLayout>() }

    override val skinningRules by lazy { ViewSkinningRules<ConstraintLayout>() }
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
    ISkinnableWithRules<LinearLayout, ViewSkinningRules<LinearLayout>>,
    IScalableWithDimensions<LinearLayout, ViewDimensions<LinearLayout>>
{
    override val dimensions by lazy { ViewDimensions<LinearLayout>() }

    override val skinningRules by lazy { ViewSkinningRules<LinearLayout>() }
}

inline fun <T> T.LinearLayout(
    attach: Boolean = true,
    block: LinearLayout.() -> Unit
) where T : ViewGroup, T : IWithContext = LinearLayout(ctx).also {

    if (attach)
        addView(it)

    it.block()
}
