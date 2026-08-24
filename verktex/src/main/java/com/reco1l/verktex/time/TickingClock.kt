package com.reco1l.verktex.time

/**
 * Represents a clock that proccess its time with ticks (frames in other words).
 */
abstract class TickingClock : Clock() {

    /**
     * The elapsed time in seconds since the last tick.
     */
    abstract val elapsedTickTime: Float

    /**
     * The time of the last tick in seconds.
     */
    abstract val lastTickTime: Float

}