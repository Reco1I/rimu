package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * Represents a spinner.
 */
class Spinner(
    /**
     * The time at which this spinner starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The time at which this spinner ends, in milliseconds.
     */
    endTime: Double
) : HitObjectWithLength(startTime, endTime, Vector2(256f, 192f)) {
    override fun clone() = super.clone() as Spinner
}
