package com.reco1l.verktex.time

/**
 * A clock whose state is set by hand.
 */
open class ManualClock : Clock() {
    override var time = 0f
    override var rate = 1f
    override var isRunning = false
}
