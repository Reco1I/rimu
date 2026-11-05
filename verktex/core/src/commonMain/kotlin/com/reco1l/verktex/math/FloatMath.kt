package com.reco1l.verktex.math

import kotlin.math.PI

/**
 * Checks if this float is almost equal to another float, within a given epsilon.
 */
fun Float.almostEquals(other: Float, epsilon: Float = 1e-3f): Boolean {
    return kotlin.math.abs(this - other) <= epsilon
}

/**
 * Checks if this float is almost zero, within a given epsilon.
 */
fun Float.almostZero(epsilon: Float = 1e-3f): Boolean {
    return almostEquals(0f)
}

/**
 * Converts degrees to radians.
 */
fun Float.toRadians() = (this * PI / 180).toFloat()