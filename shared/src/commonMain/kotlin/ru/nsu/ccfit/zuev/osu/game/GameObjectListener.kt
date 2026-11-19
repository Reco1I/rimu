package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.game.Cursor
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.game.PlayfieldMapping
import com.osudroid.math.Vector2
import com.osudroid.utils.BitSet
import com.reco1l.verktex.data.Color4

/**
 * Listens to the gameplay objects.
 *
 * Positions are in the playfield, in osu!pixels, as hit objects are placed (see [PlayfieldMapping]).
 */
interface GameObjectListener {

    /**
     * Judges a hit circle.
     *
     * @return The texture of the judgement the circle shows (`hit300g`, `hit0`…), null for none.
     */
    fun onCircleHit(id: Int, accuracy: Float, pos: Vector2, endCombo: Boolean, forcedScore: Byte, color: Color4): String?

    /**
     * Judges a part of a slider, of the given [type] ([SLIDER_START]…); a [score] of -1 misses a nested object.
     *
     * @return The texture of the judgement the slider shows (`sliderpoint30`, `hit100`…), null for none.
     */
    fun onSliderHit(id: Int, score: Int, judgementPos: Vector2,
                    endCombo: Boolean, color: Color4, type: Int, incrementCombo: Boolean): String?

    fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?)

    fun onSpinnerStart(id: Int)

    /**
     * Judges a spinner, or gives it a bonus spin with a [score] of 1000.
     *
     * @return The texture of the judgement the spinner shows, null for none (bonus spins).
     */
    fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int): String?

    fun addObject(`object`: GameObject?)

    fun isObjectHittable(`object`: GameObject?): Boolean

    fun getCursor(index: Int): Cursor

    fun getCursorsCount(): Int

    fun registerAccuracy(type: HitObjectType?, acc: Double)

    fun updateAutoBasedPos(pX: Float, pY: Float)

    fun onTrackingSliders(isTrackingSliders: Boolean)

    /**
     * Called when the cursor of autoplay moved, to a position in the space gameplay is laid out in, not the
     * playfield.
     */
    fun onUpdatedAutoCursor(pX: Float, pY: Float)

    fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?)

    /**
     * The mapping of the playfield into the space gameplay is laid out in, where spinners are drawn.
     */
    val playfieldMapping: PlayfieldMapping

    /**
     * The current elapsed time relative to the start of the beatmap, in seconds.
     */
    val elapsedTime: Float

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

    companion object {
        const val SLIDER_START = 1
        const val SLIDER_REPEAT = 2
        const val SLIDER_END = 3
        const val SLIDER_TICK = 4
    }
}
