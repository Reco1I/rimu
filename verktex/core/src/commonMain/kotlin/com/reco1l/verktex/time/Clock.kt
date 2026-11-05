package com.reco1l.verktex.time

/**
 * Represents a clock.
 */
abstract class Clock {

    /**
     * The current time in seconds.
     */
    abstract val time: Float

    /**
     * Whether it's running or not.
     */
    abstract val isRunning: Boolean

    /**
     * The rate at which the time of this clock advances.
     */
    open val rate: Float
        get() = 1f


}

