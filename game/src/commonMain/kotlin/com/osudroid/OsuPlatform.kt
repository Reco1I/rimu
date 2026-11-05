package com.osudroid

import com.russhwolf.settings.Settings

/**
 * What the game needs from the platform it runs on that the engine doesn't provide. Each platform
 * sets [implementation] before the game starts.
 */
object OsuPlatform {

    /**
     * The implementation of the current platform.
     */
    lateinit var implementation: Implementation

    /**
     * Whether a platform has set the [implementation], which unit tests don't.
     */
    val isInitialized
        get() = ::implementation.isInitialized


    interface Implementation {

        /**
         * The type of the build, as Android names them: `debug`, `release`, `pre_release` or
         * `profile`.
         */
        val buildType: String

        /**
         * Whether this is a debug build.
         */
        val isDebug: Boolean

        /**
         * The user's preferences.
         */
        val preferences: Settings

        /**
         * The folder where the game keeps its files by default, ending with a separator.
         */
        val defaultCorePath: String

        /**
         * The folder where the game keeps its cache.
         */
        val cachePath: String

        /**
         * The width of the display in pixels, including the space the system decorations take.
         */
        val displayWidth: Int

        /**
         * The height of the display in pixels, including the space the system decorations take.
         */
        val displayHeight: Int

        /**
         * Subscribes to or unsubscribes from the announcements the game receives as notifications.
         */
        fun setAnnouncementsSubscribed(subscribed: Boolean)

        /**
         * Runs [block] on the platform's main thread, right away if called from it.
         */
        fun runOnMainThread(block: () -> Unit)

        /**
         * Handles an exception that a task running in the background didn't catch.
         */
        fun handleUncaughtException(throwable: Throwable)

        /**
         * Whether the game is in the foreground, visible to the player.
         */
        val isInForeground: Boolean
    }
}
