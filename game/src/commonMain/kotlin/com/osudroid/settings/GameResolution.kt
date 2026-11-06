package com.osudroid.settings

import com.reco1l.verktex.Platform
import com.reco1l.verktex.Logger
import com.reco1l.verktex.settings.Settings
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * The size of the game's viewport in its own pixels, landscape, from the size of the display.
 */
object GameResolution {

    /**
     * The width of the viewport.
     */
    @JvmStatic
    var width = 0
        private set

    /**
     * The height of the viewport.
     */
    @JvmStatic
    var height = 0
        private set


    /**
     * Measures the viewport from the display, see [OsuSettings.UseLegacyResolutionPolicy].
     */
    @JvmStatic
    fun measure() {
        val displayWidth = Platform.currentDisplay.width
        val displayHeight = Platform.currentDisplay.height

        val longSide = max(displayWidth, displayHeight)
        val shortSide = min(displayWidth, displayHeight)

        if (Settings[OsuSettings.UseLegacyResolutionPolicy]) {
            // Tries to emulate the original behavior, the game was designed for 1280x720
            // resolution, so we try to approximate the scale factor.
            val ratio = 1280f / longSide

            width = (longSide * ratio).toInt()
            height = (shortSide * ratio).toInt()
        } else {
            width = longSide
            height = shortSide
        }

        Logger.i("GameResolution", "Viewport size: ${width}x$height Display size: ${longSide}x$shortSide")
    }
}
