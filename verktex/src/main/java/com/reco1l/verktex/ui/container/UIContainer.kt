package com.reco1l.verktex.ui.container

import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.worker.RunOnDrawWorker
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.data.LayoutConstraints
import kotlin.math.*


inline fun UIContainer.container(builder: UIContainer.() -> Unit): UIContainer {
    return UIContainer().apply(builder).also(::plusAssign)
}

/**
 * A container entity that can hold other entities as children. It is responsible for measuring and
 * drawing its children, as well as propagating invalidations to them.
 */
open class UIContainer : UIComponent(), MutableIterable<UIComponent> {

    /**
     * The list of child entities attached to this entity. It is not recommended to modify this list
     * directly, as it may lead to unexpected behavior.
     *
     * Use the provided methods to add or remove children instead.
     */
    protected val children: MutableList<UIComponent> = mutableListOf()


    init {
        width = Dimension.WrapContent
        height = Dimension.WrapContent
    }


    final override fun layout(assignedBounds: Vec4) {

        // Whether this container needs or not re-layout, descendants that are marked as invalid
        // still needs to be re-layout.
        if (!layoutInvalid) {
            val hasInvalidDescendants = children.any(UIComponent::layoutInvalid)

            if (hasInvalidDescendants) {
                onLayout(bounds)
            }
        }

        super.layout(assignedBounds)
    }


    //region Attachment

    /**
     * Called when a child is attached to this container.
     */
    open fun onChildAttached(child: UIComponent) {}

    /**
     * Called when a child is detached from this container.
     */
    open fun onChildDetached(child: UIComponent) {}

    //endregion

    //region Measurement

    override fun onMeasure(
        parentConstraints: LayoutConstraints,
        contentConstraints: LayoutConstraints
    ): Vec2 {

        var measuredWidth = 0f
        var measuredHeight = 0f

        forEach { child ->
            val childMeasuredSize = child.measure(contentConstraints)

            measuredWidth = max(measuredWidth, child.x.resolve(contentConstraints.availableWidth) + childMeasuredSize.x)
            measuredHeight = max(measuredHeight, child.y.resolve(contentConstraints.availableHeight) + childMeasuredSize.y)
        }

        val measuredSize = Vec2(measuredWidth, measuredHeight).expand(padding.toVec4())

        return parentConstraints.constrain(measuredSize)
    }

    //endregion

    //region Input

    override fun onInputEvent(event: InputEvent): Boolean {
        return propagateConsumable { child -> child.onInputEvent(event) }
    }

    //endregion

    //region Drawing

    override fun calculateTransformations() {
        super.calculateTransformations()
        propagate { it.calculateTransformations() }
    }


    /**
     * Draw a child component.
     *
     * This method can be overridden to customize the drawing behavior of child components.
     */
    protected open fun onDrawChild(renderer: GraphicsRenderer, child: UIComponent) {
        child.draw(renderer)
    }


    override fun onDraw(renderer: GraphicsRenderer) {
        for (child in children) {
            onDrawChild(renderer, child)
        }
    }

    //endregion

    //region Child management

    /**
     * Removes all children from this component and detaches them.
     */
    @RunOnDrawWorker
    fun clearChildren() {
        children.forEach { child ->
            child.parent = null
            child.onDetached()
        }
        children.clear()
    }

    /**
     * Adds a child to this container and attaches it.
     */
    @RunOnDrawWorker
    operator fun plusAssign(child: UIComponent) {
        children.add(child)
        child.parent = this
        child.onAttached()
    }

    /**
     * Removes a child from this container and detaches it.
     */
    @RunOnDrawWorker
    operator fun minusAssign(child: UIComponent) {
        children.remove(child)
        child.parent = null
        child.onDetached()
    }

    /**
     * Adds a child to this container and attaches it.
     */
    @RunOnDrawWorker
    operator fun UIComponent.unaryPlus() {
        this@UIContainer += this
    }

    /**
     * Removes a child from this container and detaches it.
     */
    @RunOnDrawWorker
    operator fun UIComponent.unaryMinus() {
        this@UIContainer -= this
    }


    /**
     * Returns the child at the specified index.
     */
    operator fun get(index: Int): UIComponent {
        return children[index]
    }

    /**
     * Whether this container contains the specified child.
     */
    operator fun contains(child: UIComponent): Boolean {
        return children.contains(child)
    }

    /**
     * Sets the child at the specified index to the given child, detaching the old child and attaching the new one.
     */
    @RunOnDrawWorker
    operator fun set(index: Int, child: UIComponent) {
        val oldChild = children[index]
        oldChild.parent = null
        oldChild.onDetached()

        children[index] = child
        child.parent = this
        child.onAttached()
    }


    override fun iterator() = children.iterator()

    //endregion

    //region Utils

    /**
     * Moves the specified child to the front of the drawing order, making it appear above all other children.
     */
    @RunOnDrawWorker
    fun UIComponent.moveToFront() {
        if (this.parent != this@UIContainer) {
            throw IllegalArgumentException("The component is not a child of this container.")
        }
        children.remove(this)
        children.add(this)
    }

    /**
     * Moves the specified child to the back of the drawing order, making it appear below all other children.
     */
    @RunOnDrawWorker
    fun UIComponent.moveToBack() {
        if (this.parent != this@UIContainer) {
            throw IllegalArgumentException("The component is not a child of this container.")
        }
        children.remove(this)
        children.add(0, this)
    }


    /**
     * Propagates the given action to this component and all of its children recursively.
     */
    fun propagate(action: (UIComponent) -> Unit) {
        action(this)
        children.forEach { child ->
            if (child is UIContainer) {
                child.propagate(action)
            } else {
                action(child)
            }
        }
    }

    /**
     * Propagates the given action to this component and all of its children recursively, stopping
     * if the action returns true for any component.
     */
    fun propagateConsumable(action: (UIComponent) -> Boolean): Boolean {
        if (action(this)) return true
        children.forEach { child ->
            if (child is UIContainer) {
                if (child.propagateConsumable(action)) return true
            } else {
                if (action(this)) return true
            }
        }
        return false
    }

    //endregion

}


