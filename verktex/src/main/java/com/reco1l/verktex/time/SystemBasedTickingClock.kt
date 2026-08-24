package com.reco1l.verktex.time

class SystemBasedTickingClock : TickingClock(), ITickableClock, IControllableClock {

    //region Clock implementation

    override var time = 0f
        private set

    override var isRunning = true
        private set

    override var elapsedTickTime = 0f
        private set

    override var lastTickTime = 0f
        private set

    //endregion

    private var lastRealFrameTime = SystemClock.time


    override fun tick() {
        if (!isRunning) return

        val realTime = SystemClock.time

        elapsedTickTime = realTime - lastRealFrameTime
        lastRealFrameTime = realTime

        lastTickTime = time
        time += elapsedTickTime
    }


    //region Controllable implementation

    override fun seek(to: Float) {
        time = to
    }

    override fun reset() {
        time = 0f
    }

    override fun start() {
        isRunning = true

        // Prevent jumps in time when resuming due to real time jumps
        lastRealFrameTime = SystemClock.time
    }

    override fun pause() {
        isRunning = false
    }

    //endregion
}