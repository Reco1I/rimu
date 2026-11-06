package com.osudroid.ui.v2.hud.elements

import com.osudroid.OsuGame
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock

import com.reco1l.verktex.ui.shape.UICircle
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.math.FloatInterpolation
import com.osudroid.ui.v2.hud.HUDElement
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.data.px

class HUDBackButton : HUDElement() {

    override val shouldBeShown = true

    private val requiredPressTimeMs = Settings[OsuSettings.BackButtonPressTime].toFloat()

    private val arrow = UISprite().apply {
        textureRegion = SkinTextures["back-arrow"]
        anchor = Anchor.Center
        origin = Anchor.Center
        width = 0.6f.pct
        height = 0.6f.pct
    }

    private val backCircle = UICircle().apply {
        setPortion(0f)
        anchor = Anchor.Center
        origin = Anchor.Center
        color = Color4.White
        depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)

        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
    }

    private val frontCircle = UICircle().apply {
        anchor = Anchor.Center
        origin = Anchor.Center
        color = Color4(0xFF002626)
        clearDepthBeforeDraw = true
        depthState = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)

        width = 0.95f.pct
        height = 0.95f.pct
    }


    private var holdDurationMs = 0f
        set(value) {
            field = value.coerceIn(0f, requiredPressTimeMs)

            // The progress is intentionally kept at 0 for 0 activation time to keep the button in its initial state.
            val progress = if (requiredPressTimeMs > 0) field / requiredPressTimeMs else 0f
            val scale = 1f + progress / 2f

            alpha = FloatInterpolation.floatAt(holdDurationMs, 0.25f, 0.5f, 0f, requiredPressTimeMs, Easing.OutCubic)
            backCircle.setPortion(progress)
            backCircle.scale = scale
            frontCircle.scale = scale
            arrow.scale = scale
        }


    private var isPressed = false


    init {
        width = SIZE.px
        height = SIZE.px
        alpha = 0.25f

        +frontCircle
        +backCircle
        +arrow
    }


    override fun onUpdate(clock: FrameClock) {
        if (!isInEditMode) {
            val realMsElapsed = clock.elapsedFrameTime * 1000

            if (isPressed) {
                holdDurationMs += realMsElapsed

                if (holdDurationMs >= requiredPressTimeMs) {
                    isPressed = false
                    OsuGame.gameScene.pause()
                }
            } else {
                holdDurationMs -= realMsElapsed * 2
            }
        }

        super.onUpdate(clock)
    }

    override fun onInputEvent(event: InputEvent): Boolean {

        if (isInEditMode || event !is PointerEvent) {
            return super.onInputEvent(event)
        }

        when (event.action) {
            PointerEvent.Action.Down -> {
                if (containsScenePoint(event.x, event.y)) {
                    isPressed = true
                    return true
                }
            }

            PointerEvent.Action.Move -> {
                if (isPressed) {
                    isPressed = containsScenePoint(event.x, event.y)
                    return true
                }
            }

            PointerEvent.Action.Up -> {
                if (isPressed) {
                    isPressed = false
                    return true
                }
            }

            else -> isPressed = false
        }

        return false
    }


    companion object {
        private const val SIZE = 72f
    }
}