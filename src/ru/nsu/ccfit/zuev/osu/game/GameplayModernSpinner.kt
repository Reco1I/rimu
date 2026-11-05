package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.game.ScoreNumber
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Created by dgsrz on 15/10/19.
 */
class GameplayModernSpinner : GameplaySpinner() {

    private val middle: UISprite
    private val middle2: UISprite
    private val bottom: UISprite
    private val top: UISprite
    private val glow: UISprite
    private val bonusScore: ScoreNumber

    init {
        position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f)
        Utils.trackToRealCoords(position)

        middle = UISprite()
        middle.origin = Anchor.Center
        middle.setPosition(position.x, position.y)
        middle.textureRegion = SkinTextures.get("spinner-middle")

        middle2 = UISprite()
        middle2.origin = Anchor.Center
        middle2.setPosition(position.x, position.y)
        middle2.textureRegion = SkinTextures.get("spinner-middle2")

        bottom = UISprite()
        bottom.origin = Anchor.Center
        bottom.setPosition(position.x, position.y)
        bottom.textureRegion = SkinTextures.get("spinner-bottom")

        top = UISprite()
        top.origin = Anchor.Center
        top.setPosition(position.x, position.y)
        top.textureRegion = SkinTextures.get("spinner-top")

        glow = UISprite()
        glow.origin = Anchor.Center
        glow.setPosition(position.x, position.y)
        glow.textureRegion = SkinTextures.get("spinner-glow")

        bonusScore = ScoreNumber(position.x, position.y + 100, "", 1.1f, true)

        // Spinners always end combo.
        endsCombo = true
    }

    override fun init(listener: GameObjectListener, scene: UIContainer,
                      beatmapSpinner: Spinner, rps: Float, stat: StatisticV2) {
        this.scene = scene
        this.beatmapSpinner = beatmapSpinner
        this.listener = listener
        this.stat = stat
        hitTime = beatmapSpinner.startTime.toFloat() / 1000
        duration = max(beatmapSpinner.duration.toFloat() / 1000f, 0f)
        needRotations = rps * duration

        if (duration < 0.05f) {
            needRotations = 0.1f
        }

        clear = duration <= 0f
        fullRotations = 0
        rotations = 0f
        bonusScoreCounter = 1
        startHit = false

        reloadHitSounds()

        glow.alpha = 0f
        glow.scale = 0.9f
        glow.setColor(0f, 0.8f, 1f)

        val isBackgroundVisible = (!GameHelper.isTraceable() ||
            (Config.isShowFirstApproachCircle() && GameHelper.getTraceable()!!.firstObject === beatmapSpinner))

        if (isBackgroundVisible) {
            middle.alpha = 0f
            middle.scale = 0.9f
            middle2.alpha = 0f
            middle2.scale = 0.9f
        }

        bottom.alpha = 0f
        bottom.scale = 0.9f

        top.alpha = 0f
        top.scale = 0.9f

        scene += glow
        scene += bottom
        scene += top

        if (isBackgroundVisible) {
            scene += middle
            scene += middle2
        }

        val timePreempt = beatmapSpinner.timePreempt.toFloat() / 1000f
        passedTime = -timePreempt

        fadeIn(top)
        fadeIn(bottom)

        if (isBackgroundVisible) {
            fadeIn(middle)
            fadeIn(middle2)
        }

        lifetimeEnd = Float.MAX_VALUE
    }

    private fun fadeIn(sprite: UISprite) {
        val timePreempt = beatmapSpinner!!.timePreempt.toFloat() / 1000

        sprite.choreographAt(hitTime - timePreempt) { sequence -> sequence.fadeIn(timePreempt) }
    }

    override fun update(dt: Float) {
        val listener = listener!!
        val stat = stat!!

        passedTime = listener.elapsedTime - hitTime

        // Allow the spinner to fully fade in first before receiving spins.
        if (passedTime < 0) {
            return
        }

        if (!startHit) {
            listener.onSpinnerStart(id)
            startHit = true

            // Fast-forward rotation state when spawned mid-spinner after a seek in Autoplay.
            if (autoPlay && passedTime > 0) {
                applySeekRotations()

                // Unlike GameplaySpinner, GameplayModernSpinner has no clearText sprite; clear state is applied
                // visually on next frame.
                if (bonusScoreCounter > 1) {
                    bonusScore.setText(((bonusScoreCounter - 1) * 1000).toString())
                    scene!! += bonusScore
                }
            }
        }

        updateSamples(dt)
        var mouse: Vector2? = null

        var i = 0
        val count = listener.getCursorsCount()
        while (i < count) {
            val cursor = listener.getCursor(i)
            val latestEvent = cursor.latestEvent

            if (mouse == null) {
                if (autoPlay) {
                    mouse = position
                } else {
                    if (latestEvent != null && !latestEvent.isActionUp) {
                        mouse = latestEvent.position
                    } else {
                        ++i
                        continue
                    }
                }
                currMouse.set(mouse.x - position.x, mouse.y - position.y)
            }

            if (oldMouse == null || (!autoPlay && latestEvent != null && latestEvent.isActionDown)) {
                if (oldMouse == null) {
                    oldMouse = Vector2(0f)
                }
                oldMouse!!.set(currMouse)
                return
            }
            ++i
        }

        if (mouse != null) {
            val oldMouse = oldMouse!!

            var degree = toDegrees(Utils.direction(currMouse))
            top.rotationZ = degree
            bottom.rotationZ = degree / 2

            val len1 = Utils.length(currMouse)
            val len2 = Utils.length(oldMouse)
            var dFill = (currMouse.x / len1) * (oldMouse.y / len2) - (currMouse.y / len1) * (oldMouse.x / len2)

            if (abs(len1) < 0.0001f || abs(len2) < 0.0001f)
                dFill = 0f

            if (autoPlay) {
                dFill = 5 * 4 * dt
                degree = (rotations + dFill / 4f) * 360
                top.rotationZ = degree
                //auto时，FL光圈绕中心旋转
                if (GameHelper.isAutopilot() || GameHelper.isAutoplay()) {
                    val pX = position.x + 50 * sin(degree.toDouble()).toFloat()
                    val pY = position.y + 50 * cos(degree.toDouble()).toFloat()
                    listener.updateAutoBasedPos(pX, pY)
                }
                // bottom.setRotation(-degree);
            }

            rotations += dFill / 4f
            val percentFilled = (abs(rotations) + fullRotations) / needRotations
            val percent = min(percentFilled, 1f)

            if (dFill != 0f) {
                updateSpinSampleFrequency(percentFilled)
                spinnerSpinSample.play()
            } else {
                spinnerSpinSample.stopAll()
            }

            middle.setColor(1f, 1 - percent, 1 - percent)
            top.scale = 0.9f + percent * 0.1f
            bottom.scale = 0.9f + percent * 0.1f

            if (middle.isVisible) {
                middle.scale = 0.9f + percent * 0.1f
            }

            if (middle2.isVisible) {
                middle2.scale = 0.9f + percent * 0.1f
            }

            glow.alpha = percent * 0.8f
            glow.scale = 0.9f + percent * 0.1f

            if (percentFilled > 1 || clear) {
                if (!clear) {
                    // Clear Sprite
                    clear = true

                    // rotations is intentionally NOT reset here. Resetting would shift the first bonus threshold
                    // from ceil(needRotations) to needRotations + 1, matching osu!lazer but reducing Autoplay
                    // scores and altering overall game balance.
                }

                if (abs(rotations) > 1) {
                    rotations -= 1 * sign(rotations)
                    bonusScore.setText((bonusScoreCounter * 1000).toString())
                    listener.onSpinnerHit(id, 1000, false, 0)
                    bonusScoreCounter++
                    if (bonusScore.parent == null) {
                        scene!! += bonusScore
                    }
                    spinnerBonusSample.play()

                    glow.choreographNow { sequence -> sequence
                        .colorTo(0f, 1f, 0.8f)
                        .colorTo(1f, 1f, 1f, 0.1f)
                        .then()
                        .colorTo(1f, 0f, 1f)
                        .colorTo(0.8f, 1f, 1f, 0.1f) }

                    var rate = 0.375f
                    if (GameHelper.getHealthDrain() > 0) {
                        rate = 1 + (GameHelper.getHealthDrain() / 4f)
                    }
                    stat.changeHp(rate * 0.01f * duration / needRotations)
                }
            } else if (abs(rotations) > 1) {
                rotations -= 1 * sign(rotations)
                val replayObjectData = replayObjectData
                if (replayObjectData == null || replayObjectData.accuracy / 4 > fullRotations) {
                    fullRotations++
                    stat.registerSpinnerHit()
                    var rate = 0.375f
                    if (GameHelper.getHealthDrain() > 0) {
                        rate = 1 + (GameHelper.getHealthDrain() / 2f)
                    }
                    stat.changeHp(rate * 0.01f * duration / needRotations)
                }
            }

            oldMouse.set(currMouse)
        }

        if (passedTime >= duration) {
            removeFromScene()
        }
    }

    override fun detachFromScene() {
        if (scene == null) {
            return
        }

        lifetimeEnd = beatmapSpinner!!.endTime.toFloat() / 1000

        middle.clearAnimations()
        middle.removeSelf()

        middle2.clearAnimations()
        middle2.removeSelf()

        bottom.clearAnimations()
        bottom.removeSelf()

        top.clearAnimations()
        top.removeSelf()

        glow.clearAnimations()
        glow.removeSelf()

        bonusScore.removeSelf()
        scene = null
    }

    override fun removeFromScene() {
        if (scene == null) {
            return
        }

        detachFromScene()

        val listener = listener!!
        val replayObjectData = replayObjectData

        var score = 0
        if (replayObjectData != null) {
            while (fullRotations + bonusScoreCounter < replayObjectData.accuracy / 4 + 1) {
                fullRotations++
                listener.onSpinnerHit(id, 1000, false, 0)
            }
            clear = fullRotations >= needRotations
        }
        val percentFilled = (abs(rotations) + fullRotations) / needRotations
        if (percentFilled > 0.9f) {
            score = 50
        }
        if (percentFilled > 0.95f) {
            score = 100
        }
        if (clear) {
            score = 300
        }
        if (replayObjectData != null) {
            score = when (replayObjectData.accuracy % 4) {
                0 -> 0
                1 -> 50
                2 -> 100
                3 -> 300
                else -> score
            }
        }
        stopLoopingSamples()
        listener.onSpinnerHit(id, score, endsCombo, this.bonusScoreCounter + fullRotations - 1)
        playAndFreeHitSamples(score)
    }
}
