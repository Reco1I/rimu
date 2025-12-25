package com.reco1l.verktex

/**
 * Timings of the engine workers for the last frames, shown by [com.reco1l.verktex.ui.UIPerformanceOverlay].
 *
 * Frame values are written and read on the draw worker, the update time comes from the update worker.
 */
object PerformanceStats {

    /**
     * How many frame times are kept.
     */
    const val HISTORY_SIZE = 120

    /**
     * Milliseconds spent drawing the last frame, from the first component to the last GL call,
     * without waiting for the display.
     */
    var drawTime = 0f
        private set

    /**
     * Milliseconds the last update frame took.
     */
    @Volatile
    var updateTime = 0f

    /**
     * Draw commands the scene produced in the last frame, overlays excluded.
     */
    var drawCommands = 0

    /**
     * Average time between frames in milliseconds over the kept frame times.
     */
    val frameTime: Float
        get() {
            var total = 0f
            var count = 0
            for (time in frameTimes) {
                if (time > 0f) {
                    total += time
                    count++
                }
            }
            return if (count > 0) total / count else 0f
        }

    /**
     * The longest kept frame time in milliseconds.
     */
    val maxFrameTime: Float
        get() = frameTimes.max()

    /**
     * Average frames per second over the kept frame times.
     */
    val fps: Float
        get() {
            val frameTime = frameTime
            return if (frameTime > 0f) 1000f / frameTime else 0f
        }


    private val frameTimes = FloatArray(HISTORY_SIZE)
    private var nextIndex = 0
    private var lastFrameNanos = 0L
    private var frameStartNanos = 0L


    /**
     * Returns the time in milliseconds between the starts of two frames, `0` is the oldest kept one.
     */
    fun frameTimeAt(index: Int) = frameTimes[(nextIndex + index) % HISTORY_SIZE]

    fun onFrameStart() {
        val now = System.nanoTime()

        if (lastFrameNanos != 0L) {
            frameTimes[nextIndex] = (now - lastFrameNanos) / 1_000_000f
            nextIndex = (nextIndex + 1) % HISTORY_SIZE
        }

        lastFrameNanos = now
        frameStartNanos = now
    }

    fun onFrameDrawn() {
        drawTime = (System.nanoTime() - frameStartNanos) / 1_000_000f
    }
}
