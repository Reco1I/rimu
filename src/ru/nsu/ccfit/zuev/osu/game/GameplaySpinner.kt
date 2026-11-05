package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.GameplaySequenceHitSampleInfo
import com.osudroid.math.Vector2
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.game.ScoreNumber
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.scaleTo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.skins.OsuSkin

open class GameplaySpinner : GameObject() {
    private val background: UISprite
    private val circle: UISprite
    private val approachCircle: UISprite
    private val metre: UIContainer
    private val fullHeight: Float
    private val spinText: UISprite
    private val clearText: UISprite
    private val bonusScore: ScoreNumber

    @JvmField
    protected var beatmapSpinner: Spinner? = null
    @JvmField
    protected var oldMouse: Vector2? = null
    @JvmField
    protected var listener: GameObjectListener? = null
    @JvmField
    protected var scene: UIContainer? = null
    @JvmField
    protected var fullRotations = 0
    @JvmField
    protected var rotations = 0f
    @JvmField
    protected var needRotations = 0f
    @JvmField
    protected var clear = false
    @JvmField
    protected var bonusScoreCounter = 1
    @JvmField
    protected var stat: StatisticV2? = null
    @JvmField
    protected var passedTime = 0f
    @JvmField
    protected var duration = 0f

    @JvmField
    protected val isSpinnerFrequencyModulate: Boolean
    @JvmField
    protected val hitSamples = ArrayList<GameplayHitSampleInfo>(5)
    @JvmField
    protected val spinnerSpinSample: GameplaySequenceHitSampleInfo
    @JvmField
    protected val spinnerBonusSample: GameplaySequenceHitSampleInfo

    @JvmField
    protected val currMouse = Vector2(0f)

    init {
        position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f)
        Utils.trackToRealCoords(position)

        background = UISprite()
        background.origin = Anchor.Center
        background.setPosition(position.x, position.y)
        background.textureRegion = SkinTextures.get("spinner-background")
        val backgroundTexture = SkinTextures.get("spinner-background")
        background.scale = if (backgroundTexture != null) Config.getRES_WIDTH() / backgroundTexture.width.toFloat() else 1f
        fullHeight = if (backgroundTexture != null) backgroundTexture.height * background.scaleY else 0f

        circle = UISprite()
        circle.origin = Anchor.Center
        circle.setPosition(position.x, position.y)
        circle.textureRegion = SkinTextures.get("spinner-circle")

        // The metre fills from the bottom: the sprite stays in place while the container clipping it grows.
        val metreSprite = UISprite()
        metreSprite.scaleType = UISprite.ScaleType.Stretch
        metreSprite.textureRegion = SkinTextures.get("spinner-metre")
        metreSprite.anchor = Anchor.BottomLeft
        metreSprite.origin = Anchor.BottomLeft
        metreSprite.setSize(Config.getRES_WIDTH().toFloat(), fullHeight)

        metre = UIContainer()
        metre.clipping = ClipMode.Bounds
        metre.origin = Anchor.BottomLeft
        metre.setPosition(position.x - Config.getRES_WIDTH() / 2f, position.y + fullHeight / 2f)
        metre.setSize(Config.getRES_WIDTH().toFloat(), 0f)
        metre += metreSprite

        approachCircle = UISprite()
        approachCircle.origin = Anchor.Center
        approachCircle.setPosition(position.x, position.y)
        approachCircle.textureRegion = SkinTextures.get("spinner-approachcircle")

        spinText = UISprite()
        spinText.origin = Anchor.Center
        spinText.setPosition(position.x, position.y * 1.5f)
        spinText.textureRegion = SkinTextures.get("spinner-spin")

        clearText = UISprite()
        clearText.origin = Anchor.Center
        clearText.setPosition(position.x, position.y * 0.5f)
        clearText.textureRegion = SkinTextures.get("spinner-clear")

        bonusScore = ScoreNumber(position.x, position.y + 100, "", 1.1f, true)

        isSpinnerFrequencyModulate = OsuSkin.get().isSpinnerFrequencyModulate
        spinnerSpinSample = GameplaySequenceHitSampleInfo()
        spinnerBonusSample = GameplaySequenceHitSampleInfo()

        // Spinners always end combo.
        endsCombo = true
    }

    open fun init(listener: GameObjectListener, scene: UIContainer,
                  beatmapSpinner: Spinner, rps: Float, stat: StatisticV2) {
        fullRotations = 0
        rotations = 0f
        this.scene = scene
        this.duration = max(beatmapSpinner.duration.toFloat() / 1000f, 0f)
        this.beatmapSpinner = beatmapSpinner

        needRotations = rps * duration
        if (duration < 0.05f) {
            needRotations = 0.1f
        }

        this.listener = listener
        this.stat = stat
        clear = duration <= 0f
        bonusScoreCounter = 1
        startHit = false

        reloadHitSounds()

        hitTime = beatmapSpinner.startTime.toFloat() / 1000f
        val timePreempt = beatmapSpinner.timePreempt.toFloat() / 1000f
        passedTime = -timePreempt

        background.isVisible = !GameHelper.isTraceable() ||
            (Config.isShowFirstApproachCircle() && GameHelper.getTraceable()!!.firstObject === beatmapSpinner)

        metre.setSize(Config.getRES_WIDTH().toFloat(), 0f)

        scene.add(0, spinText)

        if (!GameHelper.isHidden()) {
            scene.add(0, approachCircle)
            approachCircle.alpha = 0f

            approachCircle.choreographAt(hitTime) { sequence -> sequence
                .fadeTo(0.75f)
                .fadeTo(1f, duration)
                .scaleTo(2f)
                .scaleTo(0f, duration) }
        }

        scene.add(0, circle)
        scene.add(0, metre)
        scene.add(0, background)

        val fadeDuration = timePreempt * 0.25f
        val fadeInStartTime = hitTime - fadeDuration

        if (background.isVisible) {
            background.alpha = 0f
            background.choreographAt(fadeInStartTime) { sequence -> sequence.fadeIn(fadeDuration) }
        }

        circle.alpha = 0f
        circle.choreographAt(fadeInStartTime) { sequence -> sequence.fadeIn(fadeDuration) }


        metre.alpha = 0f
        metre.choreographAt(fadeInStartTime) { sequence -> sequence.fadeIn(fadeDuration) }

        spinText.alpha = 0f
        spinText.choreographAt(fadeInStartTime) { sequence -> sequence
            .fadeIn(fadeDuration)
            .then(timePreempt / 2)
            .fadeOut(fadeDuration) }

        oldMouse = null

        lifetimeEnd = Float.MAX_VALUE
    }

    protected open fun detachFromScene() {
        if (scene == null) {
            return
        }

        clearText.clearAnimations()
        clearText.removeSelf()

        spinText.clearAnimations()
        spinText.removeSelf()

        background.clearAnimations()
        background.removeSelf()

        approachCircle.clearAnimations()
        approachCircle.removeSelf()

        circle.clearAnimations()
        circle.removeSelf()

        metre.clearAnimations()
        metre.removeSelf()

        bonusScore.removeSelf()
        lifetimeEnd = hitTime + duration
        scene = null
    }

    open fun removeFromScene() {
        if (scene == null) {
            return
        }

        detachFromScene()

        val listener = listener!!
        val replayObjectData = replayObjectData

        var score = 0
        if (replayObjectData != null) {
            //int bonusRot = (int) (replayData.accuracy / 4 - needRotations + 1);
            //while (bonusRot < 0) {
            //    bonusRot++;
            //    listener.onSpinnerHit(id, 1000, false, 0);
            //}

            //if (rotations count < the rotations in replay), let rotations count = the rotations in replay
            while (fullRotations + this.bonusScoreCounter < replayObjectData.accuracy / 4 + 1) {
                fullRotations++
                listener.onSpinnerHit(id, 1000, false, 0)
            }
            if (fullRotations >= needRotations)
                clear = true
        }
        var percentfill = (abs(rotations) + fullRotations) / needRotations
        if (needRotations <= 0.1f) {
            clear = true
            percentfill = 1f
        }
        if (percentfill > 0.9f) {
            score = 50
        }
        if (percentfill > 0.95f) {
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

                if (clear) {
                    scene!! += clearText
                }

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

            circle.rotationZ = toDegrees(Utils.direction(currMouse))

            val len1 = Utils.length(currMouse)
            val len2 = Utils.length(oldMouse)
            var dfill = (currMouse.x / len1) * (oldMouse.y / len2) - (currMouse.y / len1) * (oldMouse.x / len2)

            if (abs(len1) < 0.0001f || abs(len2) < 0.0001f)
                dfill = 0f

            if (autoPlay) {
                dfill = 5 * 4 * dt
                circle.rotationZ = (rotations + dfill / 4f) * 360
                //auto时，FL光圈绕中心旋转
                if (GameHelper.isAutoplay() || GameHelper.isAutopilot()) {
                    val angle = (rotations + dfill / 4f) * 360
                    val pX = position.x + 50 * sin(angle.toDouble()).toFloat()
                    val pY = position.y + 50 * cos(angle.toDouble()).toFloat()
                    listener.updateAutoBasedPos(pX, pY)
                }
            }

            rotations += dfill / 4f
            var percentfill = (abs(rotations) + fullRotations) / needRotations

            if (dfill != 0f) {
                updateSpinSampleFrequency(percentfill)
                spinnerSpinSample.play()
            } else {
                spinnerSpinSample.stopAll()
            }

            if (percentfill > 1 || clear) {
                percentfill = 1f

                if (!clear) {
                    scene!! += clearText
                    clearText.alpha = 0f
                    clearText.fadeIn(0.25f)
                    clearText.scale = 1.5f
                    clearText.scaleTo(1f, 0.25f)

                    clear = true

                    // rotations is intentionally NOT reset here. Resetting would shift the first bonus threshold from
                    // ceil(needRotations) to needRotations + 1, matching osu!stable but reducing the highest possible
                    // obtainable score and altering leaderboard balance.
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

            metre.setSize(Config.getRES_WIDTH().toFloat(), fullHeight * min(1f, abs(percentfill)))

            oldMouse.set(currMouse)
        }

        if (passedTime >= duration) {
            removeFromScene()
        }
    }

    override fun onExpire() {
        detachFromScene()
        stopLoopingSamples()

        for (i in hitSamples.size - 1 downTo 0) {
            hitSamples[i].release()
        }

        hitSamples.clear()
        GameObjectPool.getInstance().putSpinner(this)
    }

    override fun isJudged(): Boolean {
        // In remove spinner lock mode, the spinner is assumed to be judged to allow other objects to be judged while
        // the spinner is still active.
        return Multiplayer.isRemoveSliderLock() || passedTime >= duration
    }

    protected fun applySeekRotations() {
        val listener = listener!!
        val stat = stat!!
        val totalRotations = 5f * passedTime
        val wholeRotations = totalRotations.toInt()

        if (clear) {
            // Already cleared (e.g. zero-duration spinner); all whole rotations are bonus.
            for (i in 0 until wholeRotations) {
                bonusScoreCounter++
                listener.onSpinnerHit(id, 1000, false, 0)
                var rate = 0.375f

                if (GameHelper.getHealthDrain() > 0) {
                    rate = 1 + (GameHelper.getHealthDrain() / 4f)
                }

                stat.changeHp(rate * 0.01f * duration / needRotations)
            }

            rotations = totalRotations - wholeRotations
        } else {
            // ceil(needRotations) - 1 rotations are pre-clear; bonus rotations begin at ceil(needRotations) total.
            val preClear = min(wholeRotations, ceil(needRotations.toDouble()).toInt() - 1)

            for (i in 0 until preClear) {
                fullRotations++
                stat.registerSpinnerHit()
                var rate = 0.375f

                if (GameHelper.getHealthDrain() > 0) {
                    rate = 1 + (GameHelper.getHealthDrain() / 2f)
                }

                stat.changeHp(rate * 0.01f * duration / needRotations)
            }

            if (totalRotations > needRotations) {
                clear = true
                val bonus = max(0, wholeRotations - ceil(needRotations.toDouble()).toInt() + 1)

                for (i in 0 until bonus) {
                    bonusScoreCounter++
                    listener.onSpinnerHit(id, 1000, false, 0)
                    var rate = 0.375f

                    if (GameHelper.getHealthDrain() > 0) {
                        rate = 1 + (GameHelper.getHealthDrain() / 4f)
                    }

                    stat.changeHp(rate * 0.01f * duration / needRotations)
                }
            }

            rotations = totalRotations - wholeRotations
        }
    }

    protected fun reloadHitSounds() {
        val beatmapSpinner = beatmapSpinner!!
        val parsedSamples = beatmapSpinner.samples
        hitSamples.ensureCapacity(parsedSamples.size)

        for (i in 0 until parsedSamples.size) {
            val gameplaySample = GameplayHitSampleInfo.obtain()
            gameplaySample.init(parsedSamples[i])

            if (GameHelper.isSamplesMatchPlaybackRate()) {
                gameplaySample.frequency = GameHelper.getSpeedMultiplier()
            }

            hitSamples.add(gameplaySample)
        }

        spinnerSpinSample.reset()
        spinnerBonusSample.reset()

        val startTime = beatmapSpinner.startTime.toFloat()

        for (i in 0 until beatmapSpinner.auxiliarySamples.size) {
            if (spinnerSpinSample.isInitialized && spinnerBonusSample.isInitialized) {
                break
            }

            val auxiliarySample = beatmapSpinner.auxiliarySamples[i]
            val firstSample = auxiliarySample[0].second

            if (firstSample !is BankHitSampleInfo) {
                continue
            }

            if (firstSample.name == "spinnerbonus") {
                spinnerBonusSample.init(startTime, auxiliarySample)
            } else if (firstSample.name == "spinnerspin") {
                spinnerSpinSample.init(startTime, auxiliarySample)
            }
        }

        spinnerSpinSample.isLooping = true

        val muted = GameHelper.getMuted()

        if (muted != null && muted.affectsHitSounds) {
            val volume = muted.volumeAt(stat!!.combo)

            spinnerSpinSample.volume = volume
            spinnerBonusSample.volume = volume
        }
    }

    protected fun playAndFreeHitSamples(obtainedScore: Int) {
        if (obtainedScore > 0) {
            listener!!.playHitSamples(hitSamples)
        }

        for (i in hitSamples.size - 1 downTo 0) {
            hitSamples[i].release()
        }

        hitSamples.clear()
    }

    override fun stopLoopingSamples() {
        spinnerSpinSample.stopAll()
    }

    protected fun updateSamples(dt: Float) {
        spinnerSpinSample.update(dt)
        spinnerBonusSample.update(dt)
    }

    protected fun updateSpinSampleFrequency(progress: Float) {
        val applyTrackRate = GameHelper.isSamplesMatchPlaybackRate()

        if (isSpinnerFrequencyModulate) {
            // Note that osu!stable sets the frequency directly at BassSoundProvider level.
            // This implementation tries to closely follow that behavior with the default frequency in mind.
            var frequency = min(100000f, 20000 + 40000 * progress) / SongService.defaultFrequency

            if (applyTrackRate) {
                frequency *= GameHelper.getSpeedMultiplier()
            }

            spinnerSpinSample.frequency = frequency
        } else if (applyTrackRate) {
            spinnerSpinSample.frequency = GameHelper.getSpeedMultiplier()
        } else {
            spinnerSpinSample.frequency = 1f
        }
    }

    companion object {
        /**
         * Converts [radians] to degrees as `Math.toDegrees` does.
         */
        @JvmStatic
        protected fun toDegrees(radians: Float) = (radians.toDouble() * (180.0 / PI)).toFloat()
    }
}
