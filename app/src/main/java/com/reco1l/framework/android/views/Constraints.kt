@file:JvmName("Constraints")

package com.reco1l.framework.android.views

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.reco1l.framework.graphics.Anchor
import com.reco1l.framework.graphics.BasicAnchor


val ConstraintLayout.constraintSet: ConstraintSet
    get()
    {
        ensureID()
        return ConstraintSet().apply {

            isForceId = false
            clone(this@constraintSet)
        }
    }


fun View.setConstraints(
    target: View = parent as View,
    @BasicAnchor left: Int? = null,
    @BasicAnchor top: Int? = null,
    @BasicAnchor right: Int? = null,
    @BasicAnchor bottom: Int? = null,
)
{
    val parent = parent as? ConstraintLayout
        ?: throw UnsupportedOperationException("This view must be attached to a ConstraintLayout.")

    this.ensureID()
    parent.ensureID()
    target.ensureID()

    parent.constraintSet.apply {

        if (left != null)
            connect(id, Anchor.LEFT, target.id, left)

        if (top != null)
            connect(id, Anchor.TOP, target.id, top)

        if (right != null)
            connect(id, Anchor.RIGHT, target.id, right)

        if (bottom != null)
            connect(id, Anchor.BOTTOM, target.id, bottom)

        applyTo(parent)
    }
}