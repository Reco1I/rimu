package com.reco1l.framework.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape

open class RoundShape : RectShape()
{

    private val radii = FloatArray(8)

    private val path = Path()


    override fun onResize(width: Float, height: Float)
    {
        super.onResize(width, height)

        path.reset()
        path.addRoundRect(rect(), radii, Direction.CW)
    }

    override fun draw(canvas: Canvas, paint: Paint) = canvas.drawPath(path, paint)


    /**
     * Apply a radius to the specified anchor or all anchors.
     *
     * @param anchor The anchor to apply the radius, if `null` is passed the radius will be applied
     * to all anchors.
     */
    fun setRadius(@CornerAnchor anchor: Int? = null, radius: Float)
    {
        if (anchor == null)
        {
            radii.fill(radius)
            return
        }

        when (anchor)
        {
            Anchor.TOP_LEFT -> radii.fill(radius, 0, 2)
            Anchor.TOP_RIGHT -> radii.fill(radius, 2, 4)
            Anchor.BOTTOM_LEFT -> radii.fill(radius, 4, 6)
            Anchor.BOTTOM_RIGHT -> radii.fill(radius, 6, 8)
        }
    }


    fun toDrawable() = ShapeDrawable(this)
}

fun ShapeDrawable.setRadius(radius: Float, @CornerAnchor anchor: Int? = null)
{
    if (shape !is RoundShape)
        shape = RoundShape()

    (shape as RoundShape).setRadius(anchor, radius)
    invalidateSelf()
}
