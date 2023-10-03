@file:JvmName("Views")

package com.reco1l.framework.android.views

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.*
import game.rimu.ui.views.addons.RoundOutlineProvider
import kotlin.math.roundToInt


// Radius

var View.radius: Float
    /**
     * Return the view corner radius.
     */
    get() = (outlineProvider as? RoundOutlineProvider)?.radius ?: 0f
    /**
     * Set the View corner radius.
     *
     * Note: This will replace any previous outline provider with a [RoundOutlineProvider].
     */
    set(value)
    {
        var provider = outlineProvider as? RoundOutlineProvider

        if (value <= 0 && provider == null)
            return

        if (provider == null)
        {
            provider = RoundOutlineProvider()
            outlineProvider = provider
        }

        provider.radius = value
        invalidateOutline()
    }


// Position

/**
 * @see[View.getLocationInWindow]
 */
val View.absolutePosition: IntArray
    get() = IntArray(2).apply { getLocationInWindow(this) }


// Safe properties

/**
 * Ensures that the view has layout params, if not a layout params with [WRAP_CONTENT] as
 * dimensions is set.
 */
fun View.ensureLayoutParams()
{
    if (layoutParams == null)
        layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
}

/**
 * Ensures that the view has an ID, if not a generated ID is set.
 */
fun View.ensureID()
{
    if (id == NO_ID)
        id = View.generateViewId()
}


// Size

fun View.setSize(width: Float? = null, height: Float? = null) = setSize(
    width = width?.roundToInt(),
    height = height?.roundToInt()
)

/**
 * Set the width and height of the view with defaults.
 */
fun View.setSize(width: Int? = null, height: Int? = null)
{
    ensureLayoutParams()

    layoutParams.apply {

        if (width != null)
            this.width = width

        if (height != null)
            this.height = height

        requestLayout()
    }
}


// Attachment

infix fun <T : View> T.attachTo(parent: ViewGroup): T
{
    parent.addView(this)
    return this
}

/**
 * Remove self from a parent.
 */
fun View.removeSelf()
{
    (parent as? ViewGroup)?.removeView(this)
}