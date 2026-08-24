package com.reco1l.verktex.ui.container

import com.reco1l.verktex.ui.UIComponent
import com.reco1l.toolkt.kotlin.*
import com.reco1l.verktex.data.Vec4
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import javax.microedition.khronos.opengles.*


inline fun UIContainer.constraintContainer(builder: UIConstraintContainer.() -> Unit): UIConstraintContainer {
    return UIConstraintContainer().apply(builder).also(::plusAssign)
}

/**
 * Container that allows to constrain nested entities to other entities in the same container.
 *
 * This is useful for creating complex layouts.
 */
open class UIConstraintContainer : UIContainer() {

    private val constraints = mutableMapOf<UIComponent, UIComponent>()


    override fun onLayout(bounds: Vec4) {
        for (child in this) {

            val target = constraints[child] ?: this
            val targetBounds = Vec4(
                target.bounds.x,
                target.bounds.y,
                target.bounds.width,
                target.bounds.height
            )

            child.layout(targetBounds)
        }
    }

    /**
     * Adds a constraint to a child.
     */
    fun addConstraint(child: UIComponent, target: UIComponent) {

        if (child == target) {
            throw IllegalArgumentException("Cannot constrain a child to itself.")
        }

        if (child == this) {
            throw IllegalArgumentException("Cannot constrain the container itself. Use anchorX and anchorY child's properties instead.")
        }

        if (target !in this) {
            throw IllegalArgumentException("The target must be a child of the container.")
        }

        constraints[child] = target
    }

    /**
     * Removes a constraint from a child.
     */
    fun removeConstraint(child: UIComponent?) {
        constraints.remove(child ?: return)
    }


    override fun onChildDetached(child: UIComponent) {
        removeConstraint(child)
        super.onChildDetached(child)
    }

}