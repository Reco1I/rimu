package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * Represents a hit object that ends at a different time and/or position than
 * its start time and/or position respectively.
 */
abstract class HitObjectWithLength(
    /**
     * The time at which this hit object starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The time at which this hit object ends, in milliseconds.
     */
    endTime: Double,

    /**
     * The position of the hit object relative to the play field.
     */
    position: Vector2,

    /**
     * The end position of this hit object relative to the play field. Defaults to the start position of this
     * hit object.
     */
    endPosition: Vector2 = position
) : HitObject(startTime, position) {
    /**
     * The time at which this hit object ends, in milliseconds.
     */
    var endTime = endTime
        protected set

    /**
     * The end position of this hit object.
     */
    var endPosition = endPosition
        protected set

    /**
     * The stacked end position of this hit object.
     */
    val stackedEndPosition
        get() = evaluateStackedPosition(endPosition)

    /**
     * The duration of this hit object.
     */
    val duration: Double
        get() = endTime - startTime

    override fun clone() = super.clone() as HitObjectWithLength
}
