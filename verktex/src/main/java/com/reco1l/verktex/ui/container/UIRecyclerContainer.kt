package com.reco1l.verktex.ui.container

import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIComponent
import kotlin.math.max

/**
 * A scrollable container that only keeps components for the items of its [data] that are visible,
 * reusing the ones that stop being visible.
 *
 * The size of an item is only known once it has been bound, until then it is estimated from the last
 * measured one.
 */
open class UIRecyclerContainer<T : Any, C : RecyclableComponent<T>>(private val initialPoolSize: Int = 8) : UIScrollableContainer() {

    /**
     * The components currently bound to an item. Those are the ones inside of the container's visible area
     * and the ones that are not recyclable, determined by [RecyclableComponent.isRecyclable].
     */
    val boundComponents: Map<T, C>
        field = HashMap()

    /**
     * The wrapper holding the bound components, it is the only child of this container.
     */
    val componentWrapper = RecyclerComponentWrapper()

    /**
     * The component supplier.
     */
    var onCreateComponent: (() -> C)? = null
        set(value) {
            field = value
            resetComponents()
        }

    /**
     * The items to bind to the components.
     */
    var data: List<T> = emptyList()
        set(value) {
            field = value
            unbindAll()

            itemIndices.clear()
            value.forEachIndexed { index, item -> itemIndices[item] = index }

            itemSizes = FloatArray(value.size) { estimatedItemSize }
            componentWrapper.invalidateMeasure()
        }

    /**
     * The direction in which the items are placed. The container scrolls along it.
     */
    var orientation = UILinearContainer.Orientation.Vertical
        set(value) {
            if (field == value) return
            field = value
            scrollAxes = scrollAxisOf(value)
            componentWrapper.invalidateMeasure()
        }

    /**
     * The spacing between items.
     */
    var spacing: Dimension.Fixed = Dimension.Zero
        set(value) {
            if (field == value) return
            field = value
            componentWrapper.invalidateMeasure()
        }


    private val componentPool = ArrayDeque<C>()
    private val itemIndices = HashMap<T, Int>()

    private var itemSizes = FloatArray(0)
    private var estimatedItemSize = 0f


    init {
        // Scrolling on both axes would measure the items with an unbounded cross axis.
        scrollAxes = scrollAxisOf(orientation)
        +componentWrapper
    }


    private fun scrollAxisOf(orientation: UILinearContainer.Orientation) =
        if (orientation == UILinearContainer.Orientation.Vertical) Axis.Y else Axis.X


    private fun resetComponents() {
        unbindAll()
        componentPool.clear()

        repeat(initialPoolSize) {
            createComponent()?.let(componentPool::addLast)
        }
    }

    private fun createComponent() = onCreateComponent?.invoke()

    private fun bindComponent(item: T): C? {
        boundComponents[item]?.let { return it }

        val component = componentPool.removeFirstOrNull() ?: createComponent() ?: return null

        component.boundData = item
        component.onBind(item)

        boundComponents[item] = component
        componentWrapper += component
        return component
    }

    private fun unbindComponent(item: T, component: C) {
        boundComponents.remove(item)
        component.removeSelf()

        component.boundData = null
        component.onRecycle()

        componentPool.addLast(component)
    }

    private fun unbindAll() {
        boundComponents.toList().forEach { (item, component) -> unbindComponent(item, component) }
    }

    /**
     * Binds the items entering the visible area and unbinds the ones that left it.
     */
    private fun updateBoundComponents() {
        // Until an item is measured every size is zero and all of them would seem visible, so only the
        // first one is bound to learn the size.
        if (estimatedItemSize == 0f) {
            data.firstOrNull()?.let(::bindComponent)
            return
        }

        val isVertical = orientation == UILinearContainer.Orientation.Vertical
        val spacingPixels = spacing.toPixels()

        val visibleStart = if (isVertical) scrollY else scrollX
        val visibleEnd = visibleStart + if (isVertical) bounds.height else bounds.width

        var position = if (isVertical) componentWrapper.paddingTop.toPixels() else componentWrapper.paddingLeft.toPixels()

        data.forEachIndexed { index, item ->
            val size = itemSizes[index]
            val isInBounds = position + size > visibleStart && position < visibleEnd

            if (isInBounds) {
                bindComponent(item)
            } else {
                boundComponents[item]?.let { component ->
                    if (component.isRecyclable) {
                        unbindComponent(item, component)
                    }
                }
            }

            position += size + spacingPixels
        }
    }

    /**
     * Binds a component to the given item and runs the block on it, so it isn't recycled until it goes out
     * of the visible area again.
     */
    fun applyComponent(item: T, block: C.(item: T) -> Unit): C? {
        val component = bindComponent(item) ?: return null
        component.block(item)
        return component
    }

    /**
     * The position of the given item along the scrolling axis, from the start of the content,
     * following the last measured size of every item. Null if the item isn't in [data].
     */
    fun positionOf(item: T): Float? {
        val index = itemIndices[item] ?: return null
        val isVertical = orientation == UILinearContainer.Orientation.Vertical

        var position = if (isVertical) componentWrapper.paddingTop.toPixels() else componentWrapper.paddingLeft.toPixels()
        for (i in 0 until index) {
            position += itemSizes[i]
        }
        return position + spacing.toPixels() * index
    }

    override fun onUpdate(clock: FrameClock) {
        updateBoundComponents()
        super.onUpdate(clock)
    }


    /**
     * The only child of a [UIRecyclerContainer], places the bound components following the size of every item.
     */
    inner class RecyclerComponentWrapper : UIContainer() {

        init {
            width = Dimension.FillAvailable
        }

        override fun onMeasure(parentConstraints: LayoutConstraints, contentConstraints: LayoutConstraints): Vec2 {
            val isVertical = orientation == UILinearContainer.Orientation.Vertical
            val childConstraints = contentConstraints.loosen()

            for ((item, component) in boundComponents) {
                val index = itemIndices[item] ?: continue
                val size = component.measure(childConstraints.withoutFillLimits(component))

                itemSizes[index] = if (isVertical) size.y else size.x

                // Items that were never measured take the first known size.
                if (estimatedItemSize == 0f) {
                    estimatedItemSize = itemSizes[index]
                    itemSizes.indices.forEach { if (itemSizes[it] == 0f) itemSizes[it] = estimatedItemSize }
                }
            }

            val itemsSize = itemSizes.sum() + spacing.toPixels() * max(0, itemSizes.size - 1)

            val contentSize = if (isVertical) {
                Vec2(contentConstraints.availableWidth, itemsSize)
            } else {
                Vec2(itemsSize, contentConstraints.availableHeight)
            }

            return parentConstraints.constrain(contentSize.expand(padding.toVec4()))
        }

        override fun onLayout(bounds: Vec4) {
            val isVertical = orientation == UILinearContainer.Orientation.Vertical
            val spacingPixels = spacing.toPixels()

            var position = if (isVertical) paddingTop.toPixels() else paddingLeft.toPixels()

            data.forEachIndexed { index, item ->
                boundComponents[item]?.layout(
                    if (isVertical) {
                        Vec4(paddingLeft.toPixels(), position, bounds.width - paddingRight.toPixels(), position + itemSizes[index])
                    } else {
                        Vec4(position, paddingTop.toPixels(), position + itemSizes[index], bounds.height - paddingBottom.toPixels())
                    }
                )

                position += itemSizes[index] + spacingPixels
            }
        }

        // Children sizes change while being animated, and those aren't always known from the outside.
        override fun onUpdate(clock: FrameClock) {
            invalidateMeasure()
            super.onUpdate(clock)
        }
    }
}

/**
 * A component of a [UIRecyclerContainer], it gets bound to an item while it is visible.
 */
abstract class RecyclableComponent<T : Any> : UIContainer() {

    /**
     * The item this component is bound to. It is set by the [UIRecyclerContainer].
     */
    var boundData: T? = null
        internal set

    /**
     * Whether this component is bound to an item.
     */
    val isBound
        get() = boundData != null

    /**
     * Whether this component can be recycled.
     */
    abstract val isRecyclable: Boolean

    /**
     * Called when this component is bound to an item.
     */
    abstract fun onBind(data: T)

    /**
     * Called when this component is recycled, usually used to reset its state.
     */
    open fun onRecycle() = Unit

    override fun onInputEvent(event: InputEvent) = isBound && super.onInputEvent(event)
}
