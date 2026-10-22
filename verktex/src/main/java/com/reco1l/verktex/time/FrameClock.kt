package com.reco1l.verktex.time

/**
 * Represents a clock that processes its time in frames.
 */
abstract class FrameClock : Clock() {

    /**
     * The elapsed time in seconds since the last frame.
     */
    abstract val elapsedFrameTime: Float

    /**
     * The time of the last frame in seconds.
     */
    abstract val lastFrameTime: Float

}