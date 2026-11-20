package com.osudroid.game.session

import TestResourceManager
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.game.GameplayTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlPointTrackerTest {

    private val controlPoints = BeatmapParser(
        TestResourceManager.getBeatmapFile("YOASOBI - Love Letter (ohm002) [Please accept my overflowing emotions.]")!!.path
    ).parse(true)!!.controlPoints

    private val timingPoints = controlPoints.timing.controlPoints.toTypedArray()
    private val effectPoints = controlPoints.effect.controlPoints.toTypedArray()

    private val timing = GameplayTiming()

    private val tracker = ControlPointTracker(
        timingPoints, effectPoints,
        controlPoints.timing.defaultControlPoint, controlPoints.effect.defaultControlPoint,
        timing
    )


    @Test
    fun `It starts at the first points`() {
        assertSame(timingPoints.first(), tracker.timingPoint)
        assertSame(effectPoints.first(), tracker.effectPoint)
        assertEquals(timingPoints.first().msPerBeat / 1000, timing.beatLength, 1e-9)
    }

    @Test
    fun `It follows the points as the play goes on`() {
        for (i in timingPoints.indices) {
            val time = (timingPoints[i].time / 1000).toFloat() + 0.001f
            tracker.update(time)

            assertSame(timingPoints.last { it.time <= time * 1000.0 }, tracker.timingPoint)
            assertEquals(tracker.timingPoint.msPerBeat / 1000, timing.beatLength, 1e-9)
            assertEquals(tracker.effectPoint.isKiai, timing.isKiai)
            assertTrue(timing.currentBeatTime >= 0 && timing.currentBeatTime < timing.beatLength)
        }
    }

    @Test
    fun `A seek moves back and forth`() {
        val last = timingPoints.last()
        tracker.seekTo(last.time.toFloat())
        assertSame(last, tracker.timingPoint)

        tracker.seekTo(0f)
        assertSame(timingPoints.first(), tracker.timingPoint)
        assertSame(effectPoints.first(), tracker.effectPoint)
    }
}
