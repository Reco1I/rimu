package com.reco1l.verktex.time

/**
 * Represents a clock that can be controlled.
 */
interface IControllableClock {

    /**
     * Seeks to the given time in seconds.
     */
    fun seek(to: Float)

    /**
     * Starts the clock from the current time.
     */
    fun start()

    /**
     * Pauses the clock.
     */
    fun pause()

    /**
     * Resets the clock to the beginning.
     */
    fun reset()

}