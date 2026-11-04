package com.osudroid.ui.v2.test

import android.graphics.PointF
import com.osudroid.GameMode
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.game.Cursor
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.mods.ModAutoplay
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.testing.UITestStage
import com.reco1l.verktex.time.ManualClock
import com.reco1l.verktex.ui.container.UIContainer
import java.util.BitSet
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.GameObjectPool
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Plays gameplay objects (circles, sliders, spinners) on a playfield of their own, the way the
 * game scene does, so they can be tried in the UI test browser. Objects are played by autoplay and
 * spawned again once they expire, in a loop.
 *
 * It's the [GameObjectListener] of the objects: their time is the time of the harness, which also
 * paces their animations, and their judgements are written to the log of the test.
 */
class GameplayObjectHarness(private val stage: UITestStage) : GameObjectListener {

    /**
     * The difficulty the hit objects are created with.
     */
    val difficulty = BeatmapDifficulty(cs = 4f, ar = 9f, od = 8f, hp = 5f)

    val controlPoints = BeatmapControlPoints()

    val stat = StatisticV2()

    /**
     * The container objects are added to, with the size of the gameplay playfield.
     */
    val playfield = UIContainer().apply {
        setSize(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        anchor = Anchor.Center
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
    }

    private val clock = ManualClock()

    private var current: Spawned? = null

    private var spawn: (() -> Spawned)? = null


    init {
        playfield.clock = clock
        stage += playfield

        GameHelper.setOverallDifficulty(difficulty.od)
        GameHelper.setHealthDrain(difficulty.hp)
        GameHelper.setSpeedMultiplier(1f)
        GameHelper.setOriginalTimePreempt(BeatmapDifficulty.difficultyRange(
            difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ).toFloat())
        GameHelper.setBeatLength(0.5)
        GameHelper.setKiai(false)
        GameHelper.setAutoplay(ModAutoplay())

        stage.onEachFrame { frame ->
            // Fits the playfield in the stage, as the game fits it in the screen.
            val bounds = stage.bounds
            playfield.scale = min(bounds.width / Config.getRES_WIDTH(), bounds.height / Config.getRES_HEIGHT())

            val deltaTime = frame.elapsedFrameTime
            clock.time += deltaTime
            step(deltaTime)
        }

        stage.onDispose {
            current?.let { expire(it) }
            GameHelper.setAutoplay(null)
            GameHelper.setKiai(false)
            GameHelper.setSpeedMultiplier(1f)
        }
    }


    /**
     * Plays the object [factory] creates in a loop. The factory is given the time the object should
     * start at, in milliseconds, and returns the gameplay object, already initialized.
     */
    fun loop(factory: (startTime: Double) -> Pair<HitObject, GameObject>) {
        current?.let { expire(it) }

        spawn = {
            val (hitObject, gameObject) = factory(START_TIME)
            gameObject.setAutoPlay()
            Spawned(hitObject, gameObject)
        }
        restart()
    }

    /**
     * Applies the difficulty and control points of the harness to [hitObject].
     */
    fun <T : HitObject> prepare(hitObject: T): T {
        hitObject.applyDefaults(controlPoints, difficulty, GameMode.Droid)
        hitObject.applySamples(controlPoints)
        return hitObject
    }

    /**
     * The combo color of the n-th object of the loop.
     */
    fun comboColor(index: Int) = COMBO_COLORS[index % COMBO_COLORS.size]


    private fun restart() {
        val spawn = spawn ?: return
        val spawned = spawn()
        current = spawned

        // Starts a bit before the object appears.
        clock.time = ((START_TIME - spawned.hitObject.timePreempt) / 1000 - 0.25).toFloat()
    }

    private fun step(deltaTime: Float) {
        val current = current ?: return
        current.gameObject.update(deltaTime)

        if (clock.time >= current.gameObject.lifetimeEnd + 0.5f) {
            expire(current)
            restart()
        }
    }

    private fun expire(spawned: Spawned) {
        spawned.gameObject.onExpire()
        // The pool keeps expired objects for the game to reuse them, the test makes new ones.
        GameObjectPool.getInstance().apply {
            circles.remove(spawned.gameObject)
            sliders.remove(spawned.gameObject)
            spinners.remove(spawned.gameObject)
        }
        current = null
    }


    //region GameObjectListener

    override fun onCircleHit(id: Int, accuracy: Float, pos: PointF?, endCombo: Boolean, forcedScore: Byte, color: Color4?) =
        stage.log("Circle hit, offset ${(accuracy * 1000).toInt()}ms")

    override fun onSliderHit(id: Int, score: Int, judgementPos: PointF?, endCombo: Boolean, color: Color4?, type: Int, incrementCombo: Boolean) {
        val part = when (type) {
            GameObjectListener.SLIDER_START -> "head"
            GameObjectListener.SLIDER_REPEAT -> "repeat"
            GameObjectListener.SLIDER_END -> "end"
            GameObjectListener.SLIDER_TICK -> "tick"
            else -> "part"
        }
        stage.log("Slider $part, score $score")
    }

    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?) = stage.log("Slider judged, score $accuracy")

    override fun onSpinnerStart(id: Int) = stage.log("Spinner started")

    override fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int) = stage.log("Spinner judged, score $score")

    override fun addObject(`object`: GameObject?) = Unit

    override fun isObjectHittable(`object`: GameObject?) = true

    override fun getCursor(index: Int): Cursor = throw IndexOutOfBoundsException("Autoplay has no cursors")

    override fun getCursorsCount() = 0

    override fun registerAccuracy(type: HitObjectType?, acc: Double) = Unit

    override fun updateAutoBasedPos(pX: Float, pY: Float) = Unit

    override fun onTrackingSliders(isTrackingSliders: Boolean) = Unit

    override fun onUpdatedAutoCursor(pX: Float, pY: Float) = Unit

    override fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?) = Unit

    override fun getElapsedTime() = clock.time

    override fun isAfterSeek() = false

    //endregion


    private class Spawned(val hitObject: HitObject, val gameObject: GameObject)


    companion object {
        private const val START_TIME = 1000.0

        private val COMBO_COLORS = listOf(
            Color4(1f, 0.75f, 0f),
            Color4(0f, 0.8f, 0f),
            Color4(0.07f, 0.49f, 1f),
            Color4(0.95f, 0.1f, 0.22f),
        )
    }
}
