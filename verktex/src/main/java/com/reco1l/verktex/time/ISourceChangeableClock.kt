package com.reco1l.verktex.time

/**
 * A clock that follows another clock, which can be replaced.
 */
interface ISourceChangeableClock {

    /**
     * The clock this clock follows.
     */
    val source: Clock

    /**
     * Changes the clock this clock follows.
     *
     * @param source The new source, or null to use an internal one.
     */
    fun changeSource(source: Clock?)
}
