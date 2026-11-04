package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.ui.container.UIRecyclerContainer
import kotlin.math.*

class BeatmapSetModel(
    val beatmapSetInfo: BeatmapSetInfo,

    /**
     * Whether the panel stands for a single difficulty, so it shows its name as well.
     */
    val isDifficulty: Boolean = false,
    var coverTexture: GraphicsTexture.Region? = null,
    var isLoadingCover: Boolean = false,
) {
    override fun toString(): String {
        return beatmapSetInfo.path
    }
}

class BeatmapCarrousel : UIRecyclerContainer<BeatmapSetModel, BeatmapSetPanel>(), IPanelContainer<BeatmapSetPanel> {

    /**
     * The currently selected beatmap set panel.
     */
    override var selectedPanel: BeatmapSetPanel? = null
        set(value) {
            if (field != value) {
                field = value

                boundComponents.values.forEach { panel ->
                    if (panel != value) {
                        panel.collapse()
                    } else {
                        panel.expand()
                    }
                }
            }
        }

    /**
     * Called when a beatmap is selected, or with null when the selection is cleared.
     */
    var onBeatmapSelected: ((BeatmapInfo?) -> Unit)? = null

    /**
     * Called when the selected beatmap is pressed again to play it.
     */
    var onBeatmapConfirmed: ((BeatmapInfo) -> Unit)? = null

    /**
     * Called when a beatmap set or one of its difficulties is long pressed.
     */
    var onBeatmapSetLongPress: ((BeatmapSetPanel) -> Unit)? = null

    /**
     * Whether to automatically scroll to the selected panel.
     */
    var autoScrollToSelectedPanel = false
        set(value) {
            if (value && !field) {
                autoScrollVelocity = 0f
                autoScrollPosition = scrollY
            }
            field = value
        }

    // The auto scroll is a critically damped spring, it speeds up and slows down smoothly instead
    // of jumping at the start.
    private var autoScrollVelocity = 0f

    // The scroll position is whole pixels, the spring keeps the fractions or the rounding would make
    // it oscillate around the target.
    private var autoScrollPosition = 0f

    /**
     * How far the panels are moved past the right edge of the carrousel, covering the safe area on
     * the right. They are wider than the carrousel as well, see [BeatmapSetPanel.OVERFLOW], so
     * their end is never seen and they seem to continue past the screen.
     */
    var panelOverhang = 0f
        private set


    // The selected item and its position in the content, panels above it change their size while
    // collapsing and the scroll follows them so the selected one doesn't move.
    private var anchorItem: BeatmapSetModel? = null
    private var anchorPosition = 0f

    // The scroll position is whole pixels, the rest is applied in the next frames.
    private var anchorRemainder = 0f


    init {
        scrollAxes = Axis.Y
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        spacing = (-2f).px

        componentWrapper.padding = Padding(0f.px, 200f.px)

        onCreateComponent = { BeatmapSetPanel(this) }
    }


    /**
     * The vertical position of the middle of a component in the carrousel, from its layout. The
     * transformations aren't used, they include the offset of the curve and are a frame behind.
     */
    private fun centerYOf(component: UIComponent): Float {
        var y = component.bounds.y + component.bounds.height / 2f
        var parent = component.parent

        while (parent != null && parent !== this) {
            y += parent.bounds.y
            parent = parent.parent
        }
        return y
    }

    /**
     * The offset of a component following the tilt: the panels lean to the right towards the top
     * of the carrousel.
     */
    private fun curveOffsetOf(component: UIComponent): Float {
        if (bounds.height <= 0f) {
            return 0f
        }

        val offset = TILT * (1f - centerYOf(component) / bounds.height)

        return if (offset.isFinite()) offset else 0f
    }


    override fun onLayout(bounds: Vec4) {
        keepSelectedPanelInPlace()

        super.onLayout(bounds)

        // Applied after laying out, so the tilt follows the panels in the same frame while scrolling
        // instead of lagging a frame behind and shaking sideways.
        boundComponents.values.forEach { component ->
            component.curveOffset = curveOffsetOf(component.button)

            component.panelsContainer.forEach { panel ->
                (panel as BeatmapPanel).curveOffset = curveOffsetOf(panel)
            }
        }
    }

    private fun keepSelectedPanelInPlace() {
        val item = selectedPanel?.boundData
        val position = item?.let(::positionOf)

        if (item == null || position == null) {
            anchorItem = null
            return
        }

        if (item === anchorItem) {
            autoScrollPosition += position - anchorPosition

            val target = scrollY + position - anchorPosition + anchorRemainder
            scrollY = target
            anchorRemainder = target - scrollY
        } else {
            anchorRemainder = 0f
        }

        anchorItem = item
        anchorPosition = position
    }

    override fun onUpdate(clock: FrameClock) {
        panelOverhang = Verktex.graphics.window.insets.safeArea.z

        val panel = selectedPanel
        if (panel != null && autoScrollToSelectedPanel) {

            val targetY = (scrollY + centerYOf(panel.scrollTarget) - bounds.height / 2f).coerceIn(0f, maxScrollY)
            val difference = targetY - autoScrollPosition

            // Long frames would make the spring overshoot.
            val deltaTime = clock.elapsedFrameTime.coerceAtMost(1f / 30f)

            if (abs(difference) > 0.5f || abs(autoScrollVelocity) > 5f) {
                val acceleration = AUTO_SCROLL_STIFFNESS * AUTO_SCROLL_STIFFNESS * difference - 2f * AUTO_SCROLL_STIFFNESS * autoScrollVelocity
                autoScrollVelocity += acceleration * deltaTime
                autoScrollPosition += autoScrollVelocity * deltaTime

                scrollY = autoScrollPosition
            } else {
                scrollY = targetY
                autoScrollToSelectedPanel = false
            }
        }

        super.onUpdate(clock)
    }

    override fun onInputEvent(event: InputEvent): Boolean {
        if (event is PointerEvent && event.action == PointerEvent.Action.Down && containsScenePoint(event.x, event.y)) {
            autoScrollToSelectedPanel = false
        }
        return super.onInputEvent(event)
    }


    companion object {
        private const val TILT = 180f
        private const val AUTO_SCROLL_STIFFNESS = 10f
    }
}
