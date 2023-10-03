package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * Represents a hit circle.
 */
class HitCircle(
    /**
     * The start time of this hit circle, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of this hit circle relative to the play field.
     */
    position: Vector2
) : HitObject(startTime, position) {
    override fun clone() = super.clone() as HitCircle
}
