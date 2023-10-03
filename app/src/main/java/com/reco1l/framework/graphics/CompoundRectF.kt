package com.reco1l.framework.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class CompoundRectF(var paint: Paint = Paint()) : RectF()
{

    /**
     * Draw the [RectF] and [Paint] into the specified canvas.
     *
     * @param radiusX The corner radius factor for X, by default `0`.
     * @param radiusY The corner radius factor for Y, by default same value as [radiusX] or `0`.
     */
    fun drawTo(canvas: Canvas, radiusX: Float = 0f, radiusY: Float = radiusX)
    {
        canvas.drawRoundRect(this, radiusX, radiusY, paint)
    }

}