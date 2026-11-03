package com.reco1l.verktex.ui.control

import com.reco1l.verktex.*
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatAnimation
import com.reco1l.verktex.math.interpolateColor
import com.reco1l.verktex.math.interpolateFloat
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.time.FrameClock

class UICheckbox(initialValue: Boolean = false) : UIControl<Boolean>(initialValue) {

    private val checkIcon = UIIcon(FAIcon.Check).apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        style = Style {
            color = it.palette.accent
        }

        if (!initialValue) {
            scale = 0f
            alpha = 0f
        }
    }

    private val checkScale = FloatAnimation(if (initialValue) 1f else 0f, 0.4f, Easing.OutBounce)


    private var isPressed = false
    private var pressPointerId = -1


    init {
        style = Style { theme ->
            width = theme.controlStyle.minimumHeight
            height = theme.controlStyle.minimumHeight
            backgroundColor = if (value) theme.palette.controlActiveBackground else theme.palette.controlInactiveBackground
            radius = theme.radius.lg
        }
        +checkIcon
    }


    override fun onUpdate(clock: FrameClock) {

        val targetScaleAndAlpha = if (value) 1f else 0f
        val targetBackgroundCOlor = if (value) resolvedTheme.palette.controlActiveBackground else resolvedTheme.palette.controlInactiveBackground

        checkScale.target = targetScaleAndAlpha
        checkIcon.scale = checkScale.update(clock)
        checkIcon.alpha = clock.interpolateFloat(0.2f, checkIcon.alpha, targetScaleAndAlpha)
        backgroundColor = clock.interpolateColor(0.2f, backgroundColor, targetBackgroundCOlor)

        super.onUpdate(clock)
    }


    override fun onInputEvent(event: InputEvent): Boolean {
        if (event !is PointerEvent) {
            return super.onInputEvent(event)
        }

        // Up and cancel events are dispatched to every component, only the finger that pressed
        // this checkbox can release it.
        if (event.action != PointerEvent.Action.Down && (!isPressed || event.pointerId != pressPointerId)) {
            return false
        }

        return when (event.action) {
            PointerEvent.Action.Down -> {
                if (!isPressed) {
                    isPressed = true
                    pressPointerId = event.pointerId
                }
                true
            }
            PointerEvent.Action.Up -> {
                isPressed = false
                if (containsScenePoint(event.x, event.y)) {
                    value = !value
                }
                true
            }
            PointerEvent.Action.Unknown, PointerEvent.Action.Cancel -> {
                isPressed = false
                false
            }
            PointerEvent.Action.Move -> true
        }
    }
}