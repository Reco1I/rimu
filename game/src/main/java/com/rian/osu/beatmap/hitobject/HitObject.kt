package com.rian.osu.beatmap.hitobject

import com.rian.osu.math.Vector2

/**
 * Represents a hit object.
 */
abstract class HitObject(
    /**
     * The time at which this hit object starts, in milliseconds.
     */
    startTime: Double,

    /**
     * The position of the hit object relative to the play field.
     */
    position: Vector2
) : Cloneable {
    /**
     * The time at which this hit object starts, in milliseconds.
     */
    var startTime = startTime
        protected set

    /**
     * The position of this hit object.
     */
    var position = position
        protected set

    /**
     * The stack height of this hit object.
     */
    var stackHeight = 0

    /**
     * The osu!standard scale of this hit object.
     */
    open var scale = 0f

    /**
     * The radius of this hit object.
     */
    val radius
        get() = (OBJECT_RADIUS * scale).toDouble()

    /**
     * The stack offset vector of this hit object.
     */
    private val stackOffset
        get() = Vector2(stackHeight * scale * -6.4f)

    /**
     * The stacked position of this hit object.
     */
    val stackedPosition
        get() = evaluateStackedPosition(position)

    /**
     * Evaluates a stacked position relative to this hit object.
     *
     * @return The evaluated stacked position.
     */
    protected fun evaluateStackedPosition(position: Vector2) = position + stackOffset

    public override fun clone() =
        (super.clone() as HitObject).apply { position = this@HitObject.position.copy() }

    companion object {
        /**
         * The radius of hit objects (i.e. the radius of a circle) relative to osu!standard.
         */
        const val OBJECT_RADIUS = 64f
    }
}
