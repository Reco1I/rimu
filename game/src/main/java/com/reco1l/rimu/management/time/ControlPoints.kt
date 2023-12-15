package com.reco1l.rimu.management.time

import com.reco1l.framework.kotlin.nextOf
import com.reco1l.framework.kotlin.previousOf
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
    var current: T = manager.controlPointAt(0.0)
        private set(value)
        {
            if (field != value)
            {
                previous = controlPoints.previousOf(value)
                next = controlPoints.nextOf(value)

                onControlPointChange()
            }
        }

    /**
     * The previous of [current] control point.
     */
    var previous: T? = controlPoints.previousOf(current)
        private set

    /**
     * The next of [current] control point.
     */
    var next: T? = controlPoints.nextOf(current)
        private set


    override fun onClockUpdate(msElapsedTime: Long, msDeltaTime: Long)
    {
        // If the delta time equals 0 means there's nothing to update aka clock is paused.
        if (msDeltaTime == 0L)
            return

        // Determining if the clock if going forward.
        if (msDeltaTime > 0)
        {
            if (msElapsedTime >= (next?.time ?: return))
                current = next ?: return

            return
        }

        if (msElapsedTime < current.time)
            current = previous ?: return
    }
}

enum class ControlPointType
{
    TIMING,
    DIFFICULTY
}
