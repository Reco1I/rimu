package com.reco1l.rimu.ui.views

import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.IScalable
import com.reco1l.rimu.ui.IScalableWithDimensions
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.rimu.ui.ISkinnableWithRules
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

open class ConstraintLayout(override val ctx: MainContext) :
    AndroidConstraintLayout(ctx),
    IWithContext,
    ISkinnableWithRules<ConstraintLayout, ViewSkinningRules<ConstraintLayout>>,
    IScalableWithDimensions<ConstraintLayout, ViewDimensions<ConstraintLayout>>
{

    override val dimensions by lazy { ViewDimensions<ConstraintLayout>() }

    override val rules by lazy { ViewSkinningRules<ConstraintLayout>() }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        invalidateSkin()
        invalidateScale()
    }

    override fun onViewAdded(view: View)
    {
        super.onViewAdded(view)

        if (view is IWithContext && view is ISkinnable && view is IScalable) view.doOnAttach {

            view.invalidateScale()
            view.invalidateSkin()
        }
    }
}


// LinearLayout

fun IWithContext.LinearLayout(
    parent: ViewGroup? = this as? ViewGroup,
    init: LinearLayout.() -> Unit
) = LinearLayout(ctx).apply {
    parent?.addView(this)
    init()
}

open class LinearLayout(override val ctx: MainContext) :
    AndroidLinearLayout(ctx),
    IWithContext,
    ISkinnableWithRules<LinearLayout, ViewSkinningRules<LinearLayout>>,
    IScalableWithDimensions<LinearLayout, ViewDimensions<LinearLayout>>
{

    override val dimensions by lazy { ViewDimensions<LinearLayout>() }

    override val rules by lazy { ViewSkinningRules<LinearLayout>() }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        invalidateSkin()
        invalidateScale()
    }


    override fun onViewAdded(view: View)
    {
        super.onViewAdded(view)

        if (view is IWithContext) view.doOnAttach {

            if (view is IScalable)
                view.invalidateScale()

            if (view is ISkinnable)
                view.invalidateSkin()
        }
    }
}