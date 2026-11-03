package com.reco1l.verktex.time

/**
 * A clock that takes the time of its source once per frame, so it stays the same during the frame.
 *
 * @param source The clock to follow, or null to use one following real time.
 * @param processSource Whether processing a frame of this clock also processes a frame of its source.
 */
open class FramedClock(
    source: Clock? = null,
    private val processSource: Boolean = true
) : FrameClock(), IFrameProcessableClock, ISourceChangeableClock {

    final override lateinit var source: Clock
        private set

    override var time = 0f
        protected set

    override var lastFrameTime = 0f
        protected set

    override val rate
        get() = source.rate

    override val elapsedFrameTime
        get() = time - lastFrameTime

    override val isRunning
        get() = source.isRunning


    init {
        changeSource(source)
    }


    override fun changeSource(source: Clock?) {
        this.source = source ?: StopwatchClock(true)

        time = this.source.time
        lastFrameTime = time
    }

    override fun processFrame() {
        if (processSource) {
            (source as? IFrameProcessableClock)?.processFrame()
        }

        lastFrameTime = time
        time = source.time
    }
}
