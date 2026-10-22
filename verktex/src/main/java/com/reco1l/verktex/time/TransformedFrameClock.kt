package com.reco1l.verktex.time

/**
 * Represents a clock that is offset.
 */
class TransformedFrameClock<SourceClock : FrameClock>(override val source: SourceClock) : FrameClock(), ISourceBasedClock<SourceClock> {

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

    override val elapsedFrameTime: Float
        get() = source.elapsedFrameTime * rate

    override val lastFrameTime: Float
        get() = source.lastFrameTime * rate + offset

    //endregion

}

/**
 * Returns a new [TransformedFrameClock] that is derived from this clock.
 */
fun <C : FrameClock> C.transform(offset: Float = 0f, rate: Float = 1f) = TransformedFrameClock(this).also { transformedClock ->
    transformedClock.offset = offset
    transformedClock.rate = rate
}