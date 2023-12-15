package com.reco1l.rimu.ui.views.addons

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import kotlin.math.min

/**
 * Applies a round outline to the view.
 */
class RoundOutlineProvider : ViewOutlineProvider()
{

    /**
     * Horizontal outline offset.
     *
     * Note: You must [invalidate][View.invalidateOutline] the view outline in order to take effect on this.
     */
    var offsetX = 0

    /**
     * Vertical outline offset.
     *
     * Note: You must [invalidate][View.invalidateOutline] the view outline in order to take effect on this.
     */
    var offsetY = 0

    /**
     * The corner radius.
     *
     * Note: You must [invalidate][View.invalidateOutline] the view outline in order to take effect on this.
     */
    var radius: Float = 18f


    override fun getOutline(view: View, outline: Outline)
    {
        // Clipping view to outline, without this the rounding will not take effect.
        view.clipToOutline = true

        val width = view.width
        val height = view.height

        // This is a workaround for older devices without Skia support where if the radius is greater
        // than any of the view bounds it'll cause an unexpected visual.
        val radius = radius.coerceIn(0f, min(width, height) / 2f)

        // Applying corner radius to outline.
        outline.setRoundRect(0, 0, width, height, radius)

        // Applying offset to outline.
        outline.offset(offsetX, offsetY)
    }
}
