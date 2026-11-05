package com.reco1l.verktex.time

/**
 * A clock whose position and rate can be changed.
 */
interface IAdjustableClock : IControllableClock {

    /**
     * The rate at which the time of this clock advances.
     */
    var rate: Float

    /**
     * Resets the rate of this clock to its default.
     */
    fun resetSpeedAdjustments()
}

/**
 * A base for adjustable clocks, so they can be used both as a [Clock] and as an [IAdjustableClock].
 */
abstract class AdjustableClock : Clock(), IAdjustableClock {
    abstract override var rate: Float
}
