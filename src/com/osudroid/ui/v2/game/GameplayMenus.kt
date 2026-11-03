package com.osudroid.ui.v2.game

import com.osudroid.resources.R
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreograph
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.fadeIn
import kotlin.math.hypot
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * A number drawn with the score font of the skin.
 */
class ScoreNumber @JvmOverloads constructor(
    x: Float,
    y: Float,
    text: String,
    private val letterScale: Float,
    private val isCentered: Boolean = false
) : UIContainer() {

    /**
     * The height of the letters.
     */
    var letterHeight = 0f
        private set


    init {
        setPosition(x, y)
        setText(text)
    }


    fun setText(text: String) {
        Verktex.withTreeLock {
            clearChildren()

            var totalWidth = 0f

            for (ch in text) {
                val name = when (ch) {
                    in '0'..'9' -> ch.toString()
                    '.', ',' -> "comma"
                    '%' -> "percent"
                    else -> "x"
                }

                val texture = SkinTextures[OsuSkin.get().scorePrefix, name] ?: continue
                val width = texture.width * letterScale

                letterHeight = texture.height * letterScale

                +UISprite(texture).apply {
                    setPosition(totalWidth, 0f)
                    setSize(width, letterHeight)
                }

                totalWidth += width
            }

            if (isCentered) {
                for (letter in this) {
                    letter.x = (letter.x.resolve(0f) - totalWidth / 2f).px
                    letter.choreograph {
                        at(letter.currentClockTime)
                        scaleTo(1.5f, 0.2f)
                        then()
                        scaleTo(1f, 0.4f)
                    }
                }
            }
        }
    }
}

/**
 * The menu shown when pausing or failing, on top of the frozen gameplay.
 */
class PauseMenu(private val game: GameScene, private val isFail: Boolean) : UIContainer() {

    private var isReplaySaved = false


    init {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        SkinTextures[if (isFail) "fail-background" else "pause-overlay"]?.let {
            +UISprite(it).apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = UISprite.ScaleType.Crop
            }
        }

        +UILinearContainer().apply {
            orientation = UILinearContainer.Orientation.Vertical
            anchor = Anchor.Center
            origin = Anchor.Center
            scale = Verktex.graphics.window.windowWidth.toFloat() / Config.getRES_WIDTH()

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
                game.restartGame()
            }

            +MenuItem("pause-back") {
                GlobalManager.getInstance().scoring.replayID = -1
                SkinSounds["menuback"].play()
                game.quit()
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
            ToastLogger.showText(R.string.message_save_replay_successful, true)
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


    private class MenuItem(texture: String, private val onClick: () -> Unit) : UISprite(SkinTextures[texture]) {

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
