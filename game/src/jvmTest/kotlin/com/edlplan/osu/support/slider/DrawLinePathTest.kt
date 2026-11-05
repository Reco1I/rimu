package com.edlplan.osu.support.slider

import com.edlplan.andengine.TriangleBuilder
import com.osudroid.math.LinePath
import com.osudroid.math.Vector2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawLinePathTest {
    private fun path(vararg points: Pair<Float, Float>) = LinePath().apply {
        points.forEach { add(Vector2(it.first, it.second)) }
        measure()
    }

    private fun triangles(path: LinePath, width: Float) =
        DrawLinePath().reset(path, width).computeTriangles(TriangleBuilder())

    @Test
    fun `Test single point is a circle`() {
        val builder = triangles(path(10f to 10f), 5f)

        // Two half circle caps of 24 triangles, 6 floats each.
        assertEquals(2 * 24 * 6, builder.length)
        assertEquals(15f, builder.maxX, 1e-4f)
    }

    @Test
    fun `Test straight line`() {
        val draw = DrawLinePath()
        val builder = draw.reset(path(0f to 0f, 100f to 0f), 5f).computeTriangles(TriangleBuilder())

        // Two half circle caps and the four triangles of the line.
        assertEquals((2 * 24 + 4) * 6, builder.length)
        assertEquals(1, draw.segmentQuadStartCount)
        assertEquals(105f, builder.maxX, 1e-4f)
        assertEquals(5f, builder.maxY, 1e-4f)
    }

    @Test
    fun `Test bent line`() {
        val draw = DrawLinePath()
        val builder = draw.reset(path(0f to 0f, 100f to 0f, 100f to 100f), 5f).computeTriangles(TriangleBuilder())

        assertEquals(2, draw.segmentQuadStartCount)
        assertTrue(draw.getSegmentQuadStartOffset(1) > draw.getSegmentQuadStartOffset(0))
        assertEquals(105f, builder.maxX, 1e-4f)
        assertEquals(105f, builder.maxY, 1e-4f)
    }
}
