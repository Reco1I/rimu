package com.reco1l.verktex.time

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

sealed class BaseInterpolatingFramedClockTest {
    protected lateinit var source: TestClock
    protected lateinit var realTimeClock: TestClock
    protected lateinit var interpolating: InterpolatingFrameClock

    @Before
    open fun setUp() {
        source = TestClock()
        realTimeClock = TestClock().apply { start() }

        interpolating = InterpolatingFrameClock(realTimeClockSource = realTimeClock)
        interpolating.changeSource(source)
    }
}

class MainInterpolatingFramedClockTest : BaseInterpolatingFramedClockTest() {
    @Test
    fun `Test never interpolates backwards`() {
        assertEquals("Interpolating should match source time.", interpolating.time, source.time,  0f)
        source.start()
        assertEquals("Interpolating should match source time.", interpolating.time, source.time, 0f)
        interpolating.processFrame()

        // Test with test clock not elapsing
        var lastValue = interpolating.time

        (0..100).forEach { _ ->
            interpolating.processFrame()

            assertTrue("Interpolating should not jump against rate.", interpolating.time >= lastValue)
            assertTrue("Interpolating should not jump before source time.", interpolating.time >= source.time)

            realTimeClock.time += interpolating.allowableErrorSeconds / 2
            lastValue = interpolating.time
        }

        var interpolatedCount = 0

        // Test with test clock elapsing
        lastValue = interpolating.time

        (0..100).forEach { _ ->
            // We want to interpolate but not fall behind and fail interpolation too much.
            val elapsed = interpolating.allowableErrorSeconds / 2 + 0.005f
            source.time += elapsed
            realTimeClock.time += elapsed

            interpolating.processFrame()

            assertTrue("Interpolating should not jump against rate.", interpolating.time >= lastValue)
            assertTrue("Interpolating should be within allowance.", abs(interpolating.time - source.time) <= interpolating.allowableErrorSeconds)

            if (interpolating.isInterpolating) {
                ++interpolatedCount
            }

            lastValue = interpolating.time
        }

        assertTrue(interpolatedCount > 10)
    }

    @Test
    fun `Test source change transfers value adjustable`() {
        // For interpolating clocks, value transfer is always in the direction of the interpolating clock.
        val firstSourceTime = 256f
        val secondSourceTime = 128f

        source.seek(firstSourceTime)

        val secondSource = TestClock().apply {
            // More importantly, test a value lower than the original source. This is to both test value transfer *and*
            // the case where time is going backwards, as some clocks have special provisions for this.
            time = secondSourceTime
        }

        interpolating.processFrame()
        assertEquals(firstSourceTime, interpolating.time, 0f)

        interpolating.changeSource(secondSource)
        interpolating.processFrame()

        assertEquals(secondSourceTime, secondSource.time, 0f)
        assertEquals(secondSourceTime, interpolating.time, 0f)
    }

    @Test
    fun `Test source change transfers value non-adjustable`() {
        // For interpolating clocks, value transfer is always in the direction of the interpolating clock.
        val firstSourceTime = 256f
        val secondSourceTime = 128f

        source.seek(firstSourceTime)

        val secondSource = TestNonAdjustableClock().apply {
            // More importantly, test a value lower than the original source. This is to both test value transfer *and*
            // the case where time is going backwards, as some clocks have special provisions for this.
            time = secondSourceTime
        }

        interpolating.processFrame()
        assertEquals(firstSourceTime, interpolating.time, 0f)

        interpolating.changeSource(secondSource)
        interpolating.processFrame()

        assertEquals(secondSourceTime, secondSource.time, 0f)
        assertEquals(secondSourceTime, interpolating.time, 0f)
    }

    @Test
    fun `Test never interpolates backwards on interpolation fail`() {
        val simulatedSleepTime = 0.02f

        var lastValue = interpolating.time
        source.start()
        var interpolatedCount = 0

        for (i in 0 until 200) {
            source.rate += i * 10

            if (i < 100) {
                // Stop the elapsing at some point in time. should still work as source's elapsedTime is zero.
                source.time += simulatedSleepTime * source.rate
            }

            realTimeClock.time += simulatedSleepTime
            interpolating.processFrame()

            if (interpolating.isInterpolating) {
                ++interpolatedCount
            }

            assertTrue("Interpolating should not jump against rate.", interpolating.time >= lastValue)
            assertTrue("Interpolating should be within allowance.", abs(interpolating.time - source.time) <= interpolating.allowableErrorSeconds * source.rate)

            lastValue = interpolating.time
        }

        assertTrue(interpolatedCount > 10)
    }

    @Test
    fun `Test can seek forwards on interpolation fail`() {
        val simulatedSleepTime = 0.02f

        var lastValue = interpolating.time
        source.start()
        var interpolatedCount = 0

        for (i in 0 until 200) {
            source.rate += i * 10

            // Seek forward once at a random point.
            if (i == 100) {
                source.time += interpolating.allowableErrorSeconds * 10 * source.rate
                realTimeClock.time += simulatedSleepTime
                interpolating.processFrame()
                assertFalse(interpolating.isInterpolating)
                assertEquals(source.time, interpolating.time, 0f)
            } else {
                source.time += simulatedSleepTime * source.rate
                realTimeClock.time += simulatedSleepTime
                interpolating.processFrame()
            }

            if (interpolating.isInterpolating) {
                ++interpolatedCount
            }

            assertTrue("Interpolating should not jump against rate.", interpolating.time >= lastValue)
            assertTrue("Interpolating should be within allowance.", abs(interpolating.time - source.time) <= interpolating.allowableErrorSeconds * source.rate)

            lastValue = interpolating.time
        }

        assertTrue(interpolatedCount > 10)
    }

    @Test
    fun `Test can seek backwards`() {
        assertEquals("Interpolating should match source time.", interpolating.time, source.time, 0f)
        source.start()

        assertEquals("Interpolating should match source time.", interpolating.time, source.time, 0f)
        interpolating.processFrame()

        source.seek(10f)
        interpolating.processFrame()
        assertEquals("Interpolating should match source time.", interpolating.time, source.time, 0f)

        source.seek(0f)
        interpolating.processFrame()
        assertEquals("Interpolating should match source time.", interpolating.time, source.time, 0f)
    }

    @Test
    fun `Test interpolation after source stopped then seeked`() {
        // Just to make sure this works even when still in interpolation allowance.
        interpolating.allowableErrorSeconds = 100f

        source.start()

        while (!interpolating.isInterpolating) {
            val elapsed = 0.01f
            source.time += elapsed
            realTimeClock.time += elapsed
            interpolating.processFrame()
        }

        source.pause()
        source.seek(-10f)

        interpolating.processFrame()
        assertFalse(interpolating.isInterpolating)
        assertEquals(-10f, interpolating.time, 0.1f)
        assertEquals(-10f, interpolating.elapsedFrameTime, 0.1f)

        source.start()
        interpolating.processFrame()
        assertEquals(-10f, interpolating.time, 0.1f)
        assertEquals(0f, interpolating.elapsedFrameTime, 0.1f)
    }

    @Test
    fun `Test interpolation stays within bounds`() {
        source.start()

        val simulatedSleepTime = 0.02f

        (0..100).forEach { _ ->
            source.time += simulatedSleepTime
            realTimeClock.time += simulatedSleepTime
            interpolating.processFrame()

            // Should be a no-op.
            interpolating.changeSource(source)

            assertTrue("Interpolating should be within allowable error bounds.", (kotlin.math.abs((interpolating.time) - (source.time)) <= (interpolating.allowableErrorSeconds + 0f).let { if (it == 0f) 1e-3f else it }))
        }

        source.pause()
        interpolating.processFrame()

        assertFalse(interpolating.isRunning)
        assertEquals(interpolating.time, source.time, interpolating.allowableErrorSeconds)
    }
}

@RunWith(Parameterized::class)
class InterpolatingFramedClockTestNoDrift(private val simulatedUpdateRate: Float) : BaseInterpolatingFramedClockTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "simulatedUpdateRate={0}")
        fun data() = arrayOf(0.0001f, 0.001f, 0.01f, 0.05f)
    }

    @Test
    fun `Test no interpolation drift`() {
        val stopwatch = StopwatchClock(source = realTimeClock::currentTimeLong)

        interpolating.changeSource(stopwatch)

        source.start()
        stopwatch.start()

        while (interpolating.time <= 1f) {
            realTimeClock.time += simulatedUpdateRate
            interpolating.processFrame()
            assertEquals(stopwatch.time, interpolating.time, 1e-3f)
        }
    }
}