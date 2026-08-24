package com.reco1l.verktex.ui.container

import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.rian.andengine.timing.IFrameBasedClock


inline fun UIContainer.clickableContainer(builder: UIClickableContainer.() -> Unit) = UIClickableContainer().apply(builder).also { +it }

open class UIClickableContainer : UIContainer() {

    /**
     * Whether the button is being pressed or not.
     */
    var isPressed = false
        private set

    //region Events

    /**
     * The action to perform when the button is pressed.
     */
    var onActionDown: (() -> Unit)? = null

    /**
     * The action to perform when the button is released.
     */
    var onActionUp: (() -> Unit)? = null

    /**
     * The action to perform when the button is cancelled.
     */
    var onActionCancel: (() -> Unit)? = null

    /**
     * The action to perform when the button is long pressed.
     */
    var onActionLongPress: (() -> Unit)? = null

    //endregion

    private var pressStartTime = 0L


    override fun onInputEvent(event: InputEvent): Boolean {

        if (super.onInputEvent(event)) {
            return true
        }

        if (event is PointerEvent) {
            when(event.action) {
                PointerEvent.Action.Down -> {
                    isPressed = true
                    onActionDown?.invoke()
                    pressStartTime = System.currentTimeMillis()
                }

                PointerEvent.Action.Up -> {
                    if (bounds.contains(event.x, event.y) && isPressed) {
                        onActionUp?.invoke()
                    } else {
                        onActionCancel?.invoke()
                    }
                    isPressed = false
                }

                PointerEvent.Action.Move -> {}

                PointerEvent.Action.Unknown -> {
                    onActionCancel?.invoke()
                    isPressed = false
                }
            }
        }
        return true
    }

    override fun onUpdate(clock: IFrameBasedClock) {
        if (onActionLongPress != null) {
            if (isPressed && clock.currentTime - pressStartTime >= 500L) {
                isPressed = false
                onActionLongPress?.invoke()
            }
        }
        super.onUpdate(clock)
    }

}