package com.reco1l.verktex.time

/**
 * Represents a clock that is offset.
 */
class TransformedTickingClock<SourceClock : TickingClock>(override val source: SourceClock) : TickingClock(), ISourceBasedClock<SourceClock> {

    /**
     * The offset in seconds.
     */
    var offset = 0f

    /**
     * The speed rate of the clock.
     */
    var rate = 1f


    //region Transformed properties

    override val time: Float
        get() = source.time * rate + offset

    override val isRunning: Boolean
        get() = source.isRunning

    override val elapsedTickTime: Float
        get() = source.elapsedTickTime * rate

    override val lastTickTime: Float
        get() = source.lastTickTime * rate + offset

    //endregion

}

/**
 * Returns a new [TransformedTickingClock] that is derived from this clock.
 */
fun <C : TickingClock> C.transform(offset: Float = 0f, rate: Float = 1f) = TransformedTickingClock(this).also { transformedClock ->
    transformedClock.offset = offset
    transformedClock.rate = rate
}