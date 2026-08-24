package com.reco1l.verktex.ui.container

import androidx.annotation.*
import com.reco1l.verktex.*
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.input.ScrollEvent
import com.reco1l.verktex.math.almostEquals
import com.reco1l.verktex.math.almostZero
import com.reco1l.verktex.time.TickingClock
import kotlin.math.*


inline fun UIContainer.scrollableContainer(builder: UIScrollableContainer.() -> Unit): UIScrollableContainer {
    return UIScrollableContainer().apply(builder).also(::plusAssign)
}

open class UIScrollableContainer : UIContainer() {

    /**
     * Determines which axes can be scrolled by the user.
     */
    var scrollAxes: Axis = Axis.Both

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
            val velocity = if (value.almostZero()) 0f else value.coerceIn(-maxVelocity.x, maxVelocity.x)

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
            val velocity = if (value.almostZero()) 0f else value.coerceIn(-maxVelocity.y, maxVelocity.y)

            if (field != velocity) {
                field = velocity
                setHierarchyScrollPrevention(velocity != 0f)
            }
        }

    /**
     * The deceleration factor for the scrollable container.
     */
    var deceleration = Vec2(DEFAULT_DECELERATION)

    /**
     * The maximum velocity in px/s for both axes.
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
        get() = max(0f, measuredSize.x) + scrollPadding.x

    /**
     * The height of the content that can be scrolled.
     */
    val scrollableContentHeight
        get() = max(0f, measuredSize.y) + scrollPadding.y

    //endregion

    private var indicatorYAlpha = 0f
    private var indicatorXAlpha = 0f

    private var dragStartTimeSec = 0f


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

        velocityX *= deceleration.x
        velocityY *= deceleration.y
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

    override fun onUpdate(clock: TickingClock) {
        super.onUpdate(clock)

        if (!isUserScrolling) {
            if (isScrolling) {
                decelerateProgressively(clock.elapsedTickTime)
            }
            handleOverflow()
        }

        updateIndicators(clock.elapsedTickTime)
    }

    override fun onDrawChild(renderer: GraphicsRenderer, child: UIComponent) {
        child.x = scrollX.px
        child.y = scrollY.px
        super.onDrawChild(renderer, child)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        super.onDraw(renderer)

        if (showVerticalIndicator && (scrollAxes == Axis.Both || scrollAxes == Axis.Y)) {
            val indicatorHeight = bounds.height * (bounds.height / scrollableContentHeight).coerceAtMost(1f)

            renderer.pushColor(Color4.White.copy(alpha = indicatorYAlpha))
            renderer.drawRect(
                x = bounds.width - 4f,
                y = scrollY * (bounds.height / scrollableContentHeight),
                width = 4f,
                height = indicatorHeight,
                color = Color4.White.copy(alpha = indicatorYAlpha)
            )
            renderer.popColor()
        }

        if (showHorizontalIndicator && (scrollAxes == Axis.Both || scrollAxes == Axis.X)) {
            val indicatorWidth = bounds.width * (bounds.width / scrollableContentWidth).coerceAtMost(1f)

            renderer.pushColor(Color4.White.copy(alpha = indicatorXAlpha))
            renderer.drawRect(
                x = scrollX * (bounds.width / scrollableContentWidth),
                y = bounds.height - 4f,
                width = indicatorWidth,
                height = 4f
            )
            renderer.popColor()
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
            if (alpha > 0f && velocityY == 0f) {
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

    private fun handleUserScroll(deltaX: Float, deltaY: Float) {

        val dragTimeSeconds = (Verktex.clock.time - dragStartTimeSec)
        val length = hypot(deltaX, deltaY)

        fun decreaseInBoundary(current: Float, delta: Float, max: Float): Float {
            if (current - delta > 0f && current - delta < max) {
                return delta
            }
            return delta * if (length > 0) length.pow(0.7f) / length else 0f
        }

        if (scrollAxes.isHorizontal && !deltaX.almostZero()) {
            velocityX = if (dragTimeSeconds > 0.3f) 0f else deltaX / dragTimeSeconds
            scrollX -= decreaseInBoundary(scrollX, deltaX, maxScrollX)

            if (!overflowAxes.isHorizontal) {
                scrollX = scrollX.coerceIn(0f, maxScrollX)
            }
        }

        if (scrollAxes.isVertical && !deltaY.almostZero()) {
            velocityY = if (dragTimeSeconds > 0.3f) 0f else deltaY / dragTimeSeconds
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
            when {
                event.action == PointerEvent.Action.Down -> dragStartTimeSec = Verktex.clock.time
                event.action == PointerEvent.Action.Up -> isUserScrolling = false
            }
        } else {
            event as ScrollEvent

            val isScrollingHorizontal = scrollAxes.isHorizontal && abs(event.deltaX) > minimumTravel.x
            val isScrollingVertical = scrollAxes.isVertical && abs(event.deltaY) > minimumTravel.y

            if (isScrolling || isScrollingHorizontal || isScrollingVertical) {
                handleUserScroll(event.deltaX, event.deltaY)
                isUserScrolling = true
                dragStartTimeSec = Verktex.clock.time
            } else {
                isUserScrolling = false
            }
        }

        if (!wasUserScrolling && !isUserScrolling) {
            return super.onInputEvent(event)
        }
        return true
    }


    //endregion


    companion object {

        const val DEFAULT_DECELERATION = 0.98f
        const val DEFAULT_MINIMUM_TRAVEL = 20f
        const val DEFAULT_MAX_VELOCITY = 3000f

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