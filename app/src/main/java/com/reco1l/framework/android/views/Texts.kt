@file:JvmName("Texts")

package com.reco1l.framework.android.views

import android.graphics.Typeface
import android.util.TypedValue
import android.widget.TextView


/**
 * The current [Typeface] style.
 */
var TextView.fontStyle
    get() = typeface.style
    set(value) = setTypeface(typeface, value)

/**
 * The current font color.
 */
var TextView.fontColor: Int
    get() = currentTextColor
    set(value) = setTextColor(value)

/**
 * The current font size in pixels, see [TypedValue.COMPLEX_UNIT_PX].
 */
var TextView.fontSize: Float
    get() = textSize
    set(value) = setTextSize(TypedValue.COMPLEX_UNIT_PX, value)

/**
 * The current [Typeface].
 */
var TextView.font: Typeface
    get() = typeface
    set(value) = setTypeface(value, fontStyle)