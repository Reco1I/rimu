package com.reco1l.rimu.management.time

import com.reco1l.toolkt.kotlin.BoundConflict
import com.reco1l.toolkt.kotlin.nextOf
import com.reco1l.toolkt.kotlin.previousOf
import com.rian.osu.beatmap.timings.ControlPoint
import com.rian.osu.beatmap.timings.ControlPointManager


fun interface CursorListener<T : ControlPoint>
{
    fun onControlPointChange(previous: T, current: T, next: T)
}

/**
 * Cursor to determine the current control point at current time defined by a [GameClock].
 */
class ControlPointCursor<T : ControlPoint>(

    /**
     * The control point manager.
     */
    val manager: ControlPointManager<T>,

    /**
     * Called when the [current] control point has been changed.
     */
    val onChange: (previous: T, current: T, next: T) -> Unit

) : IClockObserver
{

    private var controlPoints = manager.getControlPoints()


    /**
     * The current active control point.
     */
    var current: T = controlPoints[0]
        private set(value)
        {
            if (field != value)
            {
                field = value
                previous = controlPoints.previousOf(value, BoundConflict.CLAMP)!!
                next = controlPoints.nextOf(value, BoundConflict.CLAMP)!!

                onChange(previous, current, next)
            }
        }

    /**
     * The previous of [current] control point.
     */
    var previous: T = current
        private set

    /**
     * The next of [current] control point.
     */
    var next: T = controlPoints.nextOf(current, BoundConflict.CLAMP)!!
        private set


    init
    {
        onChange(previous, current, next)
    }


    override fun onClockUpdate(sElapsedTime: Double, sDeltaTime: Float)
    {
        // If the delta time equals 0 means there's nothing to update aka clock is paused.
        if (sDeltaTime == 0f)
            return

        // Determining if the clock if going forward.
        if (sDeltaTime > 0f)
        {
            if (sElapsedTime >= next.time / 1000.0)
                current = next

            return
        }

        if (sElapsedTime < current.time / 1000.0)
            current = previous
    }
}

enum class ControlPointType
{
    TIMING,
    DIFFICULTY
}
