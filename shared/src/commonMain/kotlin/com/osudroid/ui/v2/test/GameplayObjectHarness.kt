package com.osudroid.ui.v2.test

import com.osudroid.settings.GameResolution
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.verktex.Platform
import com.osudroid.math.Vector2
import com.osudroid.GameMode
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.game.Cursor
import com.osudroid.game.CursorEvent
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.mods.ModAutoplay
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.SettingKey
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.testing.UITestStage
import com.reco1l.verktex.time.ManualClock
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.textButton
import com.osudroid.utils.BitSet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameObject
import ru.nsu.ccfit.zuev.osu.game.GameObjectListener
import ru.nsu.ccfit.zuev.osu.game.GameObjectPool
import ru.nsu.ccfit.zuev.osu.game.GameplayEffects
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Plays gameplay objects (circles, sliders, spinners) on a playfield of their own, the way the
 * game scene does, so they can be tried in the UI test browser. Objects are spawned again once they
 * expire, in a loop, and played as [play] says.
 *
 * It's the [GameObjectListener] of the objects: their time is the time of the harness, which also
 * paces their animations, their judgements show the effects the game shows and are written to the
 * log of the test.
 */
class GameplayObjectHarness(private val stage: UITestStage) : GameObjectListener {

    /**
     * How the objects are played.
     */
    enum class Play(val label: String) {
        /**
         * Hit perfectly by autoplay.
         */
        Autoplay("Autoplay"),

        /**
         * Not touched, so they're missed.
         */
        Miss("Miss"),

        /**
         * A finger spinning around the center of the playfield, for spinners.
         */
        Spin("Spin"),
    }

    /**
     * The difficulty the hit objects are created with.
     */
    val difficulty = BeatmapDifficulty(cs = 4f, ar = 9f, od = 8f, hp = 5f)

    val controlPoints = BeatmapControlPoints()

    val stat = StatisticV2()

    // Holds the layers of the playfield, scaled to fit the stage as the game fits it in the screen.
    private val area = UIContainer().apply {
        setSize(GameResolution.width.toFloat(), GameResolution.height.toFloat())
        anchor = Anchor.Center
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
    }

    // Under the objects, as the game's background layer, for the hit lighting.
    private val background = UIContainer().apply {
        setSize(GameResolution.width.toFloat(), GameResolution.height.toFloat())
    }

    /**
     * The container objects are added to, with the size of the gameplay playfield.
     */
    val playfield = UIContainer().apply {
        setSize(GameResolution.width.toFloat(), GameResolution.height.toFloat())
    }

    /**
     * How the objects are played. Changing it starts the loop again.
     */
    var play = Play.Autoplay
        set(value) {
            field = value
            restart()
        }

    private val clock = ManualClock()

    private val effects = GameplayEffects().apply { setLayers(background, playfield) }

    private var cursor = Cursor()

    private var spinAngle = 0.0

    private var current: Spawned? = null

    private var spawn: ((Double) -> Pair<HitObject, GameObject>)? = null


    init {
        area.clock = clock
        area += background
        area += playfield
        stage += area

        // Left by a game that was played before.
        GameHelper.setHidden(null)
        GameHelper.setTraceable(null)

        GameHelper.setOverallDifficulty(difficulty.od)
        GameHelper.setHealthDrain(difficulty.hp)
        GameHelper.setSpeedMultiplier(1f)
        GameHelper.setOriginalTimePreempt(BeatmapDifficulty.difficultyRange(
            difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ).toFloat())
        GameHelper.setBeatLength(0.5)
        GameHelper.setKiai(false)

        effectButtons()

        stage.onEachFrame { frame ->
            // Fits the playfield in the stage, as the game fits it in the screen.
            val bounds = stage.bounds
            area.scale = min(bounds.width / GameResolution.width, bounds.height / GameResolution.height)

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
     * start at, in milliseconds on the clock of the harness, and returns the gameplay object, already
     * initialized.
     */
    fun loop(factory: (startTime: Double) -> Pair<HitObject, GameObject>) {
        spawn = factory
        restart()
    }

    /**
     * Adds buttons turning the effects of the hits on and off, which are settings of the game.
     */
    private fun effectButtons() {
        fun toggle(label: String, key: SettingKey<Boolean>, y: Float) = stage.textButton {
            x = 16f.dip
            this.y = y.dip
            variant = UIButton.Variant.Surface
            fun update() { text = "$label: ${if (Settings[key]) "on" else "off"}" }
            update()
            onActionUp = {
                Settings[key] = !Settings[key]
                update()
            }
        }

        toggle("Bursts", OsuSettings.BurstEffects, 16f)
        toggle("Hit lighting", OsuSettings.HitLighting, 72f)
    }

    /**
     * Adds a button cycling through the given ways of playing the objects.
     */
    fun playButton(vararg plays: Play) = stage.textButton {
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        x = (-16f).dip
        y = 16f.dip
        variant = UIButton.Variant.Surface
        text = play.label
        onActionUp = {
            play = plays[(plays.indexOf(play) + 1) % plays.size]
            text = play.label
            stage.log("Playing with ${play.label}")
        }
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
        current?.let { expire(it) }
        current = null

        val spawn = spawn ?: return

        // Objects check the mod as well as their own flag.
        GameHelper.setAutoplay(if (play == Play.Autoplay) ModAutoplay() else null)
        cursor = Cursor()

        // The clock only goes forward, effects of the previous objects are timed on it.
        val (hitObject, gameObject) = spawn(clock.time * 1000.0 + SPAWN_DELAY)
        if (play == Play.Autoplay) {
            gameObject.setAutoPlay()
        }

        current = Spawned(hitObject, gameObject)
        effects.setScale(hitObject.screenSpaceGameplayScale)
    }

    private fun step(deltaTime: Float) {
        val current = current ?: return

        if (play == Play.Spin) {
            moveSpinningCursor(deltaTime)
        }

        val lifetimeEnd = current.gameObject.lifetimeEnd

        // As the game scene does: objects are updated until their lifetime ends, then expired.
        if (!current.isExpired) {
            current.gameObject.update(deltaTime)

            if (clock.time >= lifetimeEnd) {
                expire(current)
            }
        }

        // As the game scene does once the objects of the frame were updated.
        cursor.reset(Platform.uptimeMillis, clock.time * 1000)

        // A pause between objects.
        if (clock.time >= lifetimeEnd + 0.5f) {
            restart()
        }
    }

    // A finger pressing at the start, then going around the center of the playfield.
    private fun moveSpinningCursor(deltaTime: Float) {
        spinAngle += deltaTime * SPIN_ROTATIONS_PER_SECOND * 2 * PI

        val event = CursorEvent.obtain()
        event.action = if (cursor.latestEvent == null) PointerEvent.Action.Down else PointerEvent.Action.Move
        event.systemTime = Platform.uptimeMillis
        event.trackTime = clock.time * 1000
        event.position.set(
            GameResolution.width / 2f + SPIN_RADIUS * cos(spinAngle).toFloat(),
            GameResolution.height / 2f + SPIN_RADIUS * sin(spinAngle).toFloat()
        )
        event.trackPosition.set(event.position)
        cursor.addEvent(event)
    }

    private fun expire(spawned: Spawned) {
        if (spawned.isExpired) {
            return
        }
        spawned.isExpired = true

        spawned.gameObject.onExpire()
        // The pool keeps expired objects for the game to reuse them, the test makes new ones.
        GameObjectPool.getInstance().apply {
            circles.remove(spawned.gameObject)
            sliders.remove(spawned.gameObject)
            spinners.remove(spawned.gameObject)
        }
    }

    private fun scoreName(score: Int) = when (score) {
        300 -> "hit300"
        100 -> "hit100"
        50 -> "hit50"
        else -> "hit0"
    }


    //region GameObjectListener

    override fun onCircleHit(id: Int, accuracy: Float, pos: Vector2, endCombo: Boolean, forcedScore: Byte, color: Color4) {
        val hitWindow = current?.hitObject?.hitWindow ?: return
        val error = abs(accuracy) * 1000

        // As the game scene judges it.
        val score = when {
            forcedScore == ResultType.MISS.id || error > hitWindow.mehWindow -> 0
            forcedScore == ResultType.HIT300.id || forcedScore.toInt() == 0 && error <= hitWindow.greatWindow -> 300
            forcedScore == ResultType.HIT100.id || forcedScore.toInt() == 0 && error <= hitWindow.okWindow -> 100
            else -> 50
        }

        if (score > 0) {
            effects.burst(pos, color)
        }
        effects.hit(pos, scoreName(score), color)

        stage.log(if (score == 0) "Circle missed" else "Circle hit $score, offset ${(accuracy * 1000).toInt()}ms")
    }

    override fun onSliderHit(id: Int, score: Int, judgementPos: Vector2, endCombo: Boolean, color: Color4, type: Int, incrementCombo: Boolean) {
        // The whole slider was missed.
        if (score == 0) {
            effects.hit(judgementPos, "hit0", color)
            stage.log("Slider missed")
            return
        }

        // A nested object was missed, it only breaks the combo.
        if (score == -1) {
            stage.log("Slider ${partName(type)} missed")
            return
        }

        val name = when (type) {
            GameObjectListener.SLIDER_START -> {
                if (incrementCombo) effects.burstSliderStart(judgementPos, color)
                "sliderpoint30"
            }
            GameObjectListener.SLIDER_REPEAT -> "sliderpoint30"
            GameObjectListener.SLIDER_TICK -> "sliderpoint10"
            else -> {
                if (incrementCombo) effects.burstSliderEnd(judgementPos, color)
                scoreName(score)
            }
        }

        // The end is judged as the whole slider.
        if (incrementCombo || type == GameObjectListener.SLIDER_END) {
            effects.hit(judgementPos, name, color)
        }
        stage.log("Slider ${partName(type)}, score $score")
    }

    override fun onSliderReverse(pos: Vector2, ang: Float, color: Color4) = effects.burstSliderReverse(pos, ang, color)

    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?) = Unit

    override fun onSpinnerStart(id: Int) = stage.log("Spinner started")

    override fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int) {
        // Bonus spins.
        if (score == 1000) {
            return
        }

        val center = Vector2(GameResolution.width / 2f, GameResolution.height / 2f)
        effects.hit(center, scoreName(score), null)
        stage.log(if (score == 0) "Spinner missed" else "Spinner judged $score")
    }

    override fun addObject(`object`: GameObject?) = Unit

    override fun isObjectHittable(`object`: GameObject?) = true

    override fun getCursor(index: Int) = cursor

    // Always one, as the game always has its cursors: objects played by autoplay go through them too.
    // It's only pressed while spinning.
    override fun getCursorsCount() = 1

    override fun registerAccuracy(type: HitObjectType?, acc: Double) = Unit

    override fun updateAutoBasedPos(pX: Float, pY: Float) = Unit

    override fun onTrackingSliders(isTrackingSliders: Boolean) = Unit

    override fun onUpdatedAutoCursor(pX: Float, pY: Float) = Unit

    override fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?) = Unit

    override val elapsedTime
        get() = clock.time

    override fun isAfterSeek() = false

    override fun isRemoveSliderLock() = Multiplayer.isRemoveSliderLock()

    //endregion

    private fun partName(type: Int) = when (type) {
        GameObjectListener.SLIDER_START -> "head"
        GameObjectListener.SLIDER_REPEAT -> "repeat"
        GameObjectListener.SLIDER_END -> "end"
        GameObjectListener.SLIDER_TICK -> "tick"
        else -> "part"
    }


    private class Spawned(val hitObject: HitObject, val gameObject: GameObject) {
        var isExpired = false
    }


    companion object {
        // Longer than the time the objects are shown before their start, so they appear in a moment.
        private const val SPAWN_DELAY = 1000.0

        private const val SPIN_RADIUS = 120f

        private const val SPIN_ROTATIONS_PER_SECOND = 3.0

        private val COMBO_COLORS = listOf(
            Color4(1f, 0.75f, 0f),
            Color4(0f, 0.8f, 0f),
            Color4(0.07f, 0.49f, 1f),
            Color4(0.95f, 0.1f, 0.22f),
        )
    }
}
