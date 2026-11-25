package com.osudroid.game.session

import com.reco1l.verktex.Logger
import com.reco1l.verktex.PerformanceStats
import kotlin.time.TimeSource

/**
 * Times the sections of a step of gameplay, and logs them when it takes longer than
 * [PerformanceStats.slowFrameThreshold] (debug builds), to find what stalls it. It doesn't allocate otherwise.
 */
internal class UpdateProfile(private val tag: String, private val label: String) {
    private val names = arrayOfNulls<String>(MAX_SECTIONS)
    private val nanos = LongArray(MAX_SECTIONS)
    private var count = 0
    private var startNanos = 0L
    private var lastNanos = 0L

    private val origin = TimeSource.Monotonic.markNow()

    private fun monotonicNanos() = origin.elapsedNow().inWholeNanoseconds

    fun start() {
        count = 0
        startNanos = monotonicNanos()
        lastNanos = startNanos
    }

    fun mark(name: String) {
        val now = monotonicNanos()
        if (count < MAX_SECTIONS) {
            names[count] = name
            nanos[count] = now - lastNanos
            count++
        }
        lastNanos = now
    }

    /**
     * Marks the last section, [name], and logs the step if it was slow.
     */
    fun end(name: String) {
        mark(name)

        val threshold = PerformanceStats.slowFrameThreshold
        val total = (lastNanos - startNanos) / 1_000_000f
        if (threshold <= 0f || total < threshold / 2) {
            return
        }

        val sections = StringBuilder()
        for (i in 0 until count) {
            val ms = nanos[i] / 1_000_000f
            if (ms >= 1f) {
                sections.append(names[i]).append(' ').append(ms).append("ms, ")
            }
        }
        Logger.w(tag, "Slow $label: ${total}ms ($sections)")
    }

    private companion object {
        const val MAX_SECTIONS = 16
    }
}
