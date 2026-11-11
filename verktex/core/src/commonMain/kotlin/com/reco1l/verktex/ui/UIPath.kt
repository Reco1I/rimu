package com.reco1l.verktex.ui

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.CompareFunction
import com.reco1l.verktex.graphics.DepthState
import com.reco1l.verktex.graphics.GraphicsBuffer
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.VertexLayout
import com.reco1l.verktex.math.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/**
 * Draws a [path] stroked [halfWidth] away from it on each side, with round caps and joins, in up to
 * three bands from the centre out: a hint, the body and a border. Only the part between
 * [startLength] and [endLength] along the path is drawn, e.g. to snake it in or out.
 *
 * The path is triangulated once, every vertex carrying its distance from the centre line, and the
 * mesh stays on the GPU between frames. Each band is a pass over that mesh discarding what's
 * farther than its width. All of them draw at the same depth with [DepthState] testing for less, so
 * every pixel is drawn once, by the innermost band covering it: overlapping parts of a translucent
 * path don't blend twice. Moving [startLength] or [endLength] draws a range of the mesh plus a small
 * rebuilt piece for the partial segments and their caps.
 */
class UIPath : UICanvas() {

    /**
     * The path to draw, in the local space of this component.
     */
    var path: Path = Path.Empty
        set(value) {
            if (field !== value) {
                field = value
                isMeshDirty = true
            }
        }

    /**
     * How far the path is drawn from its centre line on each side.
     */
    var halfWidth = 0f
        set(value) {
            if (field != value) {
                field = value
                isMeshDirty = true
            }
        }

    /**
     * The width of the border, the outermost band, out of [halfWidth].
     */
    var borderWidth = 0f

    /**
     * The width of the hint, the innermost band, out of [halfWidth]. No hint is drawn at 0.
     */
    var hintWidth = 0f

    var bodyColor = Color4.White
    var borderColor = Color4.White
    var hintColor = Color4.White

    /**
     * Where the drawn part starts along the path.
     */
    var startLength = 0f
        set(value) {
            if (field != value) {
                field = value
                isPieceDirty = true
            }
        }

    /**
     * Where the drawn part ends along the path, past its end the whole path is drawn.
     */
    var endLength = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                isPieceDirty = true
            }
        }

    /**
     * The length of the path as drawn, without the points that were too close to the previous one.
     */
    val length: Float
        get() {
            ensureMesh()
            return meshPath.length
        }


    /**
     * The vertices of the whole path's mesh, for tests.
     */
    internal val meshVertexCount: Int
        get() {
            ensureMesh()
            return mesh.vertexCount
        }

    /**
     * The range of the mesh drawn and the vertices of the rebuilt piece, for tests.
     */
    internal val drawnVertices: Triple<Int, Int, Int>
        get() {
            ensureMesh()
            ensurePiece()
            return Triple(rangeStart, rangeEnd, piece.vertexCount)
        }

    // The path without points closer than MERGE_DISTANCE, a zero length line has no direction.
    private var meshPath = Path.Empty

    // The whole path, see the layout in buildMesh().
    private val mesh = MeshBuilder()
    private var isMeshDirty = true

    // The vertex where the quad of each segment starts, after the join at its first point.
    private var quadStarts = IntArray(0)
    private var endCapStart = 0
    private var segmentAngles = FloatArray(0)

    // The partial segments and caps of the drawn part.
    private val piece = MeshBuilder()
    private var isPieceDirty = true
    private var rangeStart = 0
    private var rangeEnd = 0

    private var meshBuffer: GraphicsBuffer? = null
    private var pieceBuffer: GraphicsBuffer? = null
    private var isMeshUploaded = false
    private var isPieceUploaded = false


    init {
        clearDepthBeforeDraw = true
        depthState = DEPTH_LESS
    }


    override fun intersectsSceneRect(x: Float, y: Float, width: Float, height: Float): Boolean {
        // The path can extend in any direction from the position, so the bounds say nothing about what's visible.
        return true
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        ensureMesh()
        ensurePiece()

        if (halfWidth <= 0f || (rangeStart == rangeEnd && piece.vertexCount == 0)) {
            return
        }

        val meshBuffer = meshBuffer ?: renderer.createVertexBuffer().also { meshBuffer = it }
        val pieceBuffer = pieceBuffer ?: renderer.createVertexBuffer().also { pieceBuffer = it }

        if (!isMeshUploaded) {
            mesh.writeTo(meshBuffer)
            isMeshUploaded = true
        }

        if (!isPieceUploaded) {
            piece.writeTo(pieceBuffer)
            isPieceUploaded = true
        }

        val transform = renderer.currentTransform

        // Column-major, with the 2D transformation in the X and Y axes.
        val matrix = floatArrayOf(
            transform.a, transform.b, 0f, 0f,
            transform.c, transform.d, 0f, 0f,
            0f, 0f, 1f, 0f,
            transform.tx, transform.ty, 0f, 1f
        )

        val outerWidth = halfWidth
        val clampedBorder = borderWidth.coerceIn(0f, outerWidth)
        val bodyWidth = outerWidth - clampedBorder
        val clampedHint = hintWidth.coerceIn(0f, bodyWidth)

        val content = contentColor

        fun pass(color: Color4, maxDistance: Float) {
            val uniforms = GraphicsShader.UniformSet().apply {
                set("uTransform", matrix)
                set("uMaxDistance", maxDistance)
                set("uColor", renderer.transformColor(Color4(
                    color.red * content.red,
                    color.green * content.green,
                    color.blue * content.blue,
                    color.alpha
                )))
            }

            renderer.drawVertices(BuiltIn.Shaders.Path, meshBuffer, LAYOUT, rangeStart, rangeEnd - rangeStart, uniforms)
            renderer.drawVertices(BuiltIn.Shaders.Path, pieceBuffer, LAYOUT, 0, piece.vertexCount, uniforms)
        }

        if (clampedHint > 0f) {
            pass(hintColor, clampedHint / outerWidth)
        }
        if (bodyWidth > 0f) {
            pass(bodyColor, bodyWidth / outerWidth)
        }
        if (clampedBorder > 0f) {
            pass(borderColor, 1f)
        }
    }


    private fun ensureMesh() {
        if (!isMeshDirty) {
            return
        }

        isMeshDirty = false
        isMeshUploaded = false
        isPieceDirty = true

        meshPath = path.simplified(MERGE_DISTANCE)
        buildMesh()
    }

    /**
     * The mesh is laid out along the path: the start cap, then for each segment the join at its
     * first point (but the first one) and its quad, then the end cap. So the part of the path up to
     * a point is a range of it.
     */
    private fun buildMesh() {
        val path = meshPath
        val points = path.pointCount
        val segments = max(0, points - 1)

        mesh.clear()

        if (points == 0) {
            quadStarts = IntArray(0)
            endCapStart = 0
            return
        }

        if (points == 1) {
            quadStarts = IntArray(0)
            mesh.addCap(path.x(0), path.y(0), PI_F, PI_F, halfWidth)
            endCapStart = mesh.vertexCount
            mesh.addCap(path.x(0), path.y(0), 0f, PI_F, halfWidth)
            return
        }

        segmentAngles = FloatArray(segments) { atan2(path.y(it + 1) - path.y(it), path.x(it + 1) - path.x(it)) }
        quadStarts = IntArray(segments)

        mesh.addCap(path.x(0), path.y(0), segmentAngles[0] + HALF_PI_F, PI_F, halfWidth)

        for (segment in 0 until segments) {
            if (segment > 0) {
                val previous = segmentAngles[segment - 1]
                mesh.addCap(path.x(segment), path.y(segment), previous - HALF_PI_F, segmentAngles[segment] - previous, halfWidth)
            }

            quadStarts[segment] = mesh.vertexCount
            mesh.addQuad(path.x(segment), path.y(segment), path.x(segment + 1), path.y(segment + 1), halfWidth)
        }

        endCapStart = mesh.vertexCount
        mesh.addCap(path.x(points - 1), path.y(points - 1), segmentAngles[segments - 1] - HALF_PI_F, PI_F, halfWidth)
    }

    private fun quadStartOrEndCap(segment: Int) = if (segment < quadStarts.size) quadStarts[segment] else endCapStart

    private fun ensurePiece() {
        if (!isPieceDirty) {
            return
        }

        isPieceDirty = false
        isPieceUploaded = false
        piece.clear()

        val path = meshPath
        val total = path.length
        val segments = path.pointCount - 1

        if (path.pointCount < 2) {
            rangeStart = 0
            rangeEnd = mesh.vertexCount
            return
        }

        val start = startLength.coerceIn(0f, total)
        val end = endLength.coerceIn(start, total)
        val isStartWhole = start <= EPSILON
        val isEndWhole = end >= total - EPSILON

        if (isStartWhole && isEndWhole) {
            rangeStart = 0
            rangeEnd = mesh.vertexCount
            return
        }

        fun segmentAt(length: Float) = path.indexAt(length).coerceIn(0, segments - 1)

        val startSegment = segmentAt(start)
        val endSegment = segmentAt(end)
        val startPoint = path.pointAt(start)
        val endPoint = path.pointAt(end)

        if (!isStartWhole) {
            piece.addCap(startPoint.x, startPoint.y, segmentAngles[startSegment] + HALF_PI_F, PI_F, halfWidth)
        }

        // The partial segment the drawn part starts on, up to its end point and its join.
        fun addStartSegment() {
            val next = startSegment + 1
            piece.addQuadIfLong(startPoint.x, startPoint.y, path.x(next), path.y(next), halfWidth)

            if (next < segments) {
                val angle = segmentAngles[startSegment]
                piece.addCap(path.x(next), path.y(next), angle - HALF_PI_F, segmentAngles[next] - angle, halfWidth)
            }
        }

        // The partial segment the drawn part ends on, from its start point, and the end cap.
        fun addEndSegment(fromX: Float, fromY: Float) {
            piece.addQuadIfLong(fromX, fromY, endPoint.x, endPoint.y, halfWidth)
            piece.addCap(endPoint.x, endPoint.y, segmentAngles[endSegment] - HALF_PI_F, PI_F, halfWidth)
        }

        when {
            isEndWhole -> {
                addStartSegment()
                rangeStart = quadStartOrEndCap(startSegment + 1)
                rangeEnd = mesh.vertexCount
            }

            isStartWhole -> {
                rangeStart = 0
                rangeEnd = quadStarts[endSegment]
                addEndSegment(path.x(endSegment), path.y(endSegment))
            }

            startSegment == endSegment -> {
                rangeStart = 0
                rangeEnd = 0
                addEndSegment(startPoint.x, startPoint.y)
            }

            else -> {
                addStartSegment()
                rangeStart = quadStarts[startSegment + 1]
                rangeEnd = quadStarts[endSegment]
                addEndSegment(path.x(endSegment), path.y(endSegment))
            }
        }
    }


    /**
     * Vertices of a path's mesh: a position and the distance from the centre line, see path.vert.
     */
    private class MeshBuilder {

        var vertices = FloatArray(3 * 256)
        var vertexCount = 0

        fun clear() {
            vertexCount = 0
        }

        private fun add(x: Float, y: Float, distance: Float) {
            if (vertexCount * 3 + 3 > vertices.size) {
                vertices = vertices.copyOf(vertices.size * 2)
            }

            val i = vertexCount * 3
            vertices[i] = x
            vertices[i + 1] = y
            vertices[i + 2] = distance
            vertexCount++
        }

        /**
         * The rectangle of a segment as four triangles from its start point, as the old slider
         * body did, with the centre line at distance 0 and the edges at 1.
         */
        fun addQuad(startX: Float, startY: Float, endX: Float, endY: Float, width: Float) {
            val length = hypot(endX - startX, endY - startY)
            val offsetX = -(endY - startY) / length * width
            val offsetY = (endX - startX) / length * width

            add(startX, startY, 0f); add(endX, endY, 0f); add(endX + offsetX, endY + offsetY, 1f)
            add(startX, startY, 0f); add(endX + offsetX, endY + offsetY, 1f); add(startX + offsetX, startY + offsetY, 1f)
            add(startX, startY, 0f); add(endX - offsetX, endY - offsetY, 1f); add(endX, endY, 0f)
            add(startX, startY, 0f); add(startX - offsetX, startY - offsetY, 1f); add(endX - offsetX, endY - offsetY, 1f)
        }

        fun addQuadIfLong(startX: Float, startY: Float, endX: Float, endY: Float, width: Float) {
            if (hypot(endX - startX, endY - startY) > EPSILON) {
                addQuad(startX, startY, endX, endY, width)
            }
        }

        /**
         * A fan around ([x], [y]) from [angle] turning [sweep] radians, in steps of a 24th of a
         * half turn.
         */
        fun addCap(x: Float, y: Float, angle: Float, sweep: Float, width: Float) {
            val direction = sign(sweep)
            val absoluteSweep = abs(sweep)
            val steps = ceil(absoluteSweep / CAP_STEP).toInt()
            val from = if (direction < 0) angle + PI_F else angle

            var previousX = x + cos(from) * width
            var previousY = y + sin(from) * width

            for (i in 1..steps) {
                val current = from + direction * min(i * CAP_STEP, absoluteSweep)
                val currentX = x + cos(current) * width
                val currentY = y + sin(current) * width

                add(x, y, 0f)
                add(previousX, previousY, 1f)
                add(currentX, currentY, 1f)

                previousX = currentX
                previousY = currentY
            }
        }

        fun writeTo(buffer: GraphicsBuffer) {
            buffer.clear()
            buffer.requireCapacity(vertexCount * 3 * 4)
            for (i in 0 until vertexCount * 3) {
                buffer.putFloat(vertices[i])
            }
        }
    }


    companion object {
        private const val PI_F = PI.toFloat()
        private const val HALF_PI_F = (PI / 2).toFloat()
        private const val CAP_STEP = PI_F / 24
        private const val EPSILON = 0.001f
        private const val MERGE_DISTANCE = 0.01f

        private val LAYOUT = VertexLayout(listOf(
            VertexLayout.Attribute(0, 2),
            VertexLayout.Attribute(1, 1)
        ))

        private val DEPTH_LESS = DepthState(enabled = true, writeEnabled = true, function = CompareFunction.Less)
    }
}
