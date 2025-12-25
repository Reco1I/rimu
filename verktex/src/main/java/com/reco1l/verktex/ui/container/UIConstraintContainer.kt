package com.reco1l.verktex.ui.container

import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.data.Vec4


inline fun UIContainer.constraintContainer(builder: UIConstraintContainer.() -> Unit): UIConstraintContainer {
    return UIConstraintContainer().apply(builder).also(::plusAssign)
}

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * A constrained child is laid out inside the bounds of its target instead of the container, so its
 * anchor, origin and position are relative to the target. This is useful for creating complex layouts.
 */
open class UIConstraintContainer : UIContainer() {

    private val constraints = mutableMapOf<UIComponent, UIComponent>()


    override fun onLayout(bounds: Vec4) {
        val contentBounds = contentBounds(bounds)
        val laidOut = mutableSetOf<UIComponent>()

        // Targets are laid out before the children constrained to them, so they use this frame's bounds.
        fun layoutChild(child: UIComponent) {
            if (!laidOut.add(child)) {
                return
            }

            val target = constraints[child]

            if (target != null) {
                layoutChild(target)
                child.layout(target.bounds)
            } else {
                child.layout(contentBounds)
            }
        }

        forEach(::layoutChild)
    }

    /**
     * Adds a constraint to a child.
     */
    fun addConstraint(child: UIComponent, target: UIComponent) {

        if (child == target) {
            throw IllegalArgumentException("Cannot constrain a child to itself.")
        }

        if (child !in this || target !in this) {
            throw IllegalArgumentException("Both the child and the target must be children of the container.")
        }

        var next: UIComponent? = target
        while (next != null) {
            if (next == child) {
                throw IllegalArgumentException("Cannot constrain a child to a target that is constrained to it.")
            }
            next = constraints[next]
        }

        constraints[child] = target
        invalidateLayout()
    }

    /**
     * Removes a constraint from a child.
     */
    fun removeConstraint(child: UIComponent?) {
        constraints.remove(child ?: return)
        invalidateLayout()
    }


    override fun onChildDetached(child: UIComponent) {
        removeConstraint(child)
        // Children constrained to the detached one fall back to the container.
        constraints.values.removeAll { it == child }
        super.onChildDetached(child)
    }

}
