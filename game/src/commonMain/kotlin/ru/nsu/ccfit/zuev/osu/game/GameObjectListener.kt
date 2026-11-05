package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.game.Cursor
import com.osudroid.game.GameplayHitSampleInfo
import com.osudroid.math.Vector2
import com.osudroid.utils.BitSet
import com.reco1l.verktex.data.Color4

interface GameObjectListener {

    fun onCircleHit(id: Int, accuracy: Float, pos: Vector2, endCombo: Boolean, forcedScore: Byte, color: Color4)

    fun onSliderHit(id: Int, score: Int, judgementPos: Vector2,
                    endCombo: Boolean, color: Color4, type: Int, incrementCombo: Boolean)

    fun onSliderEnd(id: Int, accuracy: Int, tickSet: BitSet?)

    /**
     * Called when a slider reaches the end of a span and goes back, at the reverse arrow.
     */
    fun onSliderReverse(pos: Vector2, ang: Float, color: Color4)

    fun onSpinnerStart(id: Int)

    fun onSpinnerHit(id: Int, score: Int, endCombo: Boolean, totalScore: Int)

    fun addObject(`object`: GameObject?)

    fun isObjectHittable(`object`: GameObject?): Boolean

    fun getCursor(index: Int): Cursor

    fun getCursorsCount(): Int

    fun registerAccuracy(type: HitObjectType?, acc: Double)

    fun updateAutoBasedPos(pX: Float, pY: Float)

    fun onTrackingSliders(isTrackingSliders: Boolean)

    fun onUpdatedAutoCursor(pX: Float, pY: Float)

    fun playHitSamples(samples: MutableList<GameplayHitSampleInfo>?)

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
