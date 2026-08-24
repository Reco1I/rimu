package com.reco1l.verktex.time

/**
 * Represents a clock that its elapsed time is derived from another clock.
 *
 * This derived clock is tickable, and it requires [tick] to be called manually in order to update
 * its state.
 */
class DerivedTickableClock<SourceClock : TickingClock>(override val source: SourceClock) :
    TickingClock(),
    IControllableClock,
    ITickableClock,
    ISourceBasedClock<SourceClock>
{

    override val elapsedTickTime: Float
        get() = source.elapsedTickTime


    override var isRunning: Boolean = true
        get() = source.isRunning && field
        private set

    override var lastTickTime = 0f
        private set

    override var time = 0f
        private set


    override fun tick() {
        if (!isRunning) return

        lastTickTime = time
        time += elapsedTickTime
    }


    override fun seek(to: Float) {
        time = to
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
}


/**
 * Returns a new [DerivedTickableClock] that is derived from this clock.
 */
fun <C : TickingClock> C.derive() = DerivedTickableClock(this)