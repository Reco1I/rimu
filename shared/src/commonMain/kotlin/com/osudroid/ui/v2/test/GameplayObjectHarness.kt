package com.osudroid.ui.v2.test

import com.osudroid.game.rules.JudgementResult
import com.osudroid.game.rules.SliderPart
import com.osudroid.scoring.Statistic
import com.reco1l.verktex.Verktex
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
import com.osudroid.game.GameplayContext
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.Playfield
import com.osudroid.game.rules.HitJudging
import com.osudroid.ui.v2.game.GameplayPlayfield
import com.osudroid.mods.ModAutoplay
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.SettingKey
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.testing.UITestStage
import com.reco1l.verktex.time.ManualClock
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.textButton
import com.osudroid.utils.BitSet
import com.osudroid.utils.ModHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import com.osudroid.game.objects.GameObject
import com.osudroid.game.objects.GameObjectListener
import ru.nsu.ccfit.zuev.osu.scoring.ResultType

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

    val stat = Statistic()

    /**
     * What the objects depend on: the difficulty of the harness, and autoplay while they're played by it.
     */
    override var context = createContext()
        private set

    // The size of the space gameplay is laid out in, as the game's: 1280 wide, in the proportions of the window.
    private val spaceWidth = Playfield.SPACE_WIDTH
    private val spaceHeight = Verktex.graphics.window.let { spaceWidth * it.windowHeight / it.windowWidth.coerceAtLeast(1) }

    // Holds the layers of the playfield, scaled to fit the stage as the game fits it in the screen. It keeps the
    // size of the game's even when the stage is smaller, as what's in it is laid out for that size.
    private val area = object : Container() {
        override fun adjustParentConstraints(constraints: LayoutConstraints): LayoutConstraints {
            return LayoutConstraints(spaceWidth, spaceWidth, spaceHeight, spaceHeight)
        }
    }.apply {
        setSize(spaceWidth, spaceHeight)
        anchor = Anchor.Center
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
    }

    // The game's playfield.
    private val gameplayPlayfield = GameplayPlayfield().apply {
        setSpaceSize(spaceWidth, spaceHeight)
    }

    /**
     * The playfield the objects are added to, in osu!pixels, as the game's.
     */
    val playfield
        get() = gameplayPlayfield

    /**
     * How the objects are played. Changing it starts the loop again.
     */
    var play = Play.Autoplay
        set(value) {
            field = value
            restart()
        }

    private val clock = ManualClock()

    private var cursor = Cursor()

    private var spinAngle = 0.0

    private var current = emptyList<Spawned>()

    private var spawn: ((Double) -> List<Pair<HitObject, GameObject>>)? = null


    init {
        area.clock = clock
        area += gameplayPlayfield
        stage += area

        effectButtons()

        stage.onEachFrame { frame ->
            // Fits the playfield in the stage, as the game fits it in the screen.
            val bounds = stage.bounds
            area.scale = min(bounds.width / spaceWidth, bounds.height / spaceHeight)

            val deltaTime = frame.elapsedFrameTime
            clock.time += deltaTime
            step(deltaTime)
        }

        stage.onDispose {
            expireAll()
        }
    }


    /**
     * Plays the object [factory] creates in a loop. The factory is given the time the object should
     * start at, in milliseconds on the clock of the harness, and returns the gameplay object, already
     * initialized.
     */
    fun loop(factory: (startTime: Double) -> Pair<HitObject, GameObject>) = loopAll { listOf(factory(it)) }

    /**
     * Plays the objects [factory] creates together in a loop, as [loop] plays one: each is updated
     * until its own lifetime ends, and the loop starts again once all of them have expired. The
     * factory is given the time the first object should start at.
     */
    fun loopAll(factory: (startTime: Double) -> List<Pair<HitObject, GameObject>>) {
        spawn = factory
        restart()
    }

    /**
     * Called as the loop starts again, before the objects are created, to clear what the previous
     * objects left on the playfield (e.g. their follow points).
     */
    var onRestart: (() -> Unit)? = null

    /**
     * Adds buttons turning the effects of the hits on and off, which are settings of the game.
     */
    private fun effectButtons() {
        fun toggle(label: String, key: SettingKey<Boolean>, y: Float) = stage.textButton {
            x = 16f.dip
            this.y = y.dip
            variant = Button.Variant.Surface
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
        variant = Button.Variant.Surface
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
        expireAll()
        current = emptyList()
        onRestart?.invoke()

        val spawn = spawn ?: return

        // Objects check the mod as well as their own flag.
        context = createContext(isAutoplay = play == Play.Autoplay)
        cursor = Cursor()

        // The clock only goes forward, effects of the previous objects are timed on it.
        current = spawn(clock.time * 1000.0 + SPAWN_DELAY).map { (hitObject, gameObject) ->
            if (play == Play.Autoplay) {
                gameObject.setAutoPlay()
            }
            Spawned(hitObject, gameObject)
        }
    }

    private fun createContext(isAutoplay: Boolean = false) = GameplayContext(
        mods = ModHashMap().apply { if (isAutoplay) put(ModAutoplay::class) },
        healthDrain = difficulty.hp,
        originalTimePreempt = BeatmapDifficulty.difficultyRange(
            difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN
        ).toFloat()
    ).also {
        it.timing.beatLength = 0.5
    }

    private fun step(deltaTime: Float) {
        val current = current
        if (current.isEmpty()) return

        if (play == Play.Spin) {
            moveSpinningCursor(deltaTime)
        }

        var lifetimeEnd = 0f

        // As the game scene does: objects are updated until their lifetime ends, then expired.
        for (spawned in current) {
            if (!spawned.isExpired) {
                spawned.gameObject.updateJudgement(deltaTime)

                if (clock.time >= spawned.gameObject.lifetimeEnd) {
                    expire(spawned)
                }
            }
            lifetimeEnd = max(lifetimeEnd, spawned.gameObject.lifetimeEnd)
        }

        // As the game scene does once the objects of the frame were updated.
        cursor.reset(Platform.uptimeMillis, clock.time * 1000)

        // A pause between objects.
        if (clock.time >= lifetimeEnd + 0.5f) {
            restart()
        }
    }

    private fun expireAll() {
        for (spawned in current) expire(spawned)
    }

    // A finger pressing at the start, then going around the center of the playfield.
    private fun moveSpinningCursor(deltaTime: Float) {
        spinAngle += deltaTime * SPIN_ROTATIONS_PER_SECOND * 2 * PI

        val event = CursorEvent.obtain()
        event.action = if (cursor.latestEvent == null) PointerEvent.Action.Down else PointerEvent.Action.Move
        event.systemTime = Platform.uptimeMillis
        event.trackTime = clock.time * 1000
        event.playfieldPosition.set(
            Playfield.WIDTH / 2 + SPIN_RADIUS * cos(spinAngle).toFloat(),
            Playfield.HEIGHT / 2 + SPIN_RADIUS * sin(spinAngle).toFloat()
        )
        event.trackPosition.set(event.playfieldPosition)
        cursor.addEvent(event)
    }

    private fun expire(spawned: Spawned) {
        if (spawned.isExpired) {
            return
        }
        spawned.isExpired = true

        spawned.gameObject.onExpire()
    }

    private fun scoreName(score: Int) = when (score) {
        300 -> "hit300"
        100 -> "hit100"
        50 -> "hit50"
        else -> "hit0"
    }


    //region GameObjectListener

    override fun onCircleJudged(id: Int, hitOffset: Float, position: Vector2, endCombo: Boolean, forcedResult: ResultType?): String? {
        // The objects of a loop share their difficulty, and so their hit window.
        val hitWindow = current.firstOrNull()?.hitObject?.hitWindow ?: return null
        val score = HitJudging.judge(hitOffset, hitWindow, forcedResult).score

        stage.log(if (score == 0) "Circle missed" else "Circle hit $score, offset ${(hitOffset * 1000).toInt()}ms")
        return scoreName(score)
    }

    override fun onSliderPartJudged(id: Int, part: SliderPart, isHit: Boolean, position: Vector2): String? {
        val name = part.name.lowercase()

        // A missed part only breaks the combo.
        if (!isHit) {
            stage.log("Slider $name missed")
            return null
        }

        stage.log("Slider $name hit")
        return if (part == SliderPart.Tick) "sliderpoint10" else "sliderpoint30"
    }

    override fun onSliderJudged(id: Int, result: JudgementResult, position: Vector2, endCombo: Boolean, isTracked: Boolean): String? {
        stage.log(if (result == JudgementResult.Miss) "Slider missed" else "Slider end, score ${result.score}")
        return scoreName(result.score)
    }

    override fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?) = Unit

    override fun onSpinnerStart(id: Int) = stage.log("Spinner started")

    override fun onSpinnerBonus() = Unit

    override fun onSpinnerJudged(id: Int, result: JudgementResult, endCombo: Boolean, spins: Int): String? {
        stage.log(if (result == JudgementResult.Miss) "Spinner missed" else "Spinner judged ${result.score}")
        return scoreName(result.score)
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



    private class Spawned(val hitObject: HitObject, val gameObject: GameObject) {
        var isExpired = false
    }


    companion object {
        // Longer than the time the objects are shown before their start, so they appear in a moment.
        private const val SPAWN_DELAY = 1000.0

        private const val SPIN_RADIUS = 80f

        private const val SPIN_ROTATIONS_PER_SECOND = 3.0

        private val COMBO_COLORS = listOf(
            Color4(1f, 0.75f, 0f),
            Color4(0f, 0.8f, 0f),
            Color4(0.07f, 0.49f, 1f),
            Color4(0.95f, 0.1f, 0.22f),
        )
    }
}
