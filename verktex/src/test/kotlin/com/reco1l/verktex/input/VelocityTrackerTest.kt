package com.reco1l.verktex.input

import org.junit.Assert.assertEquals
import org.junit.Test

class VelocityTrackerTest {

    @Test
    fun `constant movement gives its velocity`() {
        val tracker = VelocityTracker()
        // 2 px/ms to the right and 1 px/ms up, sampled every 8 ms.
        for (i in 0..10) {
            tracker.addSample(i * 8L, i * 16f, -i * 8f)
        }

        assertEquals(2000f, tracker.velocityX, 1f)
        assertEquals(-1000f, tracker.velocityY, 1f)
    }

    @Test
    fun `only recent samples count`() {
        val tracker = VelocityTracker(horizon = 80)
        // Slow at first, then fast for longer than the horizon.
        for (i in 0..20) tracker.addSample(i * 10L, i * 1f, 0f)
        for (i in 1..10) tracker.addSample(200 + i * 10L, 20f + i * 50f, 0f)

        assertEquals(5000f, tracker.velocityX, 1f)
    }

    @Test
    fun `releasing after holding still doesn't fling`() {
        val tracker = VelocityTracker(stillnessTimeout = 40)
        for (i in 0..5) tracker.addSample(i * 10L, i * 20f, 0f)

        tracker.release(50 + 100)

        assertEquals(0f, tracker.velocityX, 0f)
    }

    @Test
    fun `releasing right away keeps the velocity`() {
        val tracker = VelocityTracker(stillnessTimeout = 40)
        for (i in 0..5) tracker.addSample(i * 10L, i * 20f, 0f)

        tracker.release(55)

        assertEquals(2000f, tracker.velocityX, 1f)
    }

    @Test
    fun `samples at the same time keep the last velocity`() {
        val tracker = VelocityTracker()
        tracker.addSample(0, 0f, 0f)
        tracker.addSample(10, 10f, 0f)
        tracker.addSample(10, 12f, 0f)

        assertEquals(true, tracker.velocityX.isFinite())
    }

    @Test
    fun `clearing resets the velocity`() {
        val tracker = VelocityTracker()
        tracker.addSample(0, 0f, 0f)
        tracker.addSample(10, 10f, 0f)
        tracker.clear()

        assertEquals(0f, tracker.velocityX, 0f)
    }
}
