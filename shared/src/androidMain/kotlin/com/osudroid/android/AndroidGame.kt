package com.osudroid.android

import android.content.Context
import java.lang.ref.WeakReference

/**
 * The Android state of the game, which the common `OsuGame` doesn't hold.
 */
object AndroidGame {

    // A weak reference, a destroyed activity isn't kept alive by the game.
    private var mainActivityReference: WeakReference<AndroidOsuGameHost>? = null

    // The application's context, which lives as long as the process, never an activity.
    private var applicationContext: Context? = null

    /**
     * The game's activity (`MainActivity`, in `:androidApp`), null while the game runs without it
     * (e.g. in the UI test browser) or once it's destroyed.
     */
    @JvmStatic
    var mainActivity: AndroidOsuGameHost?
        get() = mainActivityReference?.get()
        set(value) {
            mainActivityReference = value?.let(::WeakReference)
        }

    /**
     * The context to reach resources, assets and preferences with: the application's. It's the main
     * activity's, or the one of the context set here while the game runs without it.
     */
    @JvmStatic
    var context: Context
        get() = mainActivity?.applicationContext ?: applicationContext ?: throw IllegalStateException("No context was set")
        set(value) {
            applicationContext = value.applicationContext
        }
}
