package com.reco1l.verktex.ui.container

import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.worker.RunOnDrawWorker
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.Theme
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

    /**
     * The number of children in this container.
     */
    val childCount
        get() = children.size

    /**
     * Whether this container lays out its children only when something changed: a measure pass
     * (size, position or children), an invalidated layout below it or other bounds given by its
     * parent. Otherwise it lays out every frame.
     *
     * Only for content that stays still, children positioned by an ongoing layout (like a
     * scrolling container) would stop moving.
     */
    var staticLayout = false
        set(value) {
            if (field == value) return
            field = value
            invalidateLayout()
        }

    override val isLayoutStatic
        get() = staticLayout


    init {
        width = Dimension.WrapContent
        height = Dimension.WrapContent
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

        val childConstraints = contentConstraints.loosen()

        forEach { child ->
            val childMeasuredSize = child.measure(childConstraints.withoutFillLimits(child))

            measuredWidth = max(measuredWidth, child.x.resolve(contentConstraints.availableWidth) + childMeasuredSize.x)
            measuredHeight = max(measuredHeight, child.y.resolve(contentConstraints.availableHeight) + childMeasuredSize.y)
        }

        // The own constraints, so the limits of this container (e.g. minWidth) are what filling
        // children are stretched to.
        val measuredSize = contentConstraints.constrain(Vec2(measuredWidth, measuredHeight)).expand(padding.toVec4())
        stretchFillingChildren(measuredSize) { childConstraints }
        return measuredSize
    }

    /**
     * Whether [child] fills the width while this container wraps its own. Such a child would take
     * all the available space, so it is measured by its content first (see [withoutFillLimits]) and
     * stretched to the container afterwards (see [stretchFillingChildren]).
     */
    protected open fun fillsWrappedWidth(child: UIComponent) =
        width is Dimension.ContentSizeFraction && child.width is Dimension.AvailableSizeFraction

    /**
     * Same as [fillsWrappedWidth] for the height.
     */
    protected open fun fillsWrappedHeight(child: UIComponent) =
        height is Dimension.ContentSizeFraction && child.height is Dimension.AvailableSizeFraction

    /**
     * Removes the limits of the axes [child] fills while this container wraps them.
     */
    protected fun LayoutConstraints.withoutFillLimits(child: UIComponent) = copy(
        maxWidth = if (fillsWrappedWidth(child)) Float.POSITIVE_INFINITY else maxWidth,
        maxHeight = if (fillsWrappedHeight(child)) Float.POSITIVE_INFINITY else maxHeight
    )

    /**
     * Measures again the children filling a wrapped axis, now limited to the content area of the
     * [measuredSize] of this container.
     */
    protected fun stretchFillingChildren(measuredSize: Vec2, constraintsFor: (UIComponent) -> LayoutConstraints) {
        val contentWidth = measuredSize.x - paddingLeft.toPixels() - paddingRight.toPixels()
        val contentHeight = measuredSize.y - paddingTop.toPixels() - paddingBottom.toPixels()

        forEach { child ->
            val fillsWidth = fillsWrappedWidth(child)
            val fillsHeight = fillsWrappedHeight(child)

            if (fillsWidth || fillsHeight) {
                val constraints = constraintsFor(child)

                child.measure(constraints.copy(
                    maxWidth = if (fillsWidth) contentWidth else constraints.maxWidth,
                    maxHeight = if (fillsHeight) contentHeight else constraints.maxHeight
                ))
            }
        }
    }

    override fun onLayout(bounds: Vec4) {
        val contentBounds = contentBounds(bounds)
        forEach { child -> child.layout(contentBounds) }
    }

    /**
     * Returns the area children are laid out in. It is in local space, inside the [bounds] minus
     * the padding.
     */
    protected fun contentBounds(bounds: Vec4) = Vec4(
        x = paddingLeft.toPixels(),
        y = paddingTop.toPixels(),
        z = bounds.width - paddingRight.toPixels(),
        w = bounds.height - paddingBottom.toPixels()
    )

    //endregion

    //region Input

    override fun onInputEvent(event: InputEvent): Boolean {
        // Topmost children (drawn last) get the event first.
        for (child in children.asReversed()) {

            // Only presses go to the children under the pointer. Every other event goes to all of
            // them, so a pressed component keeps following its pointer after it left its bounds.
            if (event is PointerEvent
                && event.action == PointerEvent.Action.Down
                && !child.containsScenePoint(event.x, event.y)) {
                continue
            }

            if (child.onInputEvent(event)) {
                return true
            }
        }
        return false
    }

    //endregion

    //region Style

    override fun onStyle(theme: Theme) {
        super.onStyle(theme)
        // Children styles can depend on this container's state (e.g. a button's density).
        children.forEach { it.style() }
    }

    //endregion

    //region Update

    override fun onUpdate(clock: FrameClock) {
        // Indexed loop, children can be added or removed while updating.
        var i = 0
        while (i < children.size) {
            children.getOrNull(i)?.update(clock)
            i++
        }
    }

    //endregion

    //region Drawing

    override fun calculateTransformations() {
        super.calculateTransformations()
        children.forEach { it.calculateTransformations() }
    }


    /**
     * Draw a child component.
     *
     * This method can be overridden to customize the drawing behavior of child components.
     */
    protected open fun onDrawChild(renderer: GraphicsRenderer, child: UIComponent) {
        child.draw(renderer)
    }


    /**
     * The index of the first child to draw, the ones before it aren't drawn.
     */
    protected open fun firstDrawnChildIndex() = 0

    override fun onDraw(renderer: GraphicsRenderer) {
        val clip = renderer.clipBounds

        if (tintChildren) renderer.pushTint(color)

        for (index in firstDrawnChildIndex() until children.size) {
            val child = children[index]

            // Children fully outside the visible area cannot be seen, so they are not drawn.
            if (!child.intersectsSceneRect(clip.x, clip.y, clip.z, clip.w)) {
                continue
            }
            onDrawChild(renderer, child)
        }

        if (tintChildren) renderer.popTint()
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
        invalidateMeasure()
    }

    /**
     * Adds a child to this container and attaches it.
     */
    @RunOnDrawWorker
    operator fun plusAssign(child: UIComponent) {
        children.add(child)
        child.parent = this
        child.onAttached()
        invalidateMeasure()
    }

    /**
     * Adds a child to this container at the specified index and attaches it.
     */
    @RunOnDrawWorker
    fun add(index: Int, child: UIComponent) {
        children.add(index, child)
        child.parent = this
        child.onAttached()
        invalidateMeasure()
    }

    /**
     * Adds a child behind every other child of this container and attaches it.
     */
    @RunOnDrawWorker
    fun addToBack(child: UIComponent) = add(0, child)

    /**
     * Removes a child from this container and detaches it.
     */
    @RunOnDrawWorker
    operator fun minusAssign(child: UIComponent) {
        children.remove(child)
        child.parent = null
        child.onDetached()
        invalidateMeasure()
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
        invalidateMeasure()
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
        // The order can matter to the layout, e.g. in a linear container.
        this@UIContainer.invalidateLayout()
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
        this@UIContainer.invalidateLayout()
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
                if (action(child)) return true
            }
        }
        return false
    }

    //endregion

}


