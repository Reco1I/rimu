package com.osudroid.game.rules

import com.osudroid.beatmaps.timings.BreakPeriod
import kotlin.math.max
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * Drains the health of a play over time and decides when it fails.
 *
 * @param statistic The statistic whose health is drained.
 * @param drainRate The HP of the beatmap, its health drain.
 * @param lives The lives the play has once its health runs out, three with Easy, each restoring all of it.
 */
class HealthProcessor(
    private val statistic: StatisticV2,
    private val drainRate: Float,
    private val lives: Int = 0
) {

    /**
     * The lives used so far.
     */
    var usedLives = 0
        private set

    /**
     * The health drained per second, in hundredths, between objects [distanceToNextObject] seconds apart.
     */
    fun drainPerSecond(distanceToNextObject: Double): Double {
        if (drainRate > 0 && distanceToNextObject > 0) {
            return 1 + drainRate / (2 * distanceToNextObject)
        }

        return 0.375
    }

    /**
     * Drains the health of a frame [deltaTime] seconds long.
     *
     * @return Whether the play failed: its health ran out, it can fail and it has no lives left.
     */
    fun drain(deltaTime: Float, distanceToNextObject: Double): Boolean {
        statistic.changeHp((-drainPerSecond(distanceToNextObject)).toFloat() * 0.01f * deltaTime)

        return checkFailed()
    }

    /**
     * Drains the health of [seconds] seconds at once, as a seek does, using as many lives as it would have used
     * frame by frame.
     *
     * @return Whether the play failed.
     */
    fun drainSegment(seconds: Double, distanceToNextObject: Double): Boolean {
        // Drained incrementally, so that a long section can use more than one life: a single change clamps at 0 and
        // loses what's left.
        var remaining = (drainPerSecond(distanceToNextObject) * 0.01 * seconds).toFloat()

        while (remaining > 0) {
            val hp = statistic.hp

            if (remaining < hp) {
                statistic.changeHp(-remaining)
                return false
            }

            remaining -= hp
            statistic.changeHp(-hp)

            if (!statistic.canFail) {
                return false
            }

            if (!useLife()) {
                return true
            }
        }

        return false
    }

    /**
     * Whether the play failed now, using a life instead when it has one.
     */
    fun checkFailed(): Boolean {
        if (statistic.hp > 0 || !statistic.canFail) {
            return false
        }

        return !useLife()
    }

    /**
     * Restores the lives, as after a seek.
     */
    fun reset() {
        usedLives = 0
    }

    private fun useLife(): Boolean {
        if (usedLives >= lives) {
            return false
        }

        usedLives++
        statistic.changeHp(1f)
        return true
    }

    companion object {

        /**
         * The seconds from [startTime] to [endTime], in milliseconds, out of [breaks].
         *
         * @param firstBreak The index of the first break that may overlap them: the breaks before it end before
         * [startTime].
         */
        fun drainingSeconds(startTime: Double, endTime: Double, breaks: Array<BreakPeriod>?, firstBreak: Int = 0): Double {
            if (startTime >= endTime) {
                return 0.0
            }

            var total = endTime - startTime

            if (breaks != null) {
                for (i in firstBreak until breaks.size) {
                    val period = breaks[i]

                    if (period.startTime >= endTime) {
                        break
                    }

                    val overlap = min(period.endTime.toDouble(), endTime) - max(period.startTime.toDouble(), startTime)

                    if (overlap > 0) {
                        total -= overlap
                    }
                }
            }

            return max(0.0, total) / 1000
        }
    }
}
