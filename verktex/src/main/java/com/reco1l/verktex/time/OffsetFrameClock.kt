package com.reco1l.verktex.time

/**
 * A [FramedClock] whose time is shifted by an [offset].
 */
open class OffsetFrameClock(source: Clock?, processSource: Boolean = true) : FramedClock(source, processSource) {

    /**
     * The amount of time in seconds to add to the time of the source.
     */
    open var offset = 0f
        set(value) {
            lastFrameTime += value - field
            field = value
        }

    override var time
        get() = super.time + offset
        set(value) {
            super.time = value
        }
}
