package com.reco1l.verktex.time

import kotlin.time.Clock.System

/**
 * Represents a clock that uses the system clock.
 */
object SystemClock : Clock() {

    override val isRunning = true

    override val time: Float
        get() = System.now().toEpochMilliseconds() / 1000f

}