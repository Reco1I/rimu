package com.reco1l.verktex.input

/**
 * Estimates the velocity of a pointer from its recent positions, to fling scrollable content.
 *
 * The velocity is the slope of a line fitted to the samples of the last [horizon], so it follows
 * how fast the pointer moved right before being released rather than the whole drag, and a
 * single noisy sample barely affects it.
 */
class VelocityTracker(

    /**
     * How far back samples are used, in milliseconds.
     */
    var horizon: Long = 80,

    /**
     * How long the pointer can stay still before the velocity is considered zero, in
     * milliseconds. Releasing a pointer after holding it in place shouldn't fling.
     */
    var stillnessTimeout: Long = 40,
) {

    private val times = LongArray(CAPACITY)
    private val xs = FloatArray(CAPACITY)
    private val ys = FloatArray(CAPACITY)

    // Ring buffer, start is the oldest sample.
    private var start = 0
    private var count = 0

    /**
     * The estimated velocity on the x-axis in units per second.
     */
    var velocityX = 0f
        private set

    /**
     * The estimated velocity on the y-axis in units per second.
     */
    var velocityY = 0f
        private set


    /**
     * Forgets every sample, e.g. when a new pointer goes down.
     */
    fun clear() {
        start = 0
        count = 0
        velocityX = 0f
        velocityY = 0f
    }

    /**
     * Adds a position of the pointer at the given time in milliseconds and updates the velocity.
     */
    fun addSample(time: Long, x: Float, y: Float) {
        val index = (start + count) % CAPACITY
        times[index] = time
        xs[index] = x
        ys[index] = y

        if (count < CAPACITY) count++ else start = (start + 1) % CAPACITY

        // Drop what is out of the horizon, keeping at least the newest sample.
        while (count > 1 && time - times[start] > horizon) {
            start = (start + 1) % CAPACITY
            count--
        }

        computeVelocity()
    }

    /**
     * Updates the velocity for the pointer being released at the given time in milliseconds,
     * which is zero if it stayed still for too long before.
     */
    fun release(time: Long) {
        if (count == 0 || time - times[(start + count - 1) % CAPACITY] > stillnessTimeout) {
            velocityX = 0f
            velocityY = 0f
        }
    }

    // Least squares fit of position over time, its slope is the velocity.
    private fun computeVelocity() {
        if (count < 2) {
            velocityX = 0f
            velocityY = 0f
            return
        }

        // Relative to the newest sample to keep the sums small.
        val newest = times[(start + count - 1) % CAPACITY]

        var sumT = 0f
        var sumX = 0f
        var sumY = 0f
        for (i in 0 until count) {
            val index = (start + i) % CAPACITY
            sumT += (times[index] - newest) / 1000f
            sumX += xs[index]
            sumY += ys[index]
        }

        val meanT = sumT / count
        val meanX = sumX / count
        val meanY = sumY / count

        var varianceT = 0f
        var covarianceX = 0f
        var covarianceY = 0f
        for (i in 0 until count) {
            val index = (start + i) % CAPACITY
            val t = (times[index] - newest) / 1000f - meanT
            varianceT += t * t
            covarianceX += t * (xs[index] - meanX)
            covarianceY += t * (ys[index] - meanY)
        }

        // Every sample at the same time, there's no way to tell the velocity.
        if (varianceT <= 0f) {
            return
        }

        velocityX = covarianceX / varianceT
        velocityY = covarianceY / varianceT
    }


    private companion object {
        const val CAPACITY = 32
    }
}
