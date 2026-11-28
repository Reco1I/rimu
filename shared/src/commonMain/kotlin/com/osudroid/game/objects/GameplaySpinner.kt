package com.osudroid.game.objects

import com.osudroid.scoring.Statistic
import com.osudroid.ui.v2.game.GameplayPlayfield

import com.reco1l.verktex.ui.Component

import com.osudroid.game.rules.JudgementResult
import com.osudroid.game.Playfield
import com.osudroid.ui.v2.skinTexture
import com.reco1l.verktex.data.ScaleType
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.GameplaySequenceHitSampleInfo
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SpinnerPiece
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographyStates
import com.reco1l.verktex.ui.container.ClipContainer
import com.reco1l.verktex.ui.container.Container
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
import com.osudroid.audio.MusicService
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.Utils
import com.osudroid.ui.skinning.OsuSkin
import com.osudroid.ui.v2.game.SpinnerRpmCounter
import com.reco1l.verktex.data.fill
import kotlin.random.Random

/**
 * A spinner: it judges the spins, and its skin's [piece] shows them. Each spinner of a beatmap gets its own, which
 * borrows its pieces from the [PiecePool].
 */
class GameplaySpinner(

    /**
     * How the spinner looks, the skin's piece from the pool when null.
     */
    private val style: SpinnerPiece? = null

) : GameObject() {

    private lateinit var piece: SpinnerPiece
    private lateinit var rpmCounter: SpinnerRpmCounter

    private var beatmapSpinner: Spinner? = null
    private var oldMouse: Vector2? = null
    private var listener: GameObjectListener? = null
    private var scene: Container? = null

    // The playfield the judgement is shown in, until the spinner expires.
    private var effectsPlayfield: GameplayPlayfield? = null
    private var fullRotations = 0
    private var rotations = 0f
    private var needRotations = 0f
    private var clear = false
    private var bonusScoreCounter = 1
    private var stat: Statistic? = null
    private var passedTime = 0f
    private var duration = 0f

    private val isSpinnerFrequencyModulate: Boolean
    private val hitSamples = ArrayList<GameplayHitSampleInfo>(5)
    private val spinnerSpinSample: GameplaySequenceHitSampleInfo
    private val spinnerBonusSample: GameplaySequenceHitSampleInfo

    private val currMouse = Vector2(0f)

    // The part of the playfield that was shown when the spinner was laid out, to lay it out again when the window is
    // resized, see layout.
    private var laidOutArea: Vec4? = null

    init {
        // The center of the playfield, where the cursor goes in autoplay.
        position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f)

        isSpinnerFrequencyModulate = OsuSkin.current.utils.spinnerFrequencyModulate
        spinnerSpinSample = GameplaySequenceHitSampleInfo()
        spinnerBonusSample = GameplaySequenceHitSampleInfo()

        // Spinners always end combo.
        endsCombo = true
    }

    /**
     * Places the spinner at its position, the center of the playfield, as big as the part of the playfield that's
     * shown rather than the playfield, in the pixels the [piece] is laid out in.
     */
    private fun layout(playfield: GameplayPlayfield) {
        val area = playfield.visibleArea
        laidOutArea = area

        val pixel = Playfield.SCREEN_PIXEL
        piece.layout(position.x / pixel, position.y / pixel, area.width / pixel)
        rpmCounter.layout(area)
    }

    /**
     * Shows the spinner in [scene], a layer of [playfield], in osu!pixels.
     */
    fun init(listener: GameObjectListener, scene: Container, playfield: GameplayPlayfield,
                  beatmapSpinner: Spinner, rps: Float, stat: Statistic) {
        effectsPlayfield = playfield
        piece = style ?: obtain(SkinComponent.Spinner)
        this += piece
        rpmCounter = PiecePool.instance.obtainRpmCounter()
        fullRotations = 0
        rotations = 0f
        this.scene = scene

        // Below what the layer already has, or above, as the pieces of the skin were.
        if (piece.isBelowLayer) scene.add(0, this) else scene += this
        this.duration = max(beatmapSpinner.duration.toFloat() / 1000f, 0f)
        this.beatmapSpinner = beatmapSpinner

        needRotations = rps * duration
        if (duration < 0.05f) {
            needRotations = 0.1f
        }

        this.listener = listener
        context = listener.context
        this.stat = stat
        clear = duration <= 0f
        bonusScoreCounter = 1
        startHit = false

        reloadHitSounds()

        hitTime = beatmapSpinner.startTime.toFloat() / 1000f
        val timePreempt = beatmapSpinner.timePreempt.toFloat() / 1000f
        passedTime = -timePreempt

        val isBackgroundVisible = !context.isTraceable ||
            (Settings[OsuSettings.ShowFirstApproachCircle] && context.traceable!!.firstObject === beatmapSpinner)

        piece.start(hitTime, duration, timePreempt, isBackgroundVisible, isApproachCircleVisible = !context.isHidden)
        rpmCounter.attach(scene, hitTime, hitTime + duration)
        layout(playfield)

        oldMouse = null
    }

    /**
     * Shows the judgement the game gave the spinner, with the texture [judgement], at the center of the playfield.
     */
    private fun showResult(judgement: String?) {
        judgement ?: return
        val playfield = effectsPlayfield ?: return

        showJudgement(playfield, judgement, position.x, position.y, beatmapSpinner!!.gameplayScale, null, spinnerMiss = true)
    }

    /**
     * Shows [shown], a piece of the [piece], above everything the layer has, as the clear text and the bonus score
     * are. It's in the piece, which places it in its own pixels.
     */
    private fun showOnTop(shown: Component?) {
        if (shown == null || shown.parent != null) {
            return
        }

        piece += shown
        drawIn(scene!!, shown)
    }

    fun removeFromScene() {
        if (scene == null) {
            return
        }

        scene = null
        endLifetime(hitTime + duration)

        val listener = listener!!
        val replayObjectData = replayObjectData

        var score = 0
        if (replayObjectData != null) {
            //int bonusRot = (int) (replayData.accuracy / 4 - needRotations + 1);
            //while (bonusRot < 0) {
            //    bonusRot++;
            //    listener.onSpinnerBonus();
            //}

            //if (rotations count < the rotations in replay), let rotations count = the rotations in replay
            while (fullRotations + this.bonusScoreCounter < replayObjectData.accuracy / 4 + 1) {
                fullRotations++
                listener.onSpinnerBonus()
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
        applyState(if (score > 0) ArmedState.Hit else ArmedState.Miss)
        showResult(listener.onSpinnerJudged(id, JudgementResult.fromScore(score), endsCombo, this.bonusScoreCounter + fullRotations - 1))
        playAndFreeHitSamples(score)
    }

    override fun updateJudgement(dt: Float) {
        // Judged, until its judgement ends.
        if (scene == null) {
            return
        }

        val listener = listener!!
        val stat = stat!!

        val playfield = effectsPlayfield!!
        if (playfield.visibleArea != laidOutArea) {
            layout(playfield)
        }

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
                    showOnTop(piece.playCleared(animate = false))
                }

                if (bonusScoreCounter > 1) {
                    showOnTop(piece.playBonus((bonusScoreCounter - 1) * 1000, animate = false))
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
                        mouse = latestEvent.playfieldPosition
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

            var rotation = toDegrees(Utils.direction(currMouse))

            val len1 = Utils.length(currMouse)
            val len2 = Utils.length(oldMouse)
            var dfill = (currMouse.x / len1) * (oldMouse.y / len2) - (currMouse.y / len1) * (oldMouse.x / len2)

            if (abs(len1) < 0.0001f || abs(len2) < 0.0001f)
                dfill = 0f

            if (autoPlay) {
                dfill = 5 * 4 * dt
                rotation = (rotations + dfill / 4f) * 360
                //auto时，FL光圈绕中心旋转
                if (context.isAutoplay || context.isAutopilot) {
                    val angle = (rotations + dfill / 4f) * 360
                    val pX = position.x + AUTO_RADIUS * sin(angle.toDouble()).toFloat()
                    val pY = position.y + AUTO_RADIUS * cos(angle.toDouble()).toFloat()
                    listener.updateAutoBasedPos(pX, pY)
                }
            }

            rotations += dfill / 4f
            rpmCounter.update(hitTime + passedTime, rotation)
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
                    showOnTop(piece.playCleared(animate = true))

                    clear = true

                    // rotations is intentionally NOT reset here. Resetting would shift the first bonus threshold from
                    // ceil(needRotations) to needRotations + 1, matching osu!stable but reducing the highest possible
                    // obtainable score and altering leaderboard balance.
                }

                if (abs(rotations) > 1) {
                    rotations -= 1 * sign(rotations)
                    showOnTop(piece.playBonus(bonusScoreCounter * 1000, animate = true))
                    listener.onSpinnerBonus()
                    bonusScoreCounter++
                    spinnerBonusSample.play()
                    var rate = 0.375f
                    if (context.healthDrain > 0) {
                        rate = 1 + (context.healthDrain / 4f)
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
                    if (context.healthDrain > 0) {
                        rate = 1 + (context.healthDrain / 2f)
                    }
                    stat.changeHp(rate * 0.01f * duration / needRotations)
                }
            }

            piece.update(rotation, min(1f, abs(percentfill)))

            oldMouse.set(currMouse)
        }

        if (passedTime >= duration) {
            removeFromScene()
        }
    }

    override fun playState(state: ArmedState, time: Float) {
        STATES.play(this, state, time)
    }

    override fun onExpire() {
        // Not judged, when a seek expires it.
        if (scene != null) {
            piece.end()
            scene = null
        }

        PiecePool.instance.releaseRpmCounter(rpmCounter)
        releasePieces()
        effectsPlayfield = null
        removeSelf()
        stopLoopingSamples()

        for (i in hitSamples.size - 1 downTo 0) {
            hitSamples[i].release()
        }

        hitSamples.clear()
    }

    // Detached once it's judged.
    override fun hasFinalJudgement() = scene == null

    override fun isJudged(): Boolean {
        // In remove spinner lock mode, the spinner is assumed to be judged to allow other objects to be judged while
        // the spinner is still active.
        return listener?.isRemoveSliderLock() == true || passedTime >= duration
    }

    private fun applySeekRotations() {
        val listener = listener!!
        val stat = stat!!
        val totalRotations = 5f * passedTime
        val wholeRotations = totalRotations.toInt()

        if (clear) {
            // Already cleared (e.g. zero-duration spinner); all whole rotations are bonus.
            for (i in 0 until wholeRotations) {
                bonusScoreCounter++
                listener.onSpinnerBonus()
                var rate = 0.375f

                if (context.healthDrain > 0) {
                    rate = 1 + (context.healthDrain / 4f)
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

                if (context.healthDrain > 0) {
                    rate = 1 + (context.healthDrain / 2f)
                }

                stat.changeHp(rate * 0.01f * duration / needRotations)
            }

            if (totalRotations > needRotations) {
                clear = true
                val bonus = max(0, wholeRotations - ceil(needRotations.toDouble()).toInt() + 1)

                for (i in 0 until bonus) {
                    bonusScoreCounter++
                    listener.onSpinnerBonus()
                    var rate = 0.375f

                    if (context.healthDrain > 0) {
                        rate = 1 + (context.healthDrain / 4f)
                    }

                    stat.changeHp(rate * 0.01f * duration / needRotations)
                }
            }

            rotations = totalRotations - wholeRotations
        }
    }

    private fun reloadHitSounds() {
        val beatmapSpinner = beatmapSpinner!!
        val parsedSamples = beatmapSpinner.samples
        hitSamples.ensureCapacity(parsedSamples.size)

        for (i in 0 until parsedSamples.size) {
            val gameplaySample = GameplayHitSampleInfo.obtain()
            gameplaySample.init(parsedSamples[i])

            if (context.samplesMatchPlaybackRate) {
                gameplaySample.frequency = context.timing.speedMultiplier
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

        val muted = context.muted

        if (muted != null && muted.affectsHitSounds) {
            val volume = muted.volumeAt(stat!!.combo)

            spinnerSpinSample.volume = volume
            spinnerBonusSample.volume = volume
        }
    }

    private fun playAndFreeHitSamples(obtainedScore: Int) {
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

    private fun updateSamples(dt: Float) {
        spinnerSpinSample.update(dt)
        spinnerBonusSample.update(dt)
    }

    private fun updateSpinSampleFrequency(progress: Float) {
        val applyTrackRate = context.samplesMatchPlaybackRate

        if (isSpinnerFrequencyModulate) {
            // Note that osu!stable sets the frequency directly at BassSoundProvider level.
            // This implementation tries to closely follow that behavior with the default frequency in mind.
            var frequency = min(100000f, 20000 + 40000 * progress) / MusicService.DEFAULT_FREQUENCY

            if (applyTrackRate) {
                frequency *= context.timing.speedMultiplier
            }

            spinnerSpinSample.frequency = frequency
        } else if (applyTrackRate) {
            spinnerSpinSample.frequency = context.timing.speedMultiplier
        } else {
            spinnerSpinSample.frequency = 1f
        }
    }

    private companion object {

        // What the spinner shows as it's judged: hit or missed, its piece ends the same.
        val STATES = choreographyStates<ArmedState, GameplaySpinner> {
            otherwise {
                piece.end()
                rpmCounter.detach()
            }
        }

        // How far from the centre autoplay spins, 50 pixels of osu!droid's 1280x720 screen.
        const val AUTO_RADIUS = 50 * Playfield.SCREEN_PIXEL

        /**
         * Converts [radians] to degrees as `Math.toDegrees` does.
         */
        fun toDegrees(radians: Float) = (radians.toDouble() * (180.0 / PI)).toFloat()
    }
}
