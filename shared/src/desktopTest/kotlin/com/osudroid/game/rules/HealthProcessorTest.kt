package com.osudroid.game.rules

import com.osudroid.beatmaps.timings.BreakPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class HealthProcessorTest {

    private val statistic = StatisticV2()

    @Test
    fun `Drain depends on the HP and on the distance to the next object`() {
        val processor = HealthProcessor(statistic, 5f)

        assertEquals(1 + 5 / (2 * 0.5), processor.drainPerSecond(0.5), 1e-9)
        assertEquals(0.375, processor.drainPerSecond(0.0), 1e-9)
        assertEquals(0.375, HealthProcessor(statistic, 0f).drainPerSecond(0.5), 1e-9)
    }

    @Test
    fun `A frame drains its share of the health`() {
        val processor = HealthProcessor(statistic, 5f)

        assertFalse(processor.drain(1f, 0.5))
        assertEquals(1 - 0.06f, statistic.hp, 1e-6f)
    }

    @Test
    fun `A play that can't fail doesn't`() {
        statistic.canFail = false
        val processor = HealthProcessor(statistic, 10f)

        repeat(100) { assertFalse(processor.drain(1f, 0.1)) }
        assertEquals(0f, statistic.hp)
    }

    @Test
    fun `Lives restore the health before the play fails`() {
        val processor = HealthProcessor(statistic, 10f, lives = 3)

        repeat(3) {
            statistic.changeHp(-1f)
            assertFalse(processor.checkFailed())
            assertEquals(1f, statistic.hp)
        }

        statistic.changeHp(-1f)
        assertTrue(processor.checkFailed())
        assertEquals(3, processor.usedLives)
    }

    @Test
    fun `A long drained section uses as many lives as frame by frame`() {
        val processor = HealthProcessor(statistic, 0f, lives = 3)

        // 0.375 hundredths per second: 1 health in ~267 seconds, so 1000 seconds take three lives and some health.
        assertFalse(processor.drainSegment(1000.0, 0.0))
        assertEquals(3, processor.usedLives)
        assertTrue(statistic.hp < 1f)

        assertTrue(processor.drainSegment(1000.0, 0.0))
    }

    @Test
    fun `Breaks don't drain`() {
        val breaks = arrayOf(BreakPeriod(1000f, 2000f), BreakPeriod(3000f, 3500f))

        assertEquals(3.0, HealthProcessor.drainingSeconds(0.0, 4500.0, breaks), 1e-9)
        assertEquals(0.5, HealthProcessor.drainingSeconds(1500.0, 2500.0, breaks), 1e-9)
        assertEquals(0.0, HealthProcessor.drainingSeconds(1200.0, 1800.0, breaks), 1e-9)
        assertEquals(0.0, HealthProcessor.drainingSeconds(2000.0, 1000.0, breaks), 1e-9)
        assertEquals(1.0, HealthProcessor.drainingSeconds(0.0, 1000.0, null), 1e-9)
    }
}
