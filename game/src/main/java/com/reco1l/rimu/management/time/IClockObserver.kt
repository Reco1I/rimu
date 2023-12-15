package com.reco1l.rimu.management.time

/**
 * Observable for clock update calls.
 */
fun interface IClockObserver
{
    /**
     * Called by the clock with the elapsed time and the delta time both multiplied with the rate.
     */
    fun onClockUpdate(msElapsedTime: Long, msDeltaTime: Long)
}