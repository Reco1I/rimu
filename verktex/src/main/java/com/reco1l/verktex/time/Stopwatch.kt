package com.reco1l.verktex.time

/**
 * Measures elapsed time.
 *
 * @param source Provides the current time in nanoseconds.
 */
open class Stopwatch(private val source: () -> Long = System::nanoTime) {

    /**
     * Whether the stopwatch is measuring.
     */
    var isMeasuring = false
        protected set

    private var elapsedNanos = 0L
    private var startTime = 0L

    /**
     * The measured time in seconds.
     */
    val elapsedSeconds: Float
        get() {
            val currentElapsed = if (isMeasuring) elapsedNanos + (source() - startTime) else elapsedNanos
            return currentElapsed / 1e9f
        }

    open fun startMeasuring() {
        if (!isMeasuring) {
            startTime = source()
            isMeasuring = true
        }
    }

    open fun stopMeasuring() {
        if (isMeasuring) {
            elapsedNanos += source() - startTime
            isMeasuring = false
        }
    }

    open fun resetMeasure() {
        elapsedNanos = 0
        isMeasuring = false
    }

    open fun restart() {
        resetMeasure()
        startMeasuring()
    }
}
