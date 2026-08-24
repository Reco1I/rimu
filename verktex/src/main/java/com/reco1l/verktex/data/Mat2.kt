package com.reco1l.verktex.data

import com.osudroid.math.toRadians
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Represents a 2D transformation matrix. This implementation is **mutable** (for performance purposes)
 * and can be used to perform various transformations such as translation, scaling, and rotation.
 */
data class Mat2(
    var a: Float = 1f,
    var b: Float = 0f,
    var c: Float = 0f,
    var d: Float = 1f,
    var tx: Float = 0f,
    var ty: Float = 0f,
) {

    /**
     * Resets the matrix to the identity matrix, which represents no transformation.
     */
    fun identity() {
        a = 1f
        b = 0f
        c = 0f
        d = 1f
        tx = 0f
        ty = 0f
    }

    /**
     * Sets the values of this matrix to be equal to another matrix.
     */
    fun set(other: Mat2) {
        a = other.a
        b = other.b
        c = other.c
        d = other.d
        tx = other.tx
        ty = other.ty
    }

    /**
     * Translates the matrix by the specified amounts in the x and y directions.
     * This effectively moves the coordinate system by (dx, dy).
     */
    fun translate(dx: Float, dy: Float) {
        tx += dx
        ty += dy
    }

    /**
     * Scales the matrix by the specified factors in the x and y directions.
     * This effectively stretches or shrinks the coordinate system.
     */
    fun scale(sx: Float, sy: Float) {
        a *= sx
        b *= sx
        c *= sy
        d *= sy
        tx *= sx
        ty *= sy
    }

    /**
     * Skews the matrix by the specified angles in the x and y directions (in degrees).
     * This effectively slants the coordinate system by the given angles.
     */
    fun skew(sx: Float, sy: Float) {
        val tanX = tan(sx.toRadians())
        val tanY = tan(sy.toRadians())

        val a1 = a + c * tanY
        val b1 = b + d * tanY
        val c1 = c + a * tanX
        val d1 = d + b * tanX

        a = a1
        b = b1
        c = c1
        d = d1
    }

    /**
     * Rotates the matrix by the specified angle in degrees.
     * This effectively rotates the coordinate system around the origin.
     */
    fun rotate(angle: Float) {
        val angleRad = angle.toRadians()
        val cos = cos(angleRad)
        val sin = sin(angleRad)

        val a1 = a * cos - c * sin
        val b1 = b * cos - d * sin
        val c1 = a * sin + c * cos
        val d1 = b * sin + d * cos
        val tx1 = tx * cos - ty * sin
        val ty1 = tx * sin + ty * cos

        a = a1
        b = b1
        c = c1
        d = d1
        tx = tx1
        ty = ty1
    }

    /**
     * Concatenates this matrix with another matrix, effectively combining their transformations.
     * The resulting matrix will apply the transformations of both matrices in sequence.
     */
    fun concat(other: Mat2) {
        val a1 = a * other.a + c * other.b
        val b1 = b * other.a + d * other.b
        val c1 = a * other.c + c * other.d
        val d1 = b * other.c + d * other.d
        val tx1 = a * other.tx + c * other.ty + tx
        val ty1 = b * other.tx + d * other.ty + ty

        a = a1
        b = b1
        c = c1
        d = d1
        tx = tx1
        ty = ty1
    }

    /**
     * Transforms a point (x, y) using this matrix and returns the resulting point as a Vec2.
     * The transformation is applied in the order of scaling, rotation, and translation.
     */
    fun transform(x: Float, y: Float): Vec2 {
        val newX = a * x + c * y + tx
        val newY = b * x + d * y + ty

        return Vec2(newX, newY)
    }

    /**
     * Transforms a 4D vector (x, y, w, z) using this matrix and returns the resulting vector as a Vec4.
     * The transformation is applied in the order of scaling, rotation, and translation.
     */
    fun transform(x: Float, y: Float, w: Float, z: Float): Vec4 {
        val newX = a * x + c * y + tx
        val newY = b * x + d * y + ty
        val newW = a * w + c * z + tx
        val newZ = b * w + d * z + ty

        return Vec4(newX, newY, newW, newZ)
    }

    /**
     * Inverts this matrix, effectively reversing the transformations it represents.
     * If the matrix is not invertible (i.e., its determinant is zero), an [IllegalStateException] is thrown.
     * The inversion is performed in place, modifying the current matrix.
     */
    fun invert() {
        val determinant = a * d - b * c
        if (determinant == 0f) {
            throw IllegalStateException("Matrix is not invertible")
        }

        val invDet = 1f / determinant
        val a1 = d * invDet
        val b1 = -b * invDet
        val c1 = -c * invDet
        val d1 = a * invDet
        val tx1 = (c * ty - d * tx) * invDet
        val ty1 = (b * tx - a * ty) * invDet

        a = a1
        b = b1
        c = c1
        d = d1
        tx = tx1
        ty = ty1
    }


    companion object {

        /**
         * A constant representing the identity matrix, which has no effect when applied to a vector or point.
         *
         * This shouldn't be modified, as it represents the default state of a transformation matrix.
         * Use the [identity] method to reset a matrix to this state.
         */
        val Identity = Mat2()
            get() = field.apply { identity() }
    }
}
