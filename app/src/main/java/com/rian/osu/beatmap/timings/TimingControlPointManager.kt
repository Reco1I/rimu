package com.rian.osu.beatmap.timings

/**
 * A manager for timing control points.
 */
class TimingControlPointManager : ControlPointManager<TimingControlPoint>(
    TimingControlPoint(0.0, 1000.0, 4)
) {
    override fun controlPointAt(time: Double) =
        binarySearchWithFallback(time, if (controlPoints.isNotEmpty()) controlPoints[0] else defaultControlPoint)

    override fun clone() = super.clone() as TimingControlPointManager
}
