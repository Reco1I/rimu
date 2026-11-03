package com.reco1l.verktex.ui.container

import androidx.annotation.*
import com.reco1l.verktex.*
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.input.ScrollEvent
import com.reco1l.verktex.input.VelocityTracker
import com.reco1l.verktex.math.almostEquals
import com.reco1l.verktex.math.almostZero
import com.reco1l.verktex.time.FrameClock
import kotlin.math.*


inline fun UIContainer.scrollableContainer(builder: UIScrollableContainer.() -> Unit): UIScrollableContainer {
    return UIScrollableContainer().apply(builder).also(::plusAssign)
}

open class UIScrollableContainer : UIContainer() {

    /**
     * Determines which axes can be scrolled by the user.
     */
    var scrollAxes: Axis = Axis.Both
        set(value) {
            if (field == value) return
            field = value
            invalidateMeasure()
        }

    /**
     * Determines which axes can be overflowed by the scroll.
     */
    var overflowAxes: Axis = Axis.Both

    /**
     * The flag to indicate if the container is being scrolled by the user. Container
     * can be scrolled by the user or by itself (e.g. animations).
     */
    var isUserScrolling = false
        private set

    /**
     * Whether to prevent scrolling on this container and all its children.
     */
    var preventScrolling = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    velocityX = 0f
                    velocityY = 0f
                }
            }
        }

    /**
     * Whether the container is scrolling or not.
     */
    val isScrolling
        get() = velocityX != 0f || velocityY != 0f || isUserScrolling

    //region Scrolling properties

    /**
     * The scroll position on the x-axis.
     */
    var scrollX = 0f
        set(value) {
            val scroll = when {
                value.almostZero() -> 0f
                value.almostEquals(maxScrollX) -> maxScrollX
                else -> value.toInt().toFloat()
            }

            if (field != scroll) {
                field = scroll
                onScroll(Axis.X)
            }
        }

    /**
     * The scroll position on the y-axis.
     */
    var scrollY = 0f
        set(value) {
            val scroll = when {
                value.almostZero() -> 0f
                value.almostEquals(maxScrollY) -> maxScrollY
                else -> value.toInt().toFloat()
            }

            if (field != scroll) {
                field = scroll
                onScroll(Axis.Y)
            }
        }

    /**
     * The velocity in px/s on the x-axis.
     */
    var velocityX = 0f
        private set(value) {
            val max = maxVelocity.x * 1f.dip.toPixels()
            val velocity = if (value.almostZero()) 0f else value.coerceIn(-max, max)

            if (field != velocity) {
                field = velocity
                setHierarchyScrollPrevention(velocity != 0f)
            }
        }

    /**
     * The velocity in px/s on the y-axis.
     */
    var velocityY = 0f
        private set(value) {
            val max = maxVelocity.y * 1f.dip.toPixels()
            val velocity = if (value.almostZero()) 0f else value.coerceIn(-max, max)

            if (field != velocity) {
                field = velocity
                setHierarchyScrollPrevention(velocity != 0f)
            }
        }

    /**
     * The fraction of the velocity kept after flinging for one second, for both axes. Lower values
     * stop the content sooner.
     */
    var deceleration = Vec2(DEFAULT_DECELERATION)

    /**
     * The maximum velocity in dip/s for both axes.
     */
    var maxVelocity = Vec2(DEFAULT_MAX_VELOCITY)

    /**
     * The minimum travel distance in px for both axes.
     *
     * This is used to determine if the user has scrolled enough to start
     * scrolling the container.
     */
    var minimumTravel = Vec2(DEFAULT_MINIMUM_TRAVEL)

    //endregion

    //region Indicators

    /**
     * Whether to show the horizontal scroll indicator.
     */
    var showHorizontalIndicator = true

    /**
     * Whether to show the vertical scroll indicator.
     */
    var showVerticalIndicator = true


    //endregion

    //region Dimension properties

    /**
     * The padding for the scrollable content.
     */
    var scrollPadding = Vec2(0f, 0f)

    /**
     * The maximum scroll position on the x-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollX
        get() = max(0f, scrollableContentWidth - bounds.width)

    /**
     * The maximum scroll position on the y-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollY
        get() = max(0f, scrollableContentHeight - bounds.height)

    /**
     * The width of the content that can be scrolled.
     */
    val scrollableContentWidth
        get() = max(0f, contentSize.x) + scrollPadding.x

    /**
     * The height of the content that can be scrolled.
     */
    val scrollableContentHeight
        get() = max(0f, contentSize.y) + scrollPadding.y

    /**
     * The size of the children, measured without limits on the scrollable axes.
     */
    var contentSize = Vec2.Zero
        private set


    override fun onMeasure(parentConstraints: LayoutConstraints, contentConstraints: LayoutConstraints): Vec2 {

        // Children may grow beyond this container along the scrollable axes.
        val childConstraints = contentConstraints.loosen().copy(
            maxWidth = if (scrollAxes.isHorizontal) Float.POSITIVE_INFINITY else contentConstraints.maxWidth,
            maxHeight = if (scrollAxes.isVertical) Float.POSITIVE_INFINITY else contentConstraints.maxHeight
        )

        var contentWidth = 0f
        var contentHeight = 0f

        forEach { child ->
            val childSize = child.measure(childConstraints.withoutFillLimits(child))
            contentWidth = max(contentWidth, childSize.x)
            contentHeight = max(contentHeight, childSize.y)
        }

        contentSize = Vec2(contentWidth, contentHeight)

        // The own constraints, so the limits of this container (e.g. minWidth) are what filling
        // children are stretched to.
        val measuredSize = contentConstraints.constrain(contentSize).expand(padding.toVec4())
        stretchFillingChildren(measuredSize) { childConstraints }
        return measuredSize
    }

    // Scrollable axes are unbounded, so filling them is the same as wrapping.
    override fun fillsWrappedWidth(child: UIComponent) = !scrollAxes.isHorizontal && super.fillsWrappedWidth(child)

    override fun fillsWrappedHeight(child: UIComponent) = !scrollAxes.isVertical && super.fillsWrappedHeight(child)

    override fun onLayout(bounds: Vec4) {
        // Children are offset by the current scroll position.
        val contentBounds = Vec4(
            x = paddingLeft.toPixels() - scrollX,
            y = paddingTop.toPixels() - scrollY,
            z = bounds.width - paddingRight.toPixels() - scrollX,
            w = bounds.height - paddingBottom.toPixels() - scrollY
        )

        forEach { child -> child.layout(contentBounds) }
    }

    //endregion

    private var indicatorYAlpha = 0f
    private var indicatorXAlpha = 0f

    private var lastWheelTimeSec = 0f

    private val velocityTracker = VelocityTracker()

    private var lastPointerX = 0f
    private var lastPointerY = 0f
    private var pointerTravel = 0f
    private var dragX = 0f
    private var dragY = 0f

    // The finger dragging this container, other fingers are left to the children.
    private var dragPointerId = -1


    /**
     * Stops the scrolling of the container by setting the velocity to 0.
     */
    fun stopScroll() {
        velocityX = 0f
        velocityY = 0f
    }


    //region Callbacks

    @CallSuper
    open fun onScroll(axes: Axis) {

        if (axes.isVertical) {
            indicatorYAlpha = 0.5f
        }

        if (axes.isHorizontal) {
            indicatorXAlpha = 0.5f
        }

        invalidateTransformations()
    }

    //endregion

    //region Scrolling

    private fun decelerateProgressively(deltaTimeSec: Float) {
        scrollX -= velocityX * deltaTimeSec
        scrollY -= velocityY * deltaTimeSec

        // Time based, so the content glides the same distance at any frame rate.
        velocityX *= deceleration.x.pow(deltaTimeSec)
        velocityY *= deceleration.y.pow(deltaTimeSec)

        // The decay never reaches zero, stop once the movement isn't noticeable anymore.
        val minimumVelocity = MINIMUM_FLING_VELOCITY.toPixels()
        if (abs(velocityX) < minimumVelocity) velocityX = 0f
        if (abs(velocityY) < minimumVelocity) velocityY = 0f
    }

    private fun handleOverflow() {

        if (scrollX !in 0f..maxScrollX) {
            velocityX = 0f

            if (overflowAxes.isHorizontal) {
                val bounceBack = (if (scrollX < 0f) -scrollX else -(scrollX - maxScrollX)) * 0.2f

                if (abs(bounceBack) < .5f) {
                    scrollX = if (scrollX < 0f) 0f else maxScrollX
                } else {
                    scrollX += bounceBack
                }
            } else {
                scrollX = if (scrollX < 0f) 0f else maxScrollX
            }
        }

        if (scrollY !in 0f..maxScrollY) {
            velocityY = 0f

            if (overflowAxes.isVertical) {
                val bounceBack = (if (scrollY < 0f) -scrollY else -(scrollY - maxScrollY)) * 0.2f

                if (abs(bounceBack) < .5f) {
                    scrollY = if (scrollY < 0f) 0f else maxScrollY
                } else {
                    scrollY += bounceBack
                }
            } else {
                scrollY = if (scrollY < 0f) 0f else maxScrollY
            }
        }
    }

    override fun onUpdate(clock: FrameClock) {
        super.onUpdate(clock)

        if (!isUserScrolling) {
            if (isScrolling) {
                decelerateProgressively(clock.elapsedFrameTime)
            }
            handleOverflow()
        }

        updateIndicators(clock.elapsedFrameTime)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        super.onDraw(renderer)

        val indicatorThickness = INDICATOR_THICKNESS.toPixels()

        if (showVerticalIndicator && (scrollAxes == Axis.Both || scrollAxes == Axis.Y)) {
            val indicatorHeight = bounds.height * (bounds.height / scrollableContentHeight).coerceAtMost(1f)

            renderer.drawRect(
                x = bounds.width - indicatorThickness,
                y = scrollY * (bounds.height / scrollableContentHeight),
                width = indicatorThickness,
                height = indicatorHeight,
                color = Color4.White.copy(alpha = indicatorYAlpha)
            )
        }

        if (showHorizontalIndicator && (scrollAxes == Axis.Both || scrollAxes == Axis.X)) {
            val indicatorWidth = bounds.width * (bounds.width / scrollableContentWidth).coerceAtMost(1f)

            renderer.drawRect(
                x = scrollX * (bounds.width / scrollableContentWidth),
                y = bounds.height - indicatorThickness,
                width = indicatorWidth,
                height = indicatorThickness,
                color = Color4.White.copy(alpha = indicatorXAlpha)
            )
        }
    }

    /**
     * Called when the scroll position has changed.
     */
    protected open fun onScrollChanged() {}

    /**
     * Called to update the scroll indicators based on the current scroll position.
     */
    protected open fun updateIndicators(deltaTimeSec: Float) {

        if (showVerticalIndicator && (scrollAxes == Axis.Both || scrollAxes == Axis.Y)) {
            if (indicatorYAlpha > 0f && velocityY == 0f) {
                indicatorYAlpha = (indicatorYAlpha - deltaTimeSec * 0.75f).coerceAtLeast(0f)
            }
        }

        if (showHorizontalIndicator && (scrollAxes == Axis.Both || scrollAxes == Axis.X)) {
            if (indicatorXAlpha > 0f && velocityX == 0f) {
                indicatorXAlpha = (indicatorXAlpha - deltaTimeSec * 0.75f).coerceAtLeast(0f)
            }
        }
    }

    //endregion

    //region Input

    private fun handleUserScroll(deltaX: Float, deltaY: Float, velocityX: Float, velocityY: Float) {

        val length = hypot(deltaX, deltaY)

        fun decreaseInBoundary(current: Float, delta: Float, max: Float): Float {
            if (current - delta > 0f && current - delta < max) {
                return delta
            }
            return delta * if (length > 0) length.pow(0.7f) / length else 0f
        }

        if (scrollAxes.isHorizontal && !deltaX.almostZero()) {
            this.velocityX = velocityX
            scrollX -= decreaseInBoundary(scrollX, deltaX, maxScrollX)

            if (!overflowAxes.isHorizontal) {
                scrollX = scrollX.coerceIn(0f, maxScrollX)
            }
        }

        if (scrollAxes.isVertical && !deltaY.almostZero()) {
            this.velocityY = velocityY
            scrollY -= decreaseInBoundary(scrollY, deltaY, maxScrollY)

            if (!overflowAxes.isVertical) {
                scrollY = scrollY.coerceIn(0f, maxScrollY)
            }
        }
    }

    override fun onInputEvent(event: InputEvent): Boolean {

        if (preventScrolling || event !is ScrollEvent && event !is PointerEvent) {
            return super.onInputEvent(event)
        }

        val wasUserScrolling = isUserScrolling

        if (event is PointerEvent) {
            val isDragPointer = event.pointerId == dragPointerId

            if (!isDragPointer && (event.action != PointerEvent.Action.Down || dragPointerId != -1)) {
                return super.onInputEvent(event)
            }

            when (event.action) {
                PointerEvent.Action.Down -> {
                    dragPointerId = event.pointerId
                    velocityTracker.clear()
                    velocityTracker.addSample(event.timeMillis(), event.x, event.y)
                    lastPointerX = event.x
                    lastPointerY = event.y
                    pointerTravel = 0f
                    dragX = 0f
                    dragY = 0f
                    stopScroll()
                }

                PointerEvent.Action.Move -> {
                    val deltaX = event.x - lastPointerX
                    val deltaY = event.y - lastPointerY
                    lastPointerX = event.x
                    lastPointerY = event.y
                    pointerTravel += hypot(deltaX, deltaY)
                    dragX += deltaX
                    dragY += deltaY
                    velocityTracker.addSample(event.timeMillis(), event.x, event.y)

                    // Nested scrollable containers get the drag first, this one only takes it over
                    // when none of them is scrolling and it goes along one of its scroll axes.
                    if (!isUserScrolling) {
                        val consumedByChild = super.onInputEvent(event)

                        val isHorizontalDrag = abs(dragX) > abs(dragY)
                        val canScrollAlongDrag = if (isHorizontalDrag) scrollAxes.isHorizontal else scrollAxes.isVertical

                        if (hasScrollingDescendant() || pointerTravel <= minimumTravel.y || !canScrollAlongDrag) {
                            return consumedByChild
                        }

                        // Cancel the press of any child now that this is a drag.
                        super.onInputEvent(event.copy(action = PointerEvent.Action.Unknown))
                    }

                    handleUserScroll(deltaX, deltaY, velocityTracker.velocityX, velocityTracker.velocityY)
                    isUserScrolling = true
                }

                PointerEvent.Action.Up -> {
                    // Fling with the velocity the finger had when lifted.
                    if (isUserScrolling) {
                        velocityTracker.release(event.timeMillis())
                        if (scrollAxes.isHorizontal) velocityX = velocityTracker.velocityX
                        if (scrollAxes.isVertical) velocityY = velocityTracker.velocityY
                    }
                    isUserScrolling = false
                    dragPointerId = -1
                }

                PointerEvent.Action.Unknown, PointerEvent.Action.Cancel -> {
                    isUserScrolling = false
                    dragPointerId = -1
                }
            }
        } else {
            event as ScrollEvent

            val isScrollingHorizontal = scrollAxes.isHorizontal && abs(event.deltaX) > minimumTravel.x
            val isScrollingVertical = scrollAxes.isVertical && abs(event.deltaY) > minimumTravel.y

            if (isScrolling || isScrollingHorizontal || isScrollingVertical) {
                // A wheel has no samples to track, its velocity is the one of the last step.
                val stepTime = Verktex.clock.time - lastWheelTimeSec
                val velocityX = if (stepTime > 0.3f || stepTime <= 0f) 0f else event.deltaX / stepTime
                val velocityY = if (stepTime > 0.3f || stepTime <= 0f) 0f else event.deltaY / stepTime
                handleUserScroll(event.deltaX, event.deltaY, velocityX, velocityY)
                isUserScrolling = true
                lastWheelTimeSec = Verktex.clock.time
            } else {
                isUserScrolling = false
            }
        }

        if (!wasUserScrolling && !isUserScrolling) {
            return super.onInputEvent(event)
        }
        return true
    }


    // The time of the event, or the one of the frame when the platform didn't give one.
    private fun PointerEvent.timeMillis() = if (time != 0L) time else (Verktex.clock.time * 1000f).toLong()

    private fun hasScrollingDescendant() = propagateConsumable {
        it !== this && it is UIScrollableContainer && it.isUserScrolling
    }

    //endregion


    companion object {

        const val DEFAULT_DECELERATION = 0.15f
        const val DEFAULT_MINIMUM_TRAVEL = 20f
        const val DEFAULT_MAX_VELOCITY = 8000f

        private val INDICATOR_THICKNESS = 3f.dip
        private val MINIMUM_FLING_VELOCITY = 10f.dip

    }
}

/**
 * Sets the [UIScrollableContainer.preventScrolling] property for all scrollable containers
 * in the hierarchy of this entity.
 */
fun UIComponent.setHierarchyScrollPrevention(value: Boolean) {

    var parent = parent
    while (parent != null) {
        if (parent is UIScrollableContainer) {
            parent.preventScrolling = value
        }
        parent = parent.parent
    }

    if (this is UIContainer) {
        forEach { child ->
            if (child is UIScrollableContainer) {
                child.preventScrolling = value
            }
        }
    }
}