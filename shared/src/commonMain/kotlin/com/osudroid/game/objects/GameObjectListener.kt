package com.osudroid.game.objects

import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.game.Cursor
import com.osudroid.game.GameplayContext
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.PlayfieldMapping
import com.osudroid.game.rules.JudgementResult
import com.osudroid.game.rules.SliderPart
import com.osudroid.math.Vector2
import com.osudroid.utils.BitSet
import ru.nsu.ccfit.zuev.osu.scoring.ResultType

/**
 * Judges the gameplay objects of a play.
 *
 * Positions are in the playfield, in osu!pixels, as hit objects are placed (see [PlayfieldMapping]). The judgements
 * return the texture of the judgement the object shows (`hit300g`, `sliderpoint30`, `hit0`…), null for none.
 */
interface JudgementListener {

    /**
     * Judges a hit circle hit [hitOffset] seconds from its start, early or late (above the hit window for a miss).
     *
     * @param forcedResult The result a replay recorded for the circle, which is its result whatever the offset, null
     * to judge the offset.
     */
    fun onCircleJudged(id: Int, hitOffset: Float, position: Vector2, endCombo: Boolean, forcedResult: ResultType?): String?

    /**
     * Judges the head, a repeat or a tick of a slider, hit or missed. A missed part only breaks the combo.
     */
    fun onSliderPartJudged(id: Int, part: SliderPart, isHit: Boolean, position: Vector2): String?

    /**
     * Judges a slider as a whole, as its end is reached.
     *
     * @param isTracked Whether its end was tracked, which adds to the combo.
     */
    fun onSliderJudged(id: Int, result: JudgementResult, position: Vector2, endCombo: Boolean, isTracked: Boolean): String?

    /**
     * Called once a slider ended, with the accuracy of its head and the parts hit, as replays record them.
     */
    fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?)

    fun onSpinnerStart(id: Int)

    /**
     * Judges a spinner, spun [spins] times.
     */
    fun onSpinnerJudged(id: Int, result: JudgementResult, endCombo: Boolean, spins: Int): String?

    /**
     * Gives a bonus spin of a cleared spinner.
     */
    fun onSpinnerBonus()

    /**
     * Registers the offset of a hit, in seconds, for the hit error.
     */
    fun registerAccuracy(type: HitObjectType?, acc: Double)

    /**
     * Whether [object] is the one that can be hit now: the objects are hit in order.
     */
    fun isObjectHittable(`object`: GameObject?): Boolean

    /**
     * Whether the game has recently seeked and active objects are still in the catch-up frames.
     * Used to suppress hitsounds for slider objects (head, ticks, repeats) that were already passed at the seek target.
     */
    fun isAfterSeek(): Boolean

    /**
     * Whether sliders and spinners count as judged once their start is hit, so the objects after them
     * can be hit while they're still active.
     */
    fun isRemoveSliderLock(): Boolean

    fun onTrackingSliders(isTrackingSliders: Boolean)
}

/**
 * The cursors of a play, which hit its objects, and the cursor of autoplay.
 */
interface GameplayCursors {

    fun getCursor(index: Int): Cursor

    fun getCursorsCount(): Int

    /**
     * Moves the cursor of autoplay to a position in the playfield.
     */
    fun updateAutoBasedPos(pX: Float, pY: Float)

    /**
     * Called when the cursor of autoplay moved, to a position in the space gameplay is laid out in, not the
     * playfield.
     */
    fun onUpdatedAutoCursor(pX: Float, pY: Float)

    /**
     * The mapping of the playfield into the space gameplay is laid out in, where spinners are drawn.
     */
    val playfieldMapping: PlayfieldMapping
}

/**
 * Plays the hit samples of the objects of a play.
 */
interface GameplaySamples {

    fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?)
}

/**
 * What the gameplay objects of a play are given: their judgement, the cursors, the samples and the play itself.
 */
interface GameObjectListener : JudgementListener, GameplayCursors, GameplaySamples {

    /**
     * Adds an object to the play, as its lifetime starts.
     */
    fun addObject(`object`: GameObject?)

    /**
     * What the gameplay of the play depends on: its mods, its beatmap and its timing.
     */
    val context: GameplayContext

    /**
     * The current elapsed time relative to the start of the beatmap, in seconds.
     */
    val elapsedTime: Float
}
