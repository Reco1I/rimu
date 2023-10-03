@file:JvmName("Paddings")

package com.reco1l.framework.android.views

import android.view.View
import kotlin.math.roundToInt

/**
 * Change View paddings.
 */
fun View.setPaddings(
    left: Int = paddingLeft,
    top: Int = paddingTop,
    right: Int = paddingRight,
    bottom: Int = paddingBottom

) = setPadding(left, top, right, bottom)


/**
 * Change View paddings.
 */
fun View.setPaddings(
    left: Float = paddingLeft.toFloat(),
    top: Float = paddingTop.toFloat(),
    right: Float = paddingRight.toFloat(),
    bottom: Float = paddingBottom.toFloat()

) = setPaddings(
    left = left.roundToInt(),
    top = top.roundToInt(),
    right = right.roundToInt(),
    bottom = bottom.roundToInt()
)