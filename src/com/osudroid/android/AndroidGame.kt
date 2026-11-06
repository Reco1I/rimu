package com.osudroid.android

import android.content.Context
import ru.nsu.ccfit.zuev.osu.MainActivity

/**
 * The Android state of the game, which the common `OsuGame` doesn't hold.
 */
object AndroidGame {

    /**
     * The game's activity, null while the game runs without it (e.g. in the UI test browser).
     */
    @JvmStatic
    var mainActivity: MainActivity? = null

    private var fallbackContext: Context? = null

    /**
     * The context to reach resources, assets and preferences with. It's the main activity, or the
     * context set here while the game runs without it.
     */
    @JvmStatic
    var context: Context
        get() = mainActivity ?: fallbackContext ?: throw IllegalStateException("No context was set")
        set(value) {
            fallbackContext = value
        }
}
