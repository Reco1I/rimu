package com.reco1l.rimu.graphics

import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.math.Vector2
import kotlin.math.atan2

data class Line(
    /**
     * Begin point of the line.
     */
    val startPoint: Vector2,

    /**
     * End point of the line.
     */
    val endPoint: Vector2
) {

    /**
     * The direction of the second point from the first.
     */
    val theta: Float
        get() = atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)

    /**
     * The direction of this line.
     */
    val direction: Vector2
        get() = endPoint - startPoint

    /**
     * The normalized direction of this line.
     */
    val directionNormalized: Vector2
        get() = direction.copy().apply { normalize() }

    /**
     * Orthogonal direction of this line.
     */
    val orthogonalDirection: Vector2
        get()
        {
            val dir = directionNormalized
            return Vector2(-dir.y, dir.x)
        }

    /**
     * Computes a position along this line.
     * @param t A parameter representing the position along the line to compute. 0 yields the start point and 1 yields the end point.
     * @return The position along the line.
     */
    fun at(t: Float) = Vector2(startPoint.x + (endPoint.x - startPoint.x) * t, startPoint.y + (endPoint.y - startPoint.y) * t)

}


fun SliderPath.getSegments() = mutableListOf<Line>().apply {

    val vertices = calculatedPath

    if (vertices.size > 1)
    {
        for (i in 0 ..< vertices.size - 1)
            add(Line(vertices[i], vertices[i + 1]))
    }
}