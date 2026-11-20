package com.osudroid.ui.v2.game

import com.osudroid.OsuGame
import com.osudroid.game.session.GameplaySession
import com.reco1l.verktex.data.ScaleType
import com.osudroid.game.PlayfieldMapping
import com.osudroid.resources.Res
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.ScalingContainer
import com.reco1l.verktex.ui.fadeIn
import kotlin.math.hypot
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * The menu shown when pausing or failing, on top of the frozen gameplay.
 */
class PauseMenu(private val game: GameplaySession, private val isFail: Boolean) : Container() {

    private var isReplaySaved = false


    init {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        SkinTextures[if (isFail) "fail-background" else "pause-overlay"]?.let {
            +Sprite(it).apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = ScaleType.Crop
            }
        }

        // Laid out for a screen 1280 wide, as gameplay is.
        +ScalingContainer().apply {
            referenceSize = Vec2(PlayfieldMapping.REFERENCE_WIDTH, 720f)

            +LinearContainer().apply {
                orientation = LinearContainer.Orientation.Vertical
                anchor = Anchor.Center
                origin = Anchor.Center

                if (isFail && !game.replaying) {
                    +MenuItem("pause-save-replay", ::saveReplay)
                }

                if (!isFail) {
                    +MenuItem("pause-continue") {
                        SkinSounds["menuback"].play()
                        game.resume()
                    }
                }

                +MenuItem("pause-retry") {
                    SkinSounds["failsound"].stop()
                    SkinSounds["menuhit"].play()
                    game.gameplay.retry()
                }

                +MenuItem("pause-back") {
                    game.gameplay.scoring.replayID = -1
                    SkinSounds["menuback"].play()
                    game.quit()
                }
            }
        }
    }


    fun show() {
        alpha = 0f
        Verktex.addOverlay(this)
        fadeIn(0.4f)
    }

    fun hide() {
        Verktex.removeOverlay(this)
    }

    private fun saveReplay() {
        if (isFail && !isReplaySaved && !game.replaying && game.saveFailedReplay()) {
            Toasts.showLong(Res.string.message_save_replay_successful)
            isReplaySaved = true
        }
    }

    override fun onUpdate(clock: com.reco1l.verktex.time.FrameClock) {
        if (Verktex.scene != game.scene) {
            hide()
            return
        }

        super.onUpdate(clock)
    }

    override fun onInputEvent(event: InputEvent): Boolean {
        super.onInputEvent(event)

        // The gameplay below must not receive anything while paused.
        return event is PointerEvent
    }


    private class MenuItem(texture: String, private val onClick: () -> Unit) : Sprite(SkinTextures[texture]) {

        private var isPressed = false
        private var pressX = 0f
        private var pressY = 0f


        init {
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter
        }


        override fun onInputEvent(event: InputEvent): Boolean {
            if (event !is PointerEvent) {
                return false
            }

            when (event.action) {
                PointerEvent.Action.Down -> {
                    isPressed = containsScenePoint(event.x, event.y)
                    pressX = event.x
                    pressY = event.y
                }

                PointerEvent.Action.Move -> {
                    if (isPressed && hypot(event.x - pressX, event.y - pressY) > 50f) {
                        isPressed = false
                    }
                }

                PointerEvent.Action.Up -> {
                    if (isPressed && containsScenePoint(event.x, event.y)) {
                        isPressed = false
                        onClick()
                    }

                    isPressed = false
                }

                else -> isPressed = false
            }

            return isPressed
        }
    }
}
