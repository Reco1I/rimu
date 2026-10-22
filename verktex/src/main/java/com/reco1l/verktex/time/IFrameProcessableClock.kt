package com.reco1l.verktex.time

/**
 * Represents a clock that processes its time in frames and can be advanced manually by calling [processFrame].
 */
interface IFrameProcessableClock {

    /**
     * Processes a frame and updates the clock's time.
     */
    fun processFrame()

}