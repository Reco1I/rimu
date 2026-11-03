package com.reco1l.verktex.time

/**
 * A clock that keeps running on its own while its source can't, such as an audio track being before its start.
 */
class DecouplingFrameClock(
    source: Clock? = null,
    private val realtimeReferenceClock: Clock = StopwatchClock(true)
) : FrameClock(), IFrameProcessableClock, ISourceChangeableClock, IAdjustableClock {

    /**
     * Whether this clock can run while its source isn't.
     */
    var allowDecoupling = true

    override var isRunning = false
        private set

    override var time = 0f
        private set

    override var elapsedFrameTime = 0f
        private set

    override var lastFrameTime = 0f
        private set

    private var shouldBeRunning = false
    private var decoupledTime = 0f
    private var lastReferenceTime: Float? = null
    private var lastSeekFailed = false
    private var pendingSourceRestartAfterNegativeSeek = false

    override lateinit var source: Clock
        private set

    private lateinit var adjustableSource: IAdjustableClock


    init {
        changeSource(source)
    }


    override fun processFrame() {
        val lastTime = time

        (source as? IFrameProcessableClock)?.processFrame()
        (realtimeReferenceClock as? IFrameProcessableClock)?.processFrame()

        val referenceTime = realtimeReferenceClock.time

        try {
            // If the source is running, there is never a need for any decoupling logic.
            if (source.isRunning) {
                decoupledTime = source.time
                shouldBeRunning = true
                return
            }

            // If we are not allowed to decouple, we should also just pass-through the source time.
            if (!allowDecoupling) {
                decoupledTime = source.time
                shouldBeRunning = false
                return
            }

            // We then want to check whether our internal running state permits time to elapse.
            if (!shouldBeRunning) {
                return
            }

            // We can only begin tracking time from the second frame, as we need an elapsed real time reference.
            val lastReferenceTime = lastReferenceTime ?: return

            decoupledTime += (referenceTime - lastReferenceTime) * rate

            // When crossing into positive time, we should attempt to start and use the source clock.
            // Note that this carries the common assumption that the source clock *should* be able to run from 0.
            if (pendingSourceRestartAfterNegativeSeek && decoupledTime >= 0) {
                pendingSourceRestartAfterNegativeSeek = false

                // We still need to check the seek was successful, else we might have already exceeded the
                // length of the source.
                lastSeekFailed = !adjustableSource.seek(decoupledTime)

                if (!lastSeekFailed) {
                    adjustableSource.start()
                }

                // Don't use the time of the source until next frame, as our decoupled time is likely more
                // accurate (starting a clock may have slight discrepancies).
            }
        } finally {
            isRunning = shouldBeRunning
            this.lastReferenceTime = referenceTime
            lastFrameTime = time
            time = decoupledTime
            elapsedFrameTime = time - lastTime
        }
    }


    override fun changeSource(source: Clock?) {
        val newSource = source ?: StopwatchClock(true)

        require(newSource is IAdjustableClock) { "The source clock must be adjustable." }

        this.source = newSource
        adjustableSource = newSource

        decoupledTime = newSource.time
        shouldBeRunning = newSource.isRunning
        lastSeekFailed = false
        pendingSourceRestartAfterNegativeSeek = false
        lastReferenceTime = null
    }

    override fun reset() {
        adjustableSource.reset()

        pendingSourceRestartAfterNegativeSeek = false
        shouldBeRunning = false
        lastSeekFailed = false
        decoupledTime = 0f
        lastReferenceTime = null
    }

    override fun start() {
        if (shouldBeRunning) {
            return
        }

        // If the previous seek failed, avoid starting the source clock. Doing so would make it start from an
        // incorrect location (i.e. 0 in the case where we are tracking negative time).
        if (lastSeekFailed && allowDecoupling) {
            shouldBeRunning = true

            // So the first frame does not include any real time between the last seek and this call.
            lastReferenceTime = null
            return
        }

        adjustableSource.start()
        shouldBeRunning = source.isRunning || allowDecoupling
    }

    override fun pause() {
        adjustableSource.pause()
        shouldBeRunning = false
    }

    override fun seek(to: Float): Boolean {
        lastSeekFailed = !adjustableSource.seek(to)

        if (!lastSeekFailed) {
            // Attempt to transfer the decoupled running state to the source in case we succeeded.
            if (shouldBeRunning && !source.isRunning) {
                adjustableSource.start()
            }
        } else {
            if (!allowDecoupling) {
                return false
            }

            // Ensure the source is stopped as we enter decoupled mode.
            adjustableSource.pause()
            pendingSourceRestartAfterNegativeSeek = to < 0
        }

        decoupledTime = to
        return true
    }

    override fun resetSpeedAdjustments() {
        adjustableSource.resetSpeedAdjustments()
    }

    override var rate
        get() = adjustableSource.rate
        set(value) {
            adjustableSource.rate = value
        }
}
