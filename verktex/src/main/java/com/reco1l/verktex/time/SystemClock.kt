package com.reco1l.verktex.time

object SystemClock : Clock() {

    // Relative to the process start, an absolute epoch time in seconds doesn't fit into a Float
    // with enough precision (its resolution would be around two minutes).
    private val startNanos = System.nanoTime()

    override val isRunning = true

    override val time: Float
        get() = (System.nanoTime() - startNanos) / 1_000_000_000f

}
