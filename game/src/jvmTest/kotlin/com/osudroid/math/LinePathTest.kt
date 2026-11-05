package com.osudroid.math

import org.junit.Assert.assertEquals
import org.junit.Test

class LinePathTest {
    private fun path(vararg points: Pair<Float, Float>) = LinePath().apply {
        points.forEach { add(Vector2(it.first, it.second)) }
        measure()
    }

    private fun assertPoint(x: Float, y: Float, point: Vector2) {
        assertEquals(x, point.x, 1e-4f)
        assertEquals(y, point.y, 1e-4f)
    }

    @Test
    fun `Test length`() {
        assertEquals(0f, path(0f to 0f).getLength(), 0f)
        assertEquals(7f, path(0f to 0f, 3f to 4f, 3f to 6f).getLength(), 1e-4f)
    }

    @Test
    fun `Test index at length`() {
        val path = path(0f to 0f, 10f to 0f, 10f to 10f)

        assertEquals(0, path.indexAt(0f))
        assertEquals(0, path.indexAt(9.9f))
        assertEquals(1, path.indexAt(10f))
        assertEquals(1, path.indexAt(15f))
        assertEquals(2, path.indexAt(20f))
        assertEquals(2, path.indexAt(30f))
    }

    @Test
    fun `Test point at length`() {
        val path = path(0f to 0f, 10f to 0f, 10f to 10f)

        assertPoint(5f, 0f, path.pointAt(5f))
        assertPoint(10f, 5f, path.pointAt(15f))
        // Past the end, the last line continues.
        assertPoint(10f, 15f, path.pointAt(25f))
    }

    @Test
    fun `Test cut`() {
        val cut = path(0f to 0f, 10f to 0f, 10f to 10f).cut(5f, 15f)

        assertEquals(3, cut.size())
        assertPoint(5f, 0f, cut[0])
        assertPoint(10f, 0f, cut[1])
        assertPoint(10f, 5f, cut[2])
    }

    @Test
    fun `Test cut merges close points`() {
        val cut = path(0f to 0f, 10f to 0f, 10f to 10f).cut(10f, 15f)

        assertEquals(2, cut.size())
        assertPoint(10f, 0f, cut[0])
        assertPoint(10f, 5f, cut[1])
    }

    @Test
    fun `Test perpendicular and angle`() {
        assertPoint(0f, 1f, LinePath.perpendicular(Vector2(0f), Vector2(5f, 0f), Vector2(0f)))
        assertEquals(MathUtils.HALF_PI_F, LinePath.angle(Vector2(0f), Vector2(0f, 3f)), 1e-6f)
    }
}
