package com.reco1l.verktex.time

import kotlin.jvm.JvmName
import com.reco1l.verktex.Logger
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A clock that follows a [SongClock] limiting how much its time can change per frame, so that big jumps such as
 * the ones after a stutter are caught up with gradually instead of skipping time.
 */
class FrameStableClock(private val referenceClock: SongClock) :
    FrameClock(), IFrameProcessableClock, ISourceChangeableClock, IAdjustableClock {

    private val manualClock = ManualClock()
    private val framedClock = FramedClock(manualClock, false)

    private val wallClock = Stopwatch().also { it.startMeasuring() }
    private var wallElapsedSeconds = 0f

    private var firstConsumption = true
    private var invalidTimeLogCount = 0

    /**
     * Whether to limit how much the time can change per frame.
     */
    @get:JvmName("isFrameStablePlayback")
    var frameStablePlayback = true

    /**
     * The time gameplay starts at. Before it, time advances freely up to it.
     */
    var gameplayStartTime = Float.NEGATIVE_INFINITY

    /**
     * Whether this clock is behind or ahead of the reference clock.
     */
    @get:JvmName("requiresCatchUp")
    var requiresCatchUp = false
        private set

    override val time
        get() = if (firstConsumption) referenceClock.time else framedClock.time

    override val lastFrameTime
        get() = framedClock.lastFrameTime

    override val elapsedFrameTime
        get() = framedClock.elapsedFrameTime

    override val isRunning
        get() = referenceClock.isRunning

    override val source
        get() = referenceClock.source

    override var rate
        get() = referenceClock.rate
        set(value) {
            referenceClock.rate = value
        }

    var userGlobalOffset
        get() = referenceClock.userGlobalOffset
        set(value) {
            referenceClock.userGlobalOffset = value
        }

    var userBeatmapOffset
        get() = referenceClock.userBeatmapOffset
        set(value) {
            referenceClock.userBeatmapOffset = value
        }

    val totalAppliedOffset
        get() = referenceClock.totalAppliedOffset


    override fun seek(to: Float): Boolean {
        resetState()
        return referenceClock.seek(to)
    }

    override fun reset() {
        resetState()
        referenceClock.reset()
    }

    override fun start() = referenceClock.start()

    override fun pause() = referenceClock.pause()

    override fun resetSpeedAdjustments() = referenceClock.resetSpeedAdjustments()

    override fun changeSource(source: Clock?) = referenceClock.changeSource(source)

    override fun processFrame() {
        wallElapsedSeconds = wallClock.elapsedSeconds
        wallClock.restart()

        referenceClock.processFrame()
        updateClock()
    }

    private fun resetState() {
        firstConsumption = true
        invalidTimeLogCount = 0
        wallClock.restart()
        wallElapsedSeconds = 0f
    }

    private fun syncManualClock(time: Float) {
        manualClock.time = time
        manualClock.rate = referenceClock.rate
        manualClock.isRunning = referenceClock.isRunning
    }

    private fun updateClock() {
        // Handled before any other check so a seek done while paused takes effect immediately.
        if (firstConsumption) {
            val referenceTime = referenceClock.time

            manualClock.time = referenceTime

            // The first call sets the time from a zero baseline, which would be a large initial elapsed time.
            framedClock.processFrame()
            firstConsumption = false

            syncManualClock(referenceTime)

            // The second call makes the elapsed time 0.
            framedClock.processFrame()
            return
        }

        if (!referenceClock.isRunning) {
            requiresCatchUp = false

            // Freeze time but still process the framed clock so the elapsed time is 0.
            manualClock.time = framedClock.time
            manualClock.isRunning = false
            framedClock.processFrame()
            return
        }

        var proposedTime = referenceClock.time

        if (frameStablePlayback) {
            proposedTime = applyFrameStability(proposedTime)
        }

        // The audio backend can report wrong positions (https://www.un4seen.com/forum/?topic=20482), so a jump
        // larger than 500 ms is considered a glitch. Double-checking the real elapsed time ensures we do not
        // freeze time during a legitimate stutter.
        val delta = abs(proposedTime - referenceClock.time)

        if (frameStablePlayback && delta > 0.5f && wallElapsedSeconds <= 0.5f) {
            if (invalidTimeLogCount < 10) {
                invalidTimeLogCount++
                Logger.w(TAG, "Ignoring likely invalid time value provided by the track: ${referenceClock.time}, expected: $proposedTime")
            }

            requiresCatchUp = false

            // Freeze time for this frame.
            syncManualClock(framedClock.time)
            framedClock.processFrame()
            return
        }

        invalidTimeLogCount = 0
        requiresCatchUp = delta > 0f

        syncManualClock(proposedTime)
        framedClock.processFrame()
    }

    private fun applyFrameStability(proposedTime: Float): Float {
        // Scaled by the current rate so a normal frame at any speed never triggers the clamp.
        val frameTime = abs(referenceClock.rate) / 60f

        if (manualClock.time < gameplayStartTime) {
            // Before the first approach circle appears, advance freely up to the start of gameplay.
            val clamped = min(gameplayStartTime, proposedTime)
            manualClock.time = clamped
            return clamped
        }

        if (abs(manualClock.time - proposedTime) > frameTime * 1.2f) {
            return if (proposedTime > manualClock.time) {
                min(proposedTime, manualClock.time + frameTime)
            } else {
                max(proposedTime, manualClock.time - frameTime)
            }
        }

        return proposedTime
    }


    private companion object {
        const val TAG = "FrameStableClock"
    }
}
