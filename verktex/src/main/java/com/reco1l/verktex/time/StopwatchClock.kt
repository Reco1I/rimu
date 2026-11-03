package com.reco1l.verktex.time

/**
 * A clock that advances with real time.
 */
open class StopwatchClock(
    start: Boolean = false,
    source: () -> Long = System::nanoTime
) : AdjustableClock() {

    private val stopwatch = Stopwatch(source)

    private var seekOffset = 0f
    private var rateChangeUsed = 0f
    private var rateChangeAccumulated = 0f

    override val time
        get() = stopwatchCurrentTime + seekOffset

    override val isRunning
        get() = stopwatch.isMeasuring

    private val stopwatchCurrentTime
        get() = (stopwatch.elapsedSeconds - rateChangeUsed) * rate + rateChangeAccumulated

    override var rate = 1f
        set(value) {
            if (field == value) {
                return
            }

            rateChangeAccumulated += (stopwatch.elapsedSeconds - rateChangeUsed) * field
            rateChangeUsed = stopwatch.elapsedSeconds
            field = value
        }


    init {
        if (start) {
            start()
        }
    }


    override fun start() = stopwatch.startMeasuring()

    override fun pause() = stopwatch.stopMeasuring()

    override fun reset() {
        resetAccumulatedRate()
        seekOffset = 0f
        stopwatch.resetMeasure()
    }

    /**
     * Resets the clock and starts it.
     */
    fun restart() {
        resetAccumulatedRate()
        seekOffset = 0f
        stopwatch.restart()
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }

    override fun seek(to: Float): Boolean {
        // The offset that when added to the stopwatch time results in the requested time.
        seekOffset = to - stopwatchCurrentTime
        return true
    }

    private fun resetAccumulatedRate() {
        rateChangeAccumulated = 0f
        rateChangeUsed = 0f
    }
}
