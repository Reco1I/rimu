package com.reco1l.verktex.time

open class TestClock : AdjustableClock() {
    override var time = 0f
    override var rate = 1f

    override var isRunning = false
        protected set

    override fun reset() {
        time = 0f
        isRunning = false
    }

    override fun start() {
        isRunning = true
    }

    override fun pause() {
        isRunning = false
    }

    override fun seek(to: Float): Boolean {
        time = to
        return true
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }

    /**
     * The current time of this [TestClock], in nanoseconds.
     */
    fun currentTimeLong() = (time * 1e9).toLong()
}