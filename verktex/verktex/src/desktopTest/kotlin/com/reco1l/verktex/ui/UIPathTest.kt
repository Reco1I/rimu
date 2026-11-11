package com.reco1l.verktex.ui

import com.reco1l.verktex.math.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class UIPathTest {

    private fun path(path: Path) = UIPath().apply {
        this.path = path
        halfWidth = 5f
    }

    // A half circle cap is 24 triangles of 3 vertices.
    private val cap = 24 * 3

    // The four triangles of a segment.
    private val quad = 4 * 3

    @Test
    fun `Test single point is a circle`() {
        assertEquals(2 * cap, path(Path.of(10f, 10f)).meshVertexCount)
    }

    @Test
    fun `Test straight line`() {
        assertEquals(2 * cap + quad, path(Path.of(0f, 0f, 100f, 0f)).meshVertexCount)
    }

    @Test
    fun `Test bent line has a join`() {
        // A quarter turn join is 12 triangles.
        assertEquals(2 * cap + 2 * quad + 12 * 3, path(Path.of(0f, 0f, 100f, 0f, 100f, 100f)).meshVertexCount)
    }

    @Test
    fun `Test the whole path is one range`() {
        val path = path(Path.of(0f, 0f, 100f, 0f, 100f, 100f))
        assertEquals(Triple(0, path.meshVertexCount, 0), path.drawnVertices)
    }

    @Test
    fun `Test snaking in draws the start and a rebuilt end`() {
        val path = path(Path.of(0f, 0f, 100f, 0f, 100f, 100f))
        path.endLength = 150f

        // The start cap, the first segment and the join, then the partial second segment and its end cap.
        assertEquals(Triple(0, cap + quad + 12 * 3, quad + cap), path.drawnVertices)
    }

    @Test
    fun `Test snaking out draws a rebuilt start and the end`() {
        val path = path(Path.of(0f, 0f, 100f, 0f, 100f, 100f))
        path.startLength = 50f

        // The start cap, the partial first segment and the join, then the second segment and the end cap.
        assertEquals(Triple(cap + quad + 12 * 3, path.meshVertexCount, cap + quad + 12 * 3), path.drawnVertices)
    }

    @Test
    fun `Test a part within a segment is only rebuilt`() {
        val path = path(Path.of(0f, 0f, 100f, 0f))
        path.startLength = 20f
        path.endLength = 60f

        assertEquals(Triple(0, 0, cap + quad + cap), path.drawnVertices)
    }
}
