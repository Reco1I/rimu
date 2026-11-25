package com.osudroid.game.objects

import com.osudroid.GameMode
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.game.FollowPointConnection
import org.junit.Assert.assertEquals
import org.junit.Test

class PoolSizesTest {

    private fun circles(vararg circles: Pair<Double, Vector2>) = Beatmap(GameMode.Droid).apply {
        for ((time, position) in circles) {
            hitObjects.add(HitCircle(time, position, false, 0))
        }

        for (obj in hitObjects.objects) {
            obj.applyDefaults(controlPoints, difficulty, GameMode.Droid)
            obj.applySamples(controlPoints)
        }
    }.hitObjects.objects.toTypedArray()

    @Test
    fun `Test circles alive together`() {
        val sizes = PoolSizes.of(circles(1000.0 to Vector2(0, 0), 1100.0 to Vector2(0, 0), 1200.0 to Vector2(0, 0)))

        assertEquals(3, sizes.activeObjects)
        assertEquals(3, sizes.circles)
        assertEquals(0, sizes.sliders)
        assertEquals(0, sizes.spinners)
        assertEquals(0, sizes.sliderTicks)
    }

    @Test
    fun `Test circles far apart are alive one at a time`() {
        val sizes = PoolSizes.of(circles(1000.0 to Vector2(0, 0), 20000.0 to Vector2(0, 0), 40000.0 to Vector2(0, 0)))

        assertEquals(1, sizes.activeObjects)
        assertEquals(1, sizes.circles)
    }

    @Test
    fun `Test follow points of a connection`() {
        val objects = circles(1000.0 to Vector2(0, 0), 1500.0 to Vector2(400, 0))
        val count = FollowPointConnection.pointCount(objects[0], objects[1])
        val distance = objects[1].gameplayStackedPosition.getDistance(objects[0].gameplayStackedEndPosition).toInt()

        // As addConnection places them: every 32 from 48, while 32 short of the end.
        var expected = 0
        var d = 48
        while (d < distance - 32) {
            expected++
            d += 32
        }

        assertEquals(expected, count)
        assertEquals(count, PoolSizes.of(objects).followPoints)
    }

    @Test
    fun `Test close objects have no follow points`() {
        val objects = circles(1000.0 to Vector2(0, 0), 1500.0 to Vector2(40, 0))

        assertEquals(0, FollowPointConnection.pointCount(objects[0], objects[1]))
        assertEquals(0, PoolSizes.of(objects).followPoints)
    }
}
