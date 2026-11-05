package com.reco1l.verktex.time

/**
 * Represents a clock that implements a source clock.
 *
 * @param SourceClock The type of the source clock.
 */
interface ISourceBasedClock<SourceClock : Clock> {

    /**
     * The source clock of this clock.
     */
    val source: SourceClock

}