package com.reco1l.verktex.math

import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import org.junit.Assert.assertEquals
import org.junit.Test

class PathTest {

    private fun assertPoint(x: Float, y: Float, point: Vec2) {
        assertEquals(x, point.x, 1e-4f)
        assertEquals(y, point.y, 1e-4f)
    }

    @Test
    fun `Test length`() {
        assertEquals(0f, Path.of(0f, 0f).length, 0f)
        assertEquals(0f, Path.Empty.length, 0f)
        assertEquals(7f, Path.of(0f, 0f, 3f, 4f, 3f, 6f).length, 1e-4f)
        assertEquals(5f, Path.of(0f, 0f, 3f, 4f, 3f, 6f).lengthAt(1), 1e-4f)
    }

    @Test
    fun `Test index at length`() {
        val path = Path.of(0f, 0f, 10f, 0f, 10f, 10f)

        assertEquals(0, path.indexAt(0f))
        assertEquals(0, path.indexAt(9.9f))
        assertEquals(1, path.indexAt(10f))
        assertEquals(1, path.indexAt(15f))
        assertEquals(2, path.indexAt(20f))
        assertEquals(2, path.indexAt(30f))
    }

    @Test
    fun `Test point at length`() {
        val path = Path.of(0f, 0f, 10f, 0f, 10f, 10f)

        assertPoint(5f, 0f, path.pointAt(5f))
        assertPoint(10f, 5f, path.pointAt(15f))
        // Past the end, the last line continues.
        assertPoint(10f, 15f, path.pointAt(25f))
    }

    @Test
    fun `Test cut`() {
        val cut = Path.of(0f, 0f, 10f, 0f, 10f, 10f).cut(5f, 15f)

        assertEquals(3, cut.pointCount)
        assertPoint(5f, 0f, cut[0])
        assertPoint(10f, 0f, cut[1])
        assertPoint(10f, 5f, cut[2])
        assertEquals(10f, cut.length, 1e-4f)
    }

    @Test
    fun `Test cut merges close points`() {
        val cut = Path.of(0f, 0f, 10f, 0f, 10f, 10f).cut(10f, 15f)

        assertEquals(2, cut.pointCount)
        assertPoint(10f, 0f, cut[0])
        assertPoint(10f, 5f, cut[1])
    }

    @Test
    fun `Test simplified keeps the ends and drops close points`() {
        val path = Path.of(0f, 0f, 1f, 0f, 5f, 0f, 6f, 0f, 6.5f, 0f).simplified(2f)

        assertEquals(3, path.pointCount)
        assertPoint(0f, 0f, path[0])
        assertPoint(5f, 0f, path[1])
        assertPoint(6.5f, 0f, path[2])
    }

    @Test
    fun `Test bounds`() {
        assertEquals(Vec4(-2f, 1f, 10f, 7f), Path.of(0f, 1f, -2f, 7f, 10f, 3f).bounds)
    }
}
