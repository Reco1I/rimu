package com.osudroid.math

import android.graphics.PointF

// Conversions between Vector2 and Android's PointF for the code that still uses PointF. Vector2 lives
// in the game module, which can't depend on Android.

/**
 * Creates a [Vector2] from a [PointF].
 */
fun Vector2(pointF: PointF) = Vector2(pointF.x, pointF.y)

/**
 * Converts this vector to a [PointF].
 *
 * @return The converted [PointF].
 */
fun Vector2.toPointF() = PointF(x, y)
