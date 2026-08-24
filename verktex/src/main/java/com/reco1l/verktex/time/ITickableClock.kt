package com.reco1l.verktex.time

/**
 * Represents a clock that proccess its time with ticks and is tickable (we can call tick manually)
 */
interface ITickableClock {

    /**
     * Ticks the clock and updates its time.
     */
    fun tick()

}