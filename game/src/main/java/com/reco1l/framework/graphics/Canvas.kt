package com.reco1l.framework.graphics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Path.Direction
import android.graphics.RectF
import android.view.MotionEvent


/**
 * Range from left to right bounds.
 */
val RectF.rangeX
    get() = left..right

/**
 * Range from top to bottom bounds.
 */
val RectF.rangeY
    get() = top..bottom


operator fun RectF.contains(event: MotionEvent) = event.x in rangeX && event.y in rangeY


fun Path.addRoundRect(rectF: RectF, radius: Float, direction: Direction)
{
    addRoundRect(rectF, radius, radius, direction)
}

fun Canvas.drawRoundRect(rectF: RectF, radius: Float, paint: Paint)
{
    drawRoundRect(rectF, radius, radius, paint)
}