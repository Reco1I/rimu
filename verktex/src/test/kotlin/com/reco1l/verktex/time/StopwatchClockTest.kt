package com.reco1l.verktex.time

import org.junit.Assert.*
import org.junit.Test

class StopwatchClockTest {
    private var mockTimeNanos = 0L

    private fun createClock(start: Boolean = false) = StopwatchClock(start) { mockTimeNanos }

    private fun advanceTime(seconds: Float) {
        mockTimeNanos += (seconds * 1e9f).toLong()
    }

    @Test
    fun `Test reset time`() {
        val clock = createClock(true)

        advanceTime(1f)
        assertTrue(clock.time > 0)

        clock.pause()
        clock.reset()

        assertEquals(0f, clock.time, 0f)
    }

    @Test
    fun `Test rate up reset time`() {
        val clock = createClock(true)

        advanceTime(1f)

        clock.pause()
        val stoppedTime = clock.time
        assertTrue(stoppedTime > 0)

        clock.rate = 2f
        assertEquals(stoppedTime, clock.time, 0f)

        clock.reset()

        assertEquals(0f, clock.time, 0f)
    }

    @Test
    fun `Test seek while stopped`() {
        val clock = createClock()
        clock.seek(5f)
        assertEquals(5f, clock.time, 0f)
    }

    @Test
    fun `Test seek when non-zero`() {
        val clock = createClock(true)

        advanceTime(1f)

        clock.pause()
        val stoppedTime = clock.time
        assertTrue(stoppedTime > 0)

        clock.seek(stoppedTime)

        assertEquals(stoppedTime, clock.time, 0f)
    }

    @Test
    fun `Test seek negative adjust rate`() {
        val clock = createClock()
        clock.seek(-5f)
        assertEquals(-5f, clock.time, 0f)

        clock.rate = 2f
        clock.start()

        advanceTime(1f)

        clock.pause()
        val stoppedTime = clock.time
        assertTrue(stoppedTime < 0)

        clock.seek(stoppedTime)

        assertEquals(stoppedTime, clock.time, 0f)
    }

    @Test
    fun `Test negative rate`() {
        val clock = createClock()
        clock.rate = -2f
        clock.start()

        advanceTime(1f)

        clock.pause()

        assertTrue(clock.time < 0)
    }
}