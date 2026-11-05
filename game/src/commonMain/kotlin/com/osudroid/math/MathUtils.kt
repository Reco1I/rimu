package com.osudroid.math

import kotlin.jvm.JvmStatic
import kotlin.math.PI

/**
 * Float constants and helpers for the code that works in [Float]s.
 */
object MathUtils {

    /**
     * [PI] as a [Float].
     */
    const val PI_F = PI.toFloat()

    /**
     * Half of [PI] as a [Float].
     */
    const val HALF_PI_F = (PI * 0.5).toFloat()

    /**
     * Clamps [value] to [min] and [max]. Unlike [coerceIn], it doesn't check that [min] isn't greater than
     * [max], and a NaN [value] gives [max].
     */
    @JvmStatic
    fun clamp(value: Float, min: Float, max: Float) = if (value < min) min else if (value < max) value else max

    /**
     * Clamps [value] to [min] and [max]. Unlike [coerceIn], it doesn't check that [min] isn't greater than
     * [max], and a NaN [value] gives [max].
     */
    @JvmStatic
    fun clamp(value: Double, min: Double, max: Double) = if (value < min) min else if (value < max) value else max
}
