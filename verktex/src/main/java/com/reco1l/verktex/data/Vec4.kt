package com.reco1l.verktex.data

/**
 * Represents a 4D vector with x, y, z, and w components.
 */
data class Vec4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {

    constructor(value: Float = 0f) : this(value, value, value, value)
    constructor(xz: Float, yw: Float) : this(xz, yw, xz, yw)


    /**
     * Returns the left coordinate of the vector, which corresponds to the x component.
     */
    val left: Float
        get() = x

    /**
     * Returns the top coordinate of the vector, which corresponds to the y component.
     */
    val top: Float
        get() = y

    /**
     * Returns the right coordinate of the vector, which corresponds to the z component.
     */
    val right: Float
        get() = z

    /**
     * Returns the bottom coordinate of the vector, which corresponds to the w component.
     */
    val bottom: Float
        get() = w

    /**
     * Returns the vertical sum of the vector, which is the sum of the y and w components.
     */
    val vertical
        get() = y + w

    /**
     * Returns the horizontal sum of the vector, which is the sum of the x and z components.
     */
    val horizontal
        get() = x + z

    /**
     * Returns the width of the vector, which is the difference between the z and x components.
     */
    val width
        get() = z - x

    /**
     * Returns the height of the vector, which is the difference between the w and y components.
     */
    val height
        get() = w - y


    //region Operators
    operator fun plus(other: Vec4) = Vec4(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vec4) = Vec4(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(scalar: Float) = Vec4(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Float) = Vec4(x / scalar, y / scalar, z / scalar, w / scalar)
    operator fun unaryMinus() = Vec4(-x, -y, -z, -w)
    //endregion


    /**
     * Checks if the given point (x, y) is contained within the bounds of this vector.
     *
     * The point is considered contained if it lies within the rectangle defined by the vector's
     * x, y, z, and w components.
     */
    fun contains(x: Float, y: Float): Boolean {
        return x in this.x..this.z && y in this.y..this.w
    }

    fun shrink(other: Vec4): Vec4 {
        return Vec4(x + other.x, y + other.y, z - other.z, w - other.w)
    }


    companion object {

        /**
         * A vector with all components set to zero (0, 0, 0, 0).
         */
        val Zero = Vec4()

        /**
         * A vector with all components set to one (1, 1, 1, 1).
         */
        val One = Vec4(1f)

    }
}