package com.edlplan.osu.support.slider

import com.edlplan.andengine.TriangleBuilder
import com.osudroid.math.LinePath
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sign

class DrawLinePath {

    @JvmField
    var alpha: Float
    @JvmField
    var width = 0f
    // Created up front, the old vectors created them in their first use and then reused them.
    var current = Vector2(0f)
    var current2 = Vector2(0f)
    var startL = Vector2(0f)
    var startR = Vector2(0f)
    var endL = Vector2(0f)
    var endR = Vector2(0f)
    var start = Vector2(0f)
    var end = Vector2(0f)
    var oth_expand = Vector2(0f)
    private var triangles: TriangleBuilder? = null
    private var path: LinePath? = null

    private var segmentThetas = FloatArray(0)
    private var segmentQuadStartOffsets = IntArray(0)
    var segmentQuadStartCount = 0
        private set
    private var cachedThetaPath: LinePath? = null
    private var cachedThetaPathSize = 0
    private var cachedFirstX = 0f
    private var cachedFirstY = 0f
    private var cachedLastX = 0f
    private var cachedLastY = 0f

    init {
        alpha = 1f
    }

    fun reset(p: LinePath, width: Float): DrawLinePath {
        alpha = 1f
        path = p
        this.width = width
        ensureThetaCache()
        triangles?.length = 0
        return this
    }

    fun computeTriangles(builder: TriangleBuilder): TriangleBuilder {
        val cache = triangles
        if (cache != null) {
            cache.applyVertices(builder)
        } else {
            triangles = builder
            builder.reset()
            init()
        }
        triangles = cache
        return builder
    }

    fun getSegmentQuadStartOffset(segmentIndex: Int): Int {
        if (segmentIndex < 0 || segmentIndex >= segmentQuadStartCount) {
            return 0
        }

        return segmentQuadStartOffsets[segmentIndex]
    }


    private fun addLineCap(org: Vector2, theta: Float, thetaDiff: Float) {
        var theta = theta
        var thetaDiff = thetaDiff
        val step = MathUtils.PI_F / MAXRES

        val dir = sign(thetaDiff)
        thetaDiff *= dir
        val amountPoints = ceil(thetaDiff / step).toInt()

        if (dir < 0)
            theta += MathUtils.PI_F

        /* current = org + atCircle(...)*width */
        current = LinePath.pointOnCircle(theta, current).scale(width).add(org)

        for (i in 1..amountPoints) {
            current2 = LinePath.pointOnCircle(theta + dir * min(i * step, thetaDiff), current2)
                .scale(width)
                .add(org)
            triangles!!.add(
                org,
                current,
                current2
            )
            current.set(current2)
        }
    }

    private fun addLineQuads(ps: Vector2, pe: Vector2) {
        oth_expand = LinePath.perpendicular(ps, pe, oth_expand).scale(width)

        startL.set(ps)
        startL.add(oth_expand)
        startR.set(ps)
        startR.subtract(oth_expand)
        endL.set(pe)
        endL.add(oth_expand)
        endR.set(pe)
        endR.subtract(oth_expand)
        start = ps
        end = pe

        val triangles = triangles!!

        triangles.add(
            start,
            end,
            endL
        )

        triangles.add(
            start,
            endL,
            startL
        )

        triangles.add(
            start,
            endR,
            end
        )

        triangles.add(
            start,
            startR,
            endR
        )
    }

    private fun ensureThetaCache() {
        val path = path
        val size = path?.size() ?: 0

        if (size < 2) {
            cachedThetaPath = path
            cachedThetaPathSize = size
            return
        }

        val first = path!![0]
        val last = path[size - 1]

        if (cachedThetaPath === path &&
            cachedThetaPathSize == size &&
            cachedFirstX == first.x && cachedFirstY == first.y &&
            cachedLastX == last.x && cachedLastY == last.y) {
            return
        }

        val segmentCount = size - 1

        if (segmentThetas.size < segmentCount) {
            segmentThetas = FloatArray(segmentCount)
        }

        for (i in 0 until segmentCount) {
            segmentThetas[i] = LinePath.angle(path[i], path[i + 1])
        }

        cachedThetaPath = path
        cachedThetaPathSize = size
        cachedFirstX = first.x
        cachedFirstY = first.y
        cachedLastX = last.x
        cachedLastY = last.y
    }

    private fun init() {
        val path = path!!

        if (path.size() < 2) {
            segmentQuadStartCount = 0
            if (path.size() == 1) {
                addLineCap(path[0], MathUtils.PI_F, MathUtils.PI_F)
                addLineCap(path[0], 0f, MathUtils.PI_F)
            }
            return
        }

        val segmentCount = path.size() - 1

        if (segmentQuadStartOffsets.size < segmentCount) {
            segmentQuadStartOffsets = IntArray(segmentCount)
        }

        segmentQuadStartCount = segmentCount

        val theta = segmentThetas[0]
        addLineCap(path[0], theta + MathUtils.HALF_PI_F, MathUtils.PI_F)
        segmentQuadStartOffsets[0] = triangles!!.length
        addLineQuads(path[0], path[1])
        if (path.size() == 2) {
            addLineCap(path[1], theta - MathUtils.HALF_PI_F, MathUtils.PI_F)
            return
        }
        var nowPoint = path[1]
        var nextPoint: Vector2
        var preTheta = theta
        var nextTheta: Float
        val max_i = path.size()
        for (i in 2 until max_i) {
            nextPoint = path[i]
            nextTheta = segmentThetas[i - 1]
            addLineCap(nowPoint, preTheta - MathUtils.HALF_PI_F, nextTheta - preTheta)
            segmentQuadStartOffsets[i - 1] = triangles!!.length
            addLineQuads(nowPoint, nextPoint)
            nowPoint = nextPoint
            preTheta = nextTheta
        }
        addLineCap(path[max_i - 1], preTheta - MathUtils.HALF_PI_F, MathUtils.PI_F)
    }

    companion object {
        private const val MAXRES = 24
    }
}
