package com.reco1l.verktex.math

import com.reco1l.verktex.data.Color4

/**
 * A collection of color interpolation functions.
 */
object ColorInterpolation {

    /**
     * Interpolates between two sRGB colors in a linear (gamma-correct) RGB space.
     *
     * [Information regarding linear interpolation](https://blog.johnnovak.net/2016/09/21/what-every-coder-should-know-about-gamma/#gradients)
     */
    fun colorAt(time: Float, startColour: Color4, endColour: Color4, startTime: Float, endTime: Float, easing: Easing = Easing.None): Color4 {

        if (startColour == endColour) {
            return startColour
        }

        val current = time - startTime
        val duration = endTime - startTime

        if (duration == 0f || current == 0f) {
            return startColour
        }

        val t = easing.interpolate(current / duration).coerceIn(0f, 1f)

        return Color4(
            startColour.red + t * (endColour.red - startColour.red),
            startColour.green + t * (endColour.green - startColour.green),
            startColour.blue + t * (endColour.blue - startColour.blue),
            startColour.alpha + t * (endColour.alpha - startColour.alpha)
        ).toSRGB()
    }

}