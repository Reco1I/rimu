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

/**
 * Set the constraints of a [View].
 * The parent of this view needs to be a [ConstraintLayout] otherwise it'll throw [UnsupportedOperationException].
 *
 * The [target] will be the source for the constraints, this means for example if we pass [Anchor.RIGHT]
 * to [leftToTarget] it will constraint the left anchor of this view to the target right anchor.
 */
fun View.setConstraints(
    target: View = parent as View,
    @BasicAnchor leftToTarget: Int? = null,
    @BasicAnchor topToTarget: Int? = null,
    @BasicAnchor rightToTarget: Int? = null,
    @BasicAnchor bottomToTarget: Int? = null,
)
{
    val parent = parent as? ConstraintLayout
        ?: throw UnsupportedOperationException("This view needs to be attached to a ConstraintLayout.")

    this.ensureID()
    parent.ensureID()
    target.ensureID()

    parent.constraintSet.apply {

        leftToTarget?.also { connect(id, Anchor.LEFT, target.id, it) }
        topToTarget?.also { connect(id, Anchor.TOP, target.id, it) }
        rightToTarget?.also { connect(id, Anchor.RIGHT, target.id, it) }
        bottomToTarget?.also { connect(id, Anchor.BOTTOM, target.id, it) }

        applyTo(parent)
    }
}