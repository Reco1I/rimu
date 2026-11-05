package com.reco1l.verktex.time

class TestStopwatchClockWithRangeLimit(source: () -> Long = System::nanoTime) : StopwatchClock(true, source) {
    val minTime = 0f
    var maxTime = Float.POSITIVE_INFINITY

    override val time: Float
        get() {
            val time = super.time
            val clamped = time.coerceIn(minTime, maxTime)

            if (clamped == time) {
                return clamped
            }

            if ((rate > 0 && clamped == maxTime) || (rate < 0 && clamped == minTime)) {
                pause()
            }

            return clamped
        }

    override fun seek(to: Float): Boolean {
        val clamped = to.coerceIn(minTime, maxTime)

        if (clamped != to) {
            // Emulate what a BASS track would do in this situation.
            if (to >= maxTime) {
                pause()
            }

            super.seek(clamped)
            return false
        }

        return super.seek(to)
    }
}