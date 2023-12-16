package com.reco1l.rimu.management.time

import com.reco1l.toolkt.kotlin.BoundConflict
import com.reco1l.toolkt.kotlin.nextOf
import com.reco1l.toolkt.kotlin.previousOf
import com.rian.osu.beatmap.timings.ControlPoint
import com.rian.osu.beatmap.timings.ControlPointManager


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
    val onControlPointChange: ControlPointCursor<T>.() -> Unit

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

                onControlPointChange()
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
        onControlPointChange()
    }


    override fun onClockUpdate(msElapsedTime: Long, msDeltaTime: Long)
    {
        // If the delta time equals 0 means there's nothing to update aka clock is paused.
        if (msDeltaTime == 0L)
            return

        // Determining if the clock if going forward.
        if (msDeltaTime > 0)
        {
            if (msElapsedTime >= next.time)
                current = next

            return
        }

        if (msElapsedTime < current.time)
            current = previous
    }
}

enum class ControlPointType
{
    TIMING,
    DIFFICULTY
}
