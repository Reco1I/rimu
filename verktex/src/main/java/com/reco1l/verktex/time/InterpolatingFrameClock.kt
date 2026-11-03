package com.reco1l.verktex.time

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign

/**
 * A clock that advances smoothly between the updates of a source that doesn't update its time on every frame,
 * such as an audio track, following real time while staying close to the source.
 */
class InterpolatingFrameClock(
    source: Clock? = null,
    realTimeClockSource: Clock = StopwatchClock(true)
) : FrameClock(), IFrameProcessableClock, ISourceChangeableClock {

    /**
     * The biggest difference with the source allowed before jumping to its time, in seconds.
     */
    var allowableErrorSeconds = 2f / 60

    /**
     * The time it takes to halve the difference with the source, in seconds.
     */
    var driftRecoveryHalfLife = 0.05f

    /**
     * Whether the time is being interpolated at the moment.
     */
    var isInterpolating = false
        private set

    /**
     * The difference with the source.
     */
    val drift
        get() = time - framedSource.time

    override val rate
        get() = framedSource.rate

    override var isRunning = false
        private set

    override var elapsedFrameTime = 0f
        private set

    override var lastFrameTime = 0f
        private set

    override var time = 0f
        private set

    override lateinit var source: Clock
        private set

    private lateinit var framedSource: FrameClock

    private val realTimeClock = asFrameClock(realTimeClockSource)

    private var interpolatedTime = 0f


    init {
        changeSource(source)
    }


    private fun asFrameClock(clock: Clock): FrameClock {
        return if (clock is FrameClock && clock is IFrameProcessableClock) clock else FramedClock(clock)
    }

    override fun changeSource(source: Clock?) {
        if (source != null && ::source.isInitialized && this.source == source) {
            return
        }

        this.source = source ?: StopwatchClock(true)

        framedSource = asFrameClock(this.source)
        isInterpolating = false
        interpolatedTime = framedSource.time
    }

    override fun processFrame() {
        val lastTime = interpolatedTime

        (realTimeClock as IFrameProcessableClock).processFrame()
        (framedSource as IFrameProcessableClock).processFrame()

        val sourceIsRunning = framedSource.isRunning
        val sourceHasElapsed = framedSource.elapsedFrameTime != 0f

        try {
            if (!sourceIsRunning) {
                // While the source isn't running, we remain in the current interpolation mode unless a seek
                // is performed. This ensures the most consistent playback possible, and avoids fractional
                // differences when stopping and starting the source.
                if (sourceHasElapsed) {
                    isInterpolating = false
                    interpolatedTime = framedSource.time
                }
                return
            }

            if (isInterpolating) {
                val elapsedRealTime = realTimeClock.elapsedFrameTime

                interpolatedTime += elapsedRealTime * rate

                // If we differ from the time of the source, gradually approach it. The remaining error halves
                // every half-life.
                interpolatedTime = framedSource.time +
                    (interpolatedTime - framedSource.time) * 0.5f.pow(elapsedRealTime / driftRecoveryHalfLife)

                if (abs(framedSource.time - interpolatedTime) > allowableErrorSeconds * abs(rate)) {
                    // If we've exceeded the allowable error, we should use the time of the source.
                    isInterpolating = false
                    interpolatedTime = framedSource.time
                }
            } else {
                interpolatedTime = framedSource.time

                // Only start interpolating from the next frame, as the first frame after a clock starts may give
                // very incorrect results, i.e. due to a large elapsed time.
                if (sourceHasElapsed) {
                    isInterpolating = true
                }
            }

            // Seeking backwards should only be allowed if the source is explicitly doing that.
            val elapsedInOpposingDirection = framedSource.elapsedFrameTime != 0f && sign(framedSource.elapsedFrameTime) != sign(rate)

            if (!elapsedInOpposingDirection) {
                interpolatedTime = if (rate >= 0) max(lastTime, interpolatedTime) else min(lastTime, interpolatedTime)
            }
        } finally {
            isRunning = sourceIsRunning
            lastFrameTime = time
            time = interpolatedTime
            elapsedFrameTime = time - lastTime
        }
    }
}
