package com.osudroid.game.session

import com.osudroid.beatmaps.timings.BreakPeriod
import com.osudroid.ui.v2.game.BreakAnimator

/**
 * Starts and ends the breaks of a play as it goes on, shown by [animator].
 *
 * @param onBreakStart Called as a break starts.
 * @param onBreakEnd Called as a break ends.
 */
class BreakController(
    private val breakPeriods: Array<BreakPeriod>,
    private val animator: BreakAnimator,
    private val onBreakStart: () -> Unit,
    private val onBreakEnd: () -> Unit
) {
    // The next break to start.
    private var index = 0

    /**
     * Starts the next break once [time], in seconds, reached it, or ends the one shown once it's over.
     */
    fun update(time: Float) {
        if (index < breakPeriods.size) {
            if (!animator.isBreak() && breakPeriods[index].startTime / 1000 <= time) {
                val period = breakPeriods[index++]

                animator.init(period.duration / 1000)
                onBreakStart()
            }
        }

        if (animator.isOver()) {
            onBreakEnd()
        }
    }

    /**
     * Moves to [timeMs], in milliseconds, as a seek does: the break it's in is shown from there.
     *
     * @return Whether [timeMs] is in a break.
     */
    fun seekTo(timeMs: Float): Boolean {
        index = 0

        while (index < breakPeriods.size && breakPeriods[index].endTime <= timeMs) {
            index++
        }

        animator.reset()

        if (index < breakPeriods.size) {
            val period = breakPeriods[index]

            if (period.startTime <= timeMs && timeMs < period.endTime) {
                val elapsedTime = ((timeMs - period.startTime) / 1000.0).toFloat()

                animator.init(period.duration / 1000f, elapsedTime)
                index++
                return true
            }
        }

        return false
    }

    /**
     * Goes back to the first break, as the attempt starts again.
     */
    fun reset() {
        index = 0
    }
}
