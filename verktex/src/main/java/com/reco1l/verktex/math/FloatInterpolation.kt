package com.reco1l.verktex.math

/**
 * A collection of float interpolation functions.
 */
object FloatInterpolation {

    /**
     * Interpolates a float value between two start and end values.
     *
     * @param time The current time.
     * @param start The start value.
     * @param end The end value.
     * @param startTime The start time.
     * @param endTime The end time.
     * @param easing The easing function to use.
     *
     * @return The interpolated float value at the specific time.
     */
    fun floatAt(time: Float, start: Float, end: Float, startTime: Float, endTime: Float, easing: Easing = Easing.None): Float {

        if (start == end) {
            return start
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0f) {
            return start
        }

        val t = easing.interpolate(current / duration).coerceIn(0f, 1f)

        return start + t * (end - start)
    }

}