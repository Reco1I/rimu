package com.reco1l.framework.graphics

import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity


// ClipDrawable

/**
 * Wrap a drawable inside a [ClipDrawable].
 */
fun Drawable.clip(
    gravity: Int = Gravity.LEFT,
    orientation: Int = ClipDrawable.HORIZONTAL
) = ClipDrawable(this, gravity, orientation)


// LayerDrawable

/**
 * Iterate over all layers.
 */
fun LayerDrawable.forEach(block: (Drawable) -> Unit)
{
    for (i in 0 until numberOfLayers)
        block(getDrawable(i))
}

/**
 * Improved constructor of [LayerDrawable]
 */
fun LayerDrawable(vararg layers: Drawable) = LayerDrawable(layers)


// Size

fun Drawable.setSize(width: Int = bounds.width(), height: Int = bounds.height())
{
    setBounds(0, 0, width, height)
}
