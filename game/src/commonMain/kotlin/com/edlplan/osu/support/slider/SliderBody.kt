package com.edlplan.osu.support.slider

import com.edlplan.andengine.TriangleBuilder
import com.osudroid.math.LinePath
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.game.UITriangleMesh
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class SliderBody(allowHint: Boolean) : UIContainer() {

    private var cache: RenderPathCache? = null

    private var maxPathLength = 0f

    private val background: UITriangleMesh

    private val borderLayer: UITriangleMesh

    private val hint: UITriangleMesh?

    private var backgroundWidth = 0f

    private var borderWidth = 0f

    private var hintWidth = 0f

    private var startLength = 0f

    private var endLength = 0f

    private var shouldRebuildVertices = true

    // Reused temporary vectors for the end-only hybrid fast path.
    private val tmpOthExpand = Vector2(0f)
    private val tmpStartL = Vector2(0f)
    private val tmpStartR = Vector2(0f)
    private val tmpEndL = Vector2(0f)
    private val tmpEndR = Vector2(0f)
    private val tmpCapCurrent = Vector2(0f)
    private val tmpCapCurrent2 = Vector2(0f)
    private val tmpCutPoint = Vector2(0f)

    // Tracks monotonic boundary movement during snaking to reduce segment lookups.
    private var lastFastEndSegmentIndex = -1
    private var lastFastStartSegmentIndex = -1
    private var lastFastEndLength = Float.NaN
    private var lastFastStartLength = Float.NaN

    init {

        if (allowHint) {
            hint = UITriangleMesh()
            hint.isVisible = false
            hint.clearDepthBeforeDraw = true
            hint.depthState = UITriangleMesh.DEPTH_LESS
        } else {
            hint = null
        }

        borderLayer = UITriangleMesh()
        borderLayer.depthState = UITriangleMesh.DEPTH_DEFAULT
        add(0, borderLayer)

        background = UITriangleMesh()
        background.depthState = UITriangleMesh.DEPTH_DEFAULT
        add(0, background)

        if (hint != null) {
            add(0, hint)
        }

        setHintVisible(false)
    }

    fun init(beginEmpty: Boolean, position: Vector2, cache: RenderPathCache) {
        this.cache = cache

        clearLayer(hint)
        clearLayer(background)
        clearLayer(borderLayer)

        val sourcePath = cache.sourcePath
        if (sourcePath == null || cache.pointCount == 0 || sourcePath.size() == 0) {
            maxPathLength = 0f
            startLength = 0f
            endLength = 0f
            shouldRebuildVertices = false
            lastFastEndSegmentIndex = -1
            lastFastStartSegmentIndex = -1
            lastFastEndLength = Float.NaN
            lastFastStartLength = Float.NaN
            setPosition(position.x, position.y)
            return
        }

        maxPathLength = sourcePath.getLength()
        startLength = 0f

        if (beginEmpty) {
            endLength = 0f
        } else {
            endLength = maxPathLength
        }

        shouldRebuildVertices = true
        lastFastEndSegmentIndex = -1
        lastFastStartSegmentIndex = -1
        lastFastEndLength = Float.NaN
        lastFastStartLength = Float.NaN
        setPosition(position.x, position.y)
    }


    fun setBackgroundWidth(value: Float) {
        backgroundWidth = value
    }

    fun setBackgroundColor(r: Float, g: Float, b: Float, a: Float) {
        background.color = Color4(r, g, b, a)
    }

    fun setBackgroundColor(color: Color4, alpha: Float) {
        background.color = color
        background.alpha = alpha
    }


    fun setHintVisible(visible: Boolean) {
        if (hint != null) {
            hint.isVisible = visible
            background.clearDepthBeforeDraw = !visible
            background.depthState = if (visible) UITriangleMesh.DEPTH_DEFAULT else UITriangleMesh.DEPTH_LESS
        } else {
            background.clearDepthBeforeDraw = true
            background.depthState = UITriangleMesh.DEPTH_LESS
        }
    }

    fun setHintWidth(value: Float) {
        hintWidth = value
    }

    fun setHintColor(r: Float, g: Float, b: Float, a: Float) {
        hint?.color = Color4(r, g, b, a)
    }

    fun setHintColor(color: Color4, alpha: Float) {
        if (hint != null) {
            hint.color = color
            hint.alpha = alpha
        }
    }


    fun setTrackBorderWidth(value: Float) {
        borderWidth = value
    }

    fun setTrackBorderColor(color: Color4) {
        borderLayer.color = color
    }

    private fun fitToCachedPath(source: LinePath, cache: LinePath): LinePath {
        cache.clear()

        val sourceSize = source.size()
        var previousPoint: Vector2? = null

        for (i in 0 until sourceSize) {
            val currentPoint = source[i]

            if (previousPoint == null || i == sourceSize - 1 || LinePath.distance(previousPoint, currentPoint) > pointMergeDistance) {
                cache.add(currentPoint)
                previousPoint = currentPoint
            }
        }

        return cache
    }

    private val clampedBackgroundWidth: Float
        get() = max(0f, backgroundWidth)

    private val playableWidth: Float
        get() = clampedBackgroundWidth - MathUtils.clamp(borderWidth, 0f, clampedBackgroundWidth)

    private val clampedHintWidth: Float
        get() = MathUtils.clamp(hintWidth, 0f, playableWidth)

    private fun buildVertices(subPath: LinePath) {
        val cache = cache!!
        val builder = cache.triangleBuilder

        if (hint != null && hint.isVisible) {
            cache.drawLinePath
                .reset(subPath, clampedHintWidth)
                .computeTriangles(builder)
                .applyVertices(hint.vertices)
        }

        cache.drawLinePath
            .reset(subPath, playableWidth)
            .computeTriangles(builder)
            .applyVertices(background.vertices)

        cache.drawLinePath
            .reset(subPath, clampedBackgroundWidth)
            .computeTriangles(builder)
            .applyVertices(borderLayer.vertices)
    }


    override fun onUpdate(clock: FrameClock) {
        val cache = cache

        if (cache != null && shouldRebuildVertices) {
            shouldRebuildVertices = false

            val clampedStartLength = MathUtils.clamp(startLength, 0f, maxPathLength)
            val clampedEndLength = MathUtils.clamp(endLength, clampedStartLength, maxPathLength)

            // Full-length path can be built directly without slicing/allocating an intermediate path.
            if (clampedStartLength <= lengthUpdateEpsilon &&
                abs(clampedEndLength - maxPathLength) <= lengthUpdateEpsilon) {
                buildVertices(cache.sourcePath!!)
            } else {
                // Hybrid reuse path is only valid when one edge is anchored to an endpoint.
                // Otherwise, fallback to exact cutPath triangulation.
                val isBuiltByFastPath =
                    (clampedStartLength <= lengthUpdateEpsilon && tryBuildFastEndLength(clampedEndLength)) ||
                    (abs(clampedEndLength - maxPathLength) <= lengthUpdateEpsilon && tryBuildFastStartLength(clampedStartLength))

                if (!isBuiltByFastPath) {
                    val cutPath = cache.sourcePath!!.cut(clampedStartLength, clampedEndLength)
                    buildVertices(fitToCachedPath(cutPath, cache.path))
                }
            }
        }

        super.onUpdate(clock)
    }

    override fun intersectsSceneRect(x: Float, y: Float, width: Float, height: Float): Boolean {
        // The path can extend in any direction from the position, so the bounds say nothing about what's visible.
        return true
    }

    fun setStartLength(length: Float) {
        val clampedLength = max(0f, min(length, maxPathLength))

        if (abs(clampedLength - startLength) <= lengthUpdateEpsilon) {
            return
        }

        startLength = clampedLength
        shouldRebuildVertices = true
    }

    fun setEndLength(length: Float) {
        val clampedLength = MathUtils.clamp(length, 0f, maxPathLength)

        if (abs(clampedLength - endLength) <= lengthUpdateEpsilon) {
            return
        }

        endLength = clampedLength
        shouldRebuildVertices = true
    }

    private fun tryBuildFastEndLength(endLength: Float): Boolean {
        val cache = cache

        // End-moving fast path: reuse full-path prefix and rebuild only the boundary segment + end cap.
        if (cache == null ||
            cache.pointCount < 2 ||
            cache.segmentCount < 1) {
            return false
        }

        val clampedEnd = MathUtils.clamp(endLength, 0f, maxPathLength)

        if (clampedEnd <= lengthUpdateEpsilon || clampedEnd >= maxPathLength - lengthUpdateEpsilon) {
            return false
        }

        val segmentIndex = getSegmentIndexForEndLength(clampedEnd)

        if (segmentIndex < 0 || segmentIndex >= cache.segmentCount) {
            return false
        }

        val segmentStart = cache.sourcePath!![segmentIndex]
        val cutPoint = interpolatePointAtLength(clampedEnd, segmentIndex, tmpCutPoint)
        val segmentTheta = cache.segmentThetas[segmentIndex]

        if (hint != null && hint.isVisible) {
            val width = clampedHintWidth

            if (width <= 0) {
                clearLayer(hint)
            } else if (!buildFastLayer(hint, cache.hint, width, segmentIndex, segmentStart, cutPoint,
                    segmentTheta)) {
                return false
            }
        }

        if (!buildFastLayer(background, cache.background, playableWidth,
                segmentIndex, segmentStart, cutPoint, segmentTheta)) {
            return false
        }

        return buildFastLayer(borderLayer, cache.border, clampedBackgroundWidth,
            segmentIndex, segmentStart, cutPoint, segmentTheta)
    }

    private fun tryBuildFastStartLength(startLength: Float): Boolean {
        val cache = cache

        // Start-moving fast path: rebuild start boundary and append cached suffix.
        if (cache == null ||
            cache.pointCount < 2 ||
            cache.segmentCount < 1) {
            return false
        }

        val clampedStart = MathUtils.clamp(startLength, 0f, maxPathLength)

        if (clampedStart <= lengthUpdateEpsilon || clampedStart >= maxPathLength - lengthUpdateEpsilon) {
            return false
        }

        val segmentIndex = getSegmentIndexForStartLength(clampedStart)

        if (segmentIndex < 0 || segmentIndex >= cache.segmentCount) {
            return false
        }

        val segmentEnd = cache.sourcePath!![segmentIndex + 1]
        val cutPoint = interpolatePointAtLength(clampedStart, segmentIndex, tmpCutPoint)
        val segmentTheta = cache.segmentThetas[segmentIndex]

        if (hint != null && hint.isVisible) {
            val width = clampedHintWidth

            if (width <= 0) {
                clearLayer(hint)
            } else if (!buildFastLayerFromStart(hint, cache.hint, width, segmentIndex, segmentEnd, cutPoint,
                    segmentTheta)) {
                return false
            }
        }

        if (!buildFastLayerFromStart(background, cache.background, playableWidth,
                segmentIndex, segmentEnd, cutPoint, segmentTheta)) {
            return false
        }

        return buildFastLayerFromStart(borderLayer, cache.border, clampedBackgroundWidth,
            segmentIndex, segmentEnd, cutPoint, segmentTheta)
    }

    private fun buildFastLayer(target: UITriangleMesh,
                               cache: RenderPathCache.ComponentCache,
                               width: Float,
                               segmentIndex: Int,
                               segmentStart: Vector2,
                               cutPoint: Vector2,
                               segmentTheta: Float): Boolean {
        val pathCache = this.cache!!

        // Prefix reuse layout: [cached triangles before boundary] + [rebuilt boundary].
        if (width <= 0 || !cache.hasCache || cache.vertexLength <= 0 || segmentIndex >= pathCache.segmentCount) {
            return false
        }

        val prefixLength = pathCache.segmentQuadStartOffsets[segmentIndex]
        val builder = pathCache.triangleBuilder

        if (builder.ary.size < cache.vertexLength + 128) {
            builder.ary = builder.ary.copyOf(cache.vertexLength + 128)
        }

        cache.vertices.copyInto(builder.ary, 0, 0, prefixLength)
        builder.length = prefixLength
        builder.maxX = cache.maxX
        builder.maxY = cache.maxY

        if (LinePath.distance(segmentStart, cutPoint) > lengthUpdateEpsilon) {
            addLineQuad(builder, segmentStart, cutPoint, width)
            addEndCap(builder, cutPoint, segmentTheta - MathUtils.HALF_PI_F, width)
        }

        builder.applyVertices(target.vertices)
        return true
    }

    private fun buildFastLayerFromStart(target: UITriangleMesh,
                                        cache: RenderPathCache.ComponentCache,
                                        width: Float,
                                        segmentIndex: Int,
                                        segmentEnd: Vector2,
                                        cutPoint: Vector2,
                                        segmentTheta: Float): Boolean {
        val pathCache = this.cache!!

        // Suffix reuse layout: [rebuilt boundary at start] + [cached triangles after boundary].
        if (width <= 0 || !cache.hasCache || cache.vertexLength <= 0 || segmentIndex >= pathCache.segmentCount) {
            return false
        }

        val builder = pathCache.triangleBuilder

        if (builder.ary.size < cache.vertexLength + 128) {
            builder.ary = builder.ary.copyOf(cache.vertexLength + 128)
        }

        builder.length = 0
        builder.maxX = cache.maxX
        builder.maxY = cache.maxY

        addStartCap(builder, cutPoint, segmentTheta + MathUtils.HALF_PI_F, width)

        if (LinePath.distance(cutPoint, segmentEnd) > lengthUpdateEpsilon) {
            addLineQuad(builder, cutPoint, segmentEnd, width)
        }

        if (segmentIndex < pathCache.segmentCount - 1) {
            val nextTheta = pathCache.segmentThetas[segmentIndex + 1]
            addLineCap(builder, segmentEnd, segmentTheta - MathUtils.HALF_PI_F, nextTheta - segmentTheta, width)

            val suffixOffset = pathCache.segmentQuadStartOffsets[segmentIndex + 1]
            val suffixLength = cache.vertexLength - suffixOffset

            if (suffixLength > 0) {
                cache.vertices.copyInto(builder.ary, builder.length, suffixOffset, suffixOffset + suffixLength)
                builder.length += suffixLength
            }
        } else {
            addEndCap(builder, segmentEnd, segmentTheta - MathUtils.HALF_PI_F, width)
        }

        builder.applyVertices(target.vertices)
        return true
    }

    private fun clearLayer(target: UITriangleMesh?) {
        if (target == null) {
            return
        }

        target.vertices.length = 0
    }

    private fun getSegmentIndexForEndLength(length: Float): Int {
        val segmentIndex = getSegmentIndexForLength(length, false, lastFastEndSegmentIndex, lastFastEndLength)

        lastFastEndSegmentIndex = segmentIndex
        lastFastEndLength = length

        return segmentIndex
    }

    private fun getSegmentIndexForStartLength(length: Float): Int {
        val segmentIndex = getSegmentIndexForLength(length, true, lastFastStartSegmentIndex, lastFastStartLength)

        lastFastStartSegmentIndex = segmentIndex
        lastFastStartLength = length

        return segmentIndex
    }

    private fun getSegmentIndexForLength(length: Float, preferNextOnExactPoint: Boolean,
                                         previousSegmentIndex: Int, previousLength: Float): Int {
        val cache = cache!!

        // Map a measured length to the segment containing that length.
        // For start cuts at exact points, prefer the next segment to avoid reusing removed geometry.
        val segmentCount = cache.segmentCount

        if (!previousLength.isNaN() && previousSegmentIndex >= 0 && previousSegmentIndex < segmentCount) {
            var segmentIndex = previousSegmentIndex

            if (length >= previousLength) {
                while (segmentIndex + 1 < segmentCount && cache.pointLengths[segmentIndex + 1] < length) {
                    segmentIndex++
                }
            } else {
                while (segmentIndex > 0 && cache.pointLengths[segmentIndex] > length) {
                    segmentIndex--
                }
            }

            // If next exact point is preferred, select it if its length falls within the epsilon.
            if (preferNextOnExactPoint && segmentIndex + 1 < cache.pointCount - 1) {
                val pointLength = cache.pointLengths[segmentIndex + 1]

                if (abs(pointLength - length) <= lengthUpdateEpsilon) {
                    return segmentIndex + 1
                }
            }

            return segmentIndex
        }

        var left = 0
        var right = cache.pointCount - 1

        while (left <= right) {
            val mid = left + ((right - left) shr 1)

            if (cache.pointLengths[mid] < length) {
                left = mid + 1
            } else {
                right = mid - 1
            }
        }

        val pointIndex = max(1, min(left, cache.pointCount - 1))
        var segmentIndex = pointIndex - 1

        if (preferNextOnExactPoint && pointIndex < cache.pointCount - 1) {
            val pointLength = cache.pointLengths[pointIndex]

            if (abs(pointLength - length) <= lengthUpdateEpsilon) {
                segmentIndex = pointIndex
            }
        }

        return max(0, min(segmentIndex, cache.segmentCount - 1))
    }

    private fun interpolatePointAtLength(length: Float, segmentIndex: Int, out: Vector2): Vector2 {
        val cache = cache!!
        val startLength = cache.pointLengths[segmentIndex]
        val endLength = cache.pointLengths[segmentIndex + 1]
        val segmentLength = endLength - startLength

        val startPoint = cache.sourcePath!![segmentIndex]

        if (segmentLength <= lengthUpdateEpsilon) {
            out.set(startPoint)
            return out
        }

        val t = MathUtils.clamp((length - startLength) / segmentLength, 0f, 1f)
        val endPoint = cache.sourcePath!![segmentIndex + 1]

        out.set(
            startPoint.x + (endPoint.x - startPoint.x) * t,
            startPoint.y + (endPoint.y - startPoint.y) * t
        )

        return out
    }

    private fun addLineQuad(builder: TriangleBuilder, pointStart: Vector2, pointEnd: Vector2, width: Float) {
        tmpOthExpand.set(LinePath.perpendicular(pointStart, pointEnd, tmpOthExpand))
        tmpOthExpand.scale(width)

        tmpStartL.set(pointStart)
        tmpStartL.add(tmpOthExpand)

        tmpStartR.set(pointStart)
        tmpStartR.subtract(tmpOthExpand)

        tmpEndL.set(pointEnd)
        tmpEndL.add(tmpOthExpand)

        tmpEndR.set(pointEnd)
        tmpEndR.subtract(tmpOthExpand)

        builder.add(pointStart, pointEnd, tmpEndL)
        builder.add(pointStart, tmpEndL, tmpStartL)
        builder.add(pointStart, tmpEndR, pointEnd)
        builder.add(pointStart, tmpStartR, tmpEndR)
    }

    private fun addEndCap(builder: TriangleBuilder, origin: Vector2, theta: Float, width: Float) {
        addLineCap(builder, origin, theta, MathUtils.PI_F, width)
    }

    private fun addStartCap(builder: TriangleBuilder, origin: Vector2, theta: Float, width: Float) {
        addLineCap(builder, origin, theta, MathUtils.PI_F, width)
    }

    private fun addLineCap(builder: TriangleBuilder, origin: Vector2, theta: Float, thetaDiff: Float, width: Float) {
        var theta = theta
        var thetaDiff = thetaDiff
        val step = MathUtils.PI_F / 24

        val dir = sign(thetaDiff)
        thetaDiff *= dir
        val amountPoints = ceil(thetaDiff / step).toInt()

        if (dir < 0) {
            theta += MathUtils.PI_F
        }

        tmpCapCurrent.set(LinePath.pointOnCircle(theta, tmpCapCurrent))
        tmpCapCurrent.scale(width)
        tmpCapCurrent.add(origin)

        for (i in 1..amountPoints) {
            tmpCapCurrent2.set(LinePath.pointOnCircle(theta + dir * min(i * step, thetaDiff), tmpCapCurrent2))
            tmpCapCurrent2.scale(width)
            tmpCapCurrent2.add(origin)

            builder.add(origin, tmpCapCurrent, tmpCapCurrent2)
            tmpCapCurrent.set(tmpCapCurrent2)
        }
    }

    class RenderPathCache {
        @JvmField
        val path = LinePath()
        @JvmField
        val triangleBuilder = TriangleBuilder()
        @JvmField
        val drawLinePath = DrawLinePath()

        @JvmField
        var sourcePath: LinePath? = null

        // Cumulative length at each path point, used to locate boundary segments without path slicing.
        @JvmField
        var pointLengths = FloatArray(0)
        @JvmField
        var pointCount = 0

        // Quad start offsets for each segment in the precached triangulation output.
        @JvmField
        var segmentQuadStartOffsets = IntArray(0)
        @JvmField
        var segmentCount = 0

        // Per-segment theta values for the source path.
        @JvmField
        var segmentThetas = FloatArray(0)

        @JvmField
        var hint = ComponentCache()
        @JvmField
        var background = ComponentCache()
        @JvmField
        var border = ComponentCache()

        // Full precached vertex buffer for a visual layer.
        class ComponentCache {
            @JvmField
            var vertices = FloatArray(0)
            @JvmField
            var vertexLength = 0
            @JvmField
            var maxX = 0f
            @JvmField
            var maxY = 0f
            @JvmField
            var hasCache = false

            fun setCache(builder: TriangleBuilder) {
                vertices = builder.ary.copyOf(builder.length)
                vertexLength = builder.length
                maxX = builder.maxX
                maxY = builder.maxY
                hasCache = true
            }
        }
    }

    companion object {
        private const val lengthUpdateEpsilon = 0.001f
        private const val pointMergeDistance = 0.01f

        @JvmStatic
        fun createCache(renderPath: LinePath, backgroundWidth: Float, borderWidth: Float,
                        isHintVisible: Boolean, hintWidth: Float, computeFullCache: Boolean): RenderPathCache {
            val cache = RenderPathCache()

            cache.sourcePath = renderPath

            if (renderPath.size() == 0) {
                cache.pointCount = 0
                cache.segmentCount = 0
                return cache
            }

            val clampedBackgroundWidth = max(0f, backgroundWidth)
            val clampedBorderWidth = MathUtils.clamp(borderWidth, 0f, clampedBackgroundWidth)
            val playableWidth = clampedBackgroundWidth - clampedBorderWidth
            val clampedHintWidth = MathUtils.clamp(hintWidth, 0f, playableWidth)

            buildPointLengthCache(renderPath, cache)
            buildSegmentThetaCache(renderPath, cache)

            cache.segmentCount = max(0, cache.pointCount - 1)

            if (computeFullCache) {
                val builder = cache.triangleBuilder

                // Build once to capture per-segment quad offsets used by hybrid prefix/suffix reuse.
                val offsetProbeWidth = if (playableWidth > 0) playableWidth else clampedBackgroundWidth

                if (offsetProbeWidth > 0) {
                    cache.drawLinePath.reset(renderPath, offsetProbeWidth).computeTriangles(builder)
                    cache.segmentCount = cache.drawLinePath.segmentQuadStartCount

                    if (cache.segmentQuadStartOffsets.size < cache.segmentCount) {
                        cache.segmentQuadStartOffsets = IntArray(cache.segmentCount)
                    }

                    for (i in 0 until cache.segmentCount) {
                        cache.segmentQuadStartOffsets[i] = cache.drawLinePath.getSegmentQuadStartOffset(i)
                    }
                }

                if (isHintVisible && clampedHintWidth > 0) {
                    cache.drawLinePath.reset(renderPath, clampedHintWidth).computeTriangles(builder)
                    cache.hint.setCache(builder)
                }

                if (playableWidth > 0) {
                    cache.drawLinePath.reset(renderPath, playableWidth).computeTriangles(builder)
                    cache.background.setCache(builder)
                }

                if (clampedBackgroundWidth > 0) {
                    cache.drawLinePath.reset(renderPath, clampedBackgroundWidth).computeTriangles(builder)
                    cache.border.setCache(builder)
                }
            }

            return cache
        }

        private fun buildPointLengthCache(path: LinePath, cache: RenderPathCache) {
            val size = path.size()

            if (cache.pointLengths.size < size) {
                cache.pointLengths = FloatArray(size)
            }

            var length = 0f
            cache.pointLengths[0] = 0f

            for (i in 1 until size) {
                length += LinePath.distance(path[i - 1], path[i])
                cache.pointLengths[i] = length
            }

            cache.pointCount = size
        }

        private fun buildSegmentThetaCache(path: LinePath, cache: RenderPathCache) {
            val segmentCount = max(0, path.size() - 1)

            if (cache.segmentThetas.size < segmentCount) {
                cache.segmentThetas = FloatArray(segmentCount)
            }

            for (i in 0 until segmentCount) {
                cache.segmentThetas[i] = LinePath.angle(path[i], path[i + 1])
            }
        }
    }
}
