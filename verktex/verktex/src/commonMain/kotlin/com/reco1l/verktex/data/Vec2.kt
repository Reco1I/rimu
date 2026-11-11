package com.reco1l.verktex.data

import kotlin.math.*

/**
 * Represents a 2D vector with x and y components.
 */
data class Vec2(
    val x: Float,
    val y: Float
) {

    constructor(value: Float = 0f) : this(value, value)


    //region Operators
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(other: Vec2) = Vec2(x * other.x, y * other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(other: Vec2) = Vec2(x / other.x, y / other.y)
    operator fun div(scalar: Float) = Vec2(x / scalar, y / scalar)
    operator fun unaryMinus() = Vec2(-x, -y)
    //endregion


    /**
     * Calculates the distance between this vector and another vector.
     */
    fun distance(other: Vec2) = hypot(x - other.x, y - other.y)

    /**
     * Expands this vector by the horizontal and vertical components of another vector of type Vec4.
     */
    fun expand(other: Vec4) = Vec2(
        x = x + other.horizontal,
        y = y + other.vertical
    )


    companion object {

        /**
         * A vector with both components set to zero (0, 0).
         */
        val Zero = Vec2()

        /**
         * A vector with both components set to one (1, 1).
         */
        val One = Vec2(1f)

    }
}