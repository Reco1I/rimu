package com.reco1l.rimu.graphics

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT
import com.badlogic.gdx.graphics.GL20.GL_DEPTH_TEST
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import com.reco1l.rimu.data.Color4
import com.reco1l.toolkt.MathF
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.math.Vector2
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin



class PathMeshDrawer(val segments: MutableList<Line>)
{

    /**
     * The line radius.
     */
    var radius = HitObject.OBJECT_RADIUS

    /**
     * The inner color of the gradient.
     */
    var innerColor: Color = Color.WHITE

    /**
     * The outer color of the gradient.
     */
    var outerColor: Color = Color4(80, 80, 80)


    private val vertexBuffer = arrayListOf<Float>()


    private fun addVertex(position: Vector2, color: Color)
    {
        vertexBuffer.add(position.x)
        vertexBuffer.add(position.y)
        vertexBuffer.add(color.toFloatBits())
    }

    private fun addSegmentQuads(segment: Line, segmentLeft: Line, segmentRight: Line)
    {
        // Each segment of the path is actually rendered as 2 quads, being split in half along the
        // approximating line.

        // FIXME Slider vertices overlap.
        // On this line the depth is 1 instead of 0, which is done in order to properly handle
        // self-overlap using the depth buffer.
        val firstMiddlePoint = Vector2(segment.startPoint.x, segment.startPoint.y)
        val secondMiddlePoint = Vector2(segment.endPoint.x, segment.endPoint.y)

        // Each of the quads (mentioned above) is rendered as 2 triangles:

        // Outer quad, triangle 1
        addVertex(
            position = Vector2(segmentRight.endPoint.x, segmentRight.endPoint.y),
            color = outerColor
        )
        addVertex(
            position = Vector2(segmentRight.startPoint.x, segmentRight.startPoint.y),
            color = outerColor
        )
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )

        // Outer quad, triangle 2
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = secondMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = Vector2(segmentRight.endPoint.x, segmentRight.endPoint.y),
            color = outerColor
        )

        // Inner quad, triangle 1
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = secondMiddlePoint,
            color = innerColor
        )
        addVertex(
            position = Vector2(segmentLeft.endPoint.x, segmentLeft.endPoint.y),
            color = outerColor
        )

        // Inner quad, triangle 2
        addVertex(
            position = Vector2(segmentLeft.endPoint.x, segmentLeft.endPoint.y),
            color = outerColor
        )
        addVertex(
            position = Vector2(segmentLeft.startPoint.x, segmentLeft.startPoint.y),
            color = outerColor
        )
        addVertex(
            position = firstMiddlePoint,
            color = innerColor
        )
    }

    private fun addSegmentCaps(
        rawThetaDifference: Float,
        segmentLeft: Line,
        segmentRight: Line,
        previousSegmentLeft: Line,
        previousSegmentRight: Line
    )
    {
        val thetaDifference = if (abs(rawThetaDifference) > MathF.PI)
            -sign(rawThetaDifference) * 2f * MathF.PI + rawThetaDifference
        else
            rawThetaDifference

        if (thetaDifference == 0f)
            return

        val origin = (segmentLeft.startPoint + segmentRight.startPoint) / 2

        // Use segment end points instead of calculating start/end via theta to guarantee that the
        // vertices have the exact same position as the quads, which prevents possible pixel gaps
        // during rasterization.
        var current = if (thetaDifference > 0f) previousSegmentRight.endPoint else previousSegmentLeft.endPoint
        val end = if (thetaDifference > 0f) segmentRight.startPoint else segmentLeft.startPoint

        val start = if (thetaDifference > 0f)
            Line(previousSegmentLeft.endPoint, previousSegmentRight.endPoint)
        else
            Line(previousSegmentRight.endPoint, previousSegmentLeft.endPoint)

        val initialTheta = start.theta
        val thetaStep = sign(thetaDifference) * MathF.PI / 24
        val stepCount = ceil(thetaDifference / thetaStep).toInt()


        fun pointOnCircle(angle: Float) = Vector2(cos(angle), sin(angle))

        for (i in 1 .. stepCount)
        {
            // Center point
            addVertex(
                position = Vector2(origin.x, origin.y),
                color = innerColor
            )

            // First outer point
            addVertex(
                position = Vector2(current.x, current.y),
                color = outerColor
            )

            current = if (i >= stepCount) end else {

                origin + pointOnCircle(initialTheta + i * thetaStep) * radius / 2f
            }

            // Second outer point
            addVertex(
                position = Vector2(current.x, current.y),
                color = outerColor
            )
        }
    }

    fun drawToBuffer(): FloatArray
    {
        // The coordinate system here is flipped, "left" corresponds to positive angles (anti-clockwise)
        // and "right" corresponds to negative angles (clockwise).

        var previousSegmentLeft: Line? = null
        var previousSegmentRight: Line? = null

        for (i in 0..<segments.size)
        {
            val segment: Line = segments[i]

            var orthogonalDirection = segment.orthogonalDirection
            
            if (orthogonalDirection.x.isNaN() || orthogonalDirection.y.isNaN())
                orthogonalDirection = Vector2(0f, 1f)

            val segmentLeft = Line(
                segment.startPoint + orthogonalDirection * radius / 2f,
                segment.endPoint + orthogonalDirection * radius / 2f
            )

            val segmentRight = Line(
                segment.startPoint - orthogonalDirection * radius / 2f,
                segment.endPoint - orthogonalDirection * radius / 2f
            )

            addSegmentQuads(segment, segmentLeft, segmentRight)

            if (previousSegmentLeft != null && previousSegmentRight != null)
            {
                // Connection/filler caps between segment quads
                val thetaDifference = segment.theta - segments[i - 1].theta

                addSegmentCaps(
                    thetaDifference,
                    segmentLeft,
                    segmentRight,
                    previousSegmentLeft,
                    previousSegmentRight
                )
            }

            // Semi-circles are essentially 180 degree caps. So to create these caps, we can simply
            // "fake" a segment that's 180 degrees flipped. This works because we are taking advantage
            // of the fact that a path which makes a 180 degree bend would have a semi-circle cap.
            if (i == 0)
            {
                // Path start cap (semi-circle);
                val flippedLeft = Line(segmentRight.endPoint, segmentRight.startPoint)
                val flippedRight = Line(segmentLeft.endPoint, segmentLeft.startPoint)

                addSegmentCaps(
                    MathF.PI,
                    segmentLeft,
                    segmentRight,
                    flippedLeft,
                    flippedRight
                )
            }

            if (i == segments.lastIndex)
            {
                // Path end cap (semi-circle)
                val flippedLeft = Line(segmentRight.endPoint, segmentRight.startPoint)
                val flippedRight = Line(segmentLeft.endPoint, segmentLeft.startPoint)

                addSegmentCaps(MathF.PI, flippedLeft, flippedRight, segmentLeft, segmentRight)
            }

            previousSegmentLeft = segmentLeft
            previousSegmentRight = segmentRight
        }

        return vertexBuffer.toFloatArray()
    }
}


open class PathMesh(

    drawer: PathMeshDrawer,

    vertices: FloatArray = drawer.drawToBuffer()

) : Mesh(true, vertices.size / VERTEX_SIZE, VERTEX_SIZE, *arrayOf(
    VertexAttribute.Position(),
    VertexAttribute.ColorPacked()
))
{

    var clearDepthOnStart = false


    override fun render(
        shader: ShaderProgram?,
        primitiveType: Int,
        offset: Int,
        count: Int,
        autoBind: Boolean
    )
    {
        // There's nothing to draw.
        if (count == 0)
            return

        if (clearDepthOnStart)
            Gdx.gl.glClear(GL_DEPTH_BUFFER_BIT)

        Gdx.gl.glEnable(GL_DEPTH_TEST)

        super.render(shader, primitiveType, offset, count, autoBind)

        Gdx.gl.glDisable(GL_DEPTH_TEST)
    }


    companion object
    {
        /**
         * The number of floats per vertex.
         */
        const val VERTEX_SIZE = 3
    }
}


/**
 * An [Actor] that draws a [Mesh].
 */
open class MeshActor(

    /**
     * The mesh to draw.
     */
    val mesh: PathMesh,

    /**
     * The type of primitive to draw.
     */
    var primitiveType: Int = GL20.GL_TRIANGLES

) : Actor()
{
    override fun draw(batch: Batch, parentAlpha: Float) = mesh.render(batch.shader, primitiveType)
}