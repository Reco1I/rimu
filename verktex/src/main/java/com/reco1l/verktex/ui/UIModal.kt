package com.reco1l.verktex.ui

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer

/**
 * A container shown above everything else, with a [card] holding its content on top of a dimmed
 * backdrop. It is attached as an overlay when shown, unless it was added somewhere else before.
 */
@Suppress("LeakingThis")
open class UIModal(

    /**
     * The content of the modal. This is where you should add your UI elements.
     */
    val card: UIContainer = UIContainer().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        clipping = ClipMode.Bounds
        style = Style { theme ->
            backgroundColor = theme.palette.surface
            radius = theme.radius.lg
            applyElevation(theme.cardStyle.elevation)
        }
    }

) : UIContainer() {

    /**
     * Whether the modal stays open when the user presses outside of the [card].
     */
    var staticBackdrop = false

    /**
     * Whether the modal is detached from its parent once hidden.
     */
    var detachOnHide = false

    /**
     * Whether the modal is shown or being shown.
     */
    var isShown = false
        private set


    // The finger that pressed the backdrop, releasing it outside of the card hides the modal.
    private var backdropPointerId = -1

    // The fingers that pressed the modal, only their events are handled.
    private val pressedPointers = mutableSetOf<Int>()

    // Whether onShown was called for the current show.
    private var isFullyShown = false


    init {
        width = fill()
        height = fill()

        backgroundColor = BACKDROP_COLOR
        isVisible = false
        alpha = 0f

        card.scale = HIDDEN_CARD_SCALE
        +card
    }


    //region Visibility

    /**
     * Shows the modal.
     */
    fun show() {
        if (isShown) return

        if (parent == null) {
            Verktex.addOverlay(this)
        }

        isShown = true
        isVisible = true
        isFullyShown = false
        pressedPointers.clear()
        onShow()
    }

    /**
     * Hides the modal.
     */
    fun hide() {
        if (!isShown) return

        isShown = false
        backdropPointerId = -1
        onHide()
    }

    /**
     * Called when [show] is called.
     */
    protected open fun onShow() = Unit

    /**
     * Called once the show animation finished.
     */
    protected open fun onShown() = Unit

    /**
     * Called when [hide] is called.
     */
    protected open fun onHide() = Unit

    /**
     * Called once the modal finished hiding.
     */
    protected open fun onHidden() {
        if (detachOnHide) {
            removeSelf()
        }
    }

    /**
     * Advances the show animation by one frame and returns whether it finished. Override it to
     * change how the modal appears.
     */
    protected open fun onShowAnimation(clock: FrameClock): Boolean {
        alpha = clock.interpolateFloat(0.2f, alpha, 1f)
        card.scale = clock.interpolateFloat(0.2f, card.scale, 1f)
        return alpha == 1f && card.scale == 1f
    }

    /**
     * Advances the hide animation by one frame and returns whether it finished. Override it to
     * change how the modal disappears.
     */
    protected open fun onHideAnimation(clock: FrameClock): Boolean {
        alpha = clock.interpolateFloat(0.2f, alpha, 0f)
        card.scale = clock.interpolateFloat(0.2f, card.scale, HIDDEN_CARD_SCALE)
        return alpha == 0f
    }

    override fun onUpdate(clock: FrameClock) {
        if (isShown) {
            if (!isFullyShown && onShowAnimation(clock)) {
                isFullyShown = true
                onShown()
            }
        } else if (isVisible && onHideAnimation(clock)) {
            isVisible = false
            onHidden()
        }

        super.onUpdate(clock)
    }

    //endregion

    //region Input

    override fun onInputEvent(event: InputEvent): Boolean {
        // While hiding the scene below is usable again.
        if (!isShown) {
            return false
        }

        // Only pointers pressed on the modal are handled, so components outside of it still get
        // the moves and releases of their own presses (the modal doesn't always cover the window).
        if (event is PointerEvent) {
            when (event.action) {
                PointerEvent.Action.Down -> if (containsScenePoint(event.x, event.y)) {
                    pressedPointers += event.pointerId
                } else {
                    return false
                }

                else -> if (event.pointerId !in pressedPointers) {
                    return false
                } else if (event.action != PointerEvent.Action.Move) {
                    pressedPointers -= event.pointerId
                }
            }
        }

        if (super.onInputEvent(event)) {
            return true
        }

        if (event is PointerEvent && !staticBackdrop) {
            when (event.action) {
                PointerEvent.Action.Down -> backdropPointerId = event.pointerId

                PointerEvent.Action.Up -> if (event.pointerId == backdropPointerId) {
                    backdropPointerId = -1
                    if (!card.containsScenePoint(event.x, event.y)) {
                        hide()
                    }
                }

                PointerEvent.Action.Unknown, PointerEvent.Action.Cancel -> backdropPointerId = -1
                PointerEvent.Action.Move -> Unit
            }
        }

        // The modal blocks everything below it.
        return true
    }

    //endregion


    companion object {
        private val BACKDROP_COLOR = Color4.Black.copy(alpha = 0.5f)
        private const val HIDDEN_CARD_SCALE = 0.9f
    }
}
