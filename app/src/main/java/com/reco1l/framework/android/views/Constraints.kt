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
    @BasicAnchor leftToTarget: Int? = null,
    @BasicAnchor topToTarget: Int? = null,
    @BasicAnchor rightToTarget: Int? = null,
    @BasicAnchor bottomToTarget: Int? = null,
)
{
    val parent = parent as? ConstraintLayout
        ?: throw UnsupportedOperationException("This view must be attached to a ConstraintLayout.")

    this.ensureID()
    parent.ensureID()
    target.ensureID()

    parent.constraintSet.apply {

        if (leftToTarget != null)
            connect(id, Anchor.LEFT, target.id, leftToTarget)

        if (topToTarget != null)
            connect(id, Anchor.TOP, target.id, topToTarget)

        if (rightToTarget != null)
            connect(id, Anchor.RIGHT, target.id, rightToTarget)

        if (bottomToTarget != null)
            connect(id, Anchor.BOTTOM, target.id, bottomToTarget)

        applyTo(parent)
    }
}