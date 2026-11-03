package com.reco1l.verktex.time

/**
 * Represents a clock that its elapsed time is derived from another clock, scaled by its [rate].
 *
 * This derived clock requires [processFrame] to be called manually in order to update its state.
 */
class DerivedFrameClock<SourceClock : FrameClock>(override val source: SourceClock) :
    FrameClock(),
    IAdjustableClock,
    IFrameProcessableClock,
    ISourceBasedClock<SourceClock>
{

    override var rate = 1f

    override val elapsedFrameTime: Float
        get() = if (isRunning) source.elapsedFrameTime * rate else 0f


    override var isRunning: Boolean = true
        get() = source.isRunning && field
        private set

    override var lastFrameTime = 0f
        private set

    override var time = 0f
        private set


    override fun processFrame() {
        if (!isRunning) return

        lastFrameTime = time
        time += elapsedFrameTime
    }


    override fun seek(to: Float): Boolean {
        time = to
        return true
    }

    override fun start() {
        isRunning = true
    }

    override fun pause() {
        isRunning = false
    }

    override fun reset() {
        time = 0f
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }
}


/**
 * Returns a new [DerivedFrameClock] that is derived from this clock.
 */
fun <C : FrameClock> C.derive() = DerivedFrameClock(this)