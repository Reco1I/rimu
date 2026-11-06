package com.osudroid

import com.osudroid.ui.v2.UIVideo
import com.russhwolf.settings.Settings
import ru.nsu.ccfit.zuev.osu.ToastLogger

/**
 * What the game needs from the platform it runs on that the engine doesn't provide. Each platform
 * sets [implementation] before the game starts.
 */
object OsuPlatform {

    /**
     * The article that helps players who can't log in.
     */
    const val LOGIN_FAIL_HELP_URL = "https://neroyuki.github.io/osudroid-guide/help/login_fail"

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
         * The name of the version of the game, such as `1.8.2`.
         */
        val versionName: String

        /**
         * Whether this is a debug build.
         */
        val isDebug: Boolean

        /**
         * The user's preferences.
         */
        val preferences: Settings

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
         * Logs an analytics event, such as `login`, with its parameters.
         */
        fun logAnalyticsEvent(name: String, parameters: Map<String, String>)

        /**
         * Whether the game is in the foreground, visible to the player.
         */
        val isInForeground: Boolean


        /**
         * Imports the beatmap sets waiting to be imported (`.osz` files in the game's folders, or
         * opened with the game) and loads the library.
         */
        fun loadBeatmapLibrary()

        // The features below aren't on every platform. Their defaults are what a platform without
        // them does: the game hides what opens them, or does without them.

        /**
         * Imports the skins waiting to be imported (`.osk` files in the game's folders).
         */
        fun checkNewSkins() = Unit

        /**
         * Closes the game.
         */
        fun exitGame() = Unit

        /**
         * Checks a texture of a skin folder: an [optional] one is created transparent when it's
         * missing, other ones are deleted when they're empty or transparent, so the default skin's
         * are used instead. Platforms that can't read images leave them as they are.
         */
        fun checkSkinTexture(path: String, optional: Boolean) = Unit

        /**
         * Tells the player that the game couldn't connect to the server to log in.
         */
        fun showConnectionFailure() {
            ToastLogger.showText("Cannot connect to server. Please check $LOGIN_FAIL_HELP_URL for troubleshooting.", true)
        }

        /**
         * Whether [showBeatmapListing] and [downloadBeatmapSet] are available.
         */
        val hasBeatmapListing: Boolean
            get() = false

        /**
         * Shows the listing to search and download beatmaps.
         */
        fun showBeatmapListing() = Unit

        /**
         * Downloads and imports the beatmap set with [setId], saving it as [filename].
         */
        fun downloadBeatmapSet(setId: Long, filename: String) = Unit

        /**
         * Whether [pickAndImportReplays] is available.
         */
        val canImportReplays: Boolean
            get() = false

        /**
         * Lets the player pick replay files or replay packs and imports them.
         */
        fun pickAndImportReplays() = Unit

        /**
         * Whether [showBlockAreaEditor] is available.
         */
        val hasBlockAreaEditor: Boolean
            get() = false

        /**
         * Shows the editor of the areas of the screen that ignore touches in gameplay.
         */
        fun showBlockAreaEditor() = Unit

        /**
         * Shows the progress of a task running in the background, such as the difficulty calculation,
         * or returns null where the platform doesn't show it.
         */
        fun showProgressBadge(header: String, message: String): ProgressBadge? = null

        /**
         * Creates the video of the file at [path], or returns null where videos can't be played.
         */
        fun createVideo(path: String): UIVideo? = null

        /**
         * The vibrations of gameplay, or null where the device can't vibrate.
         */
        val vibrations: GameplayVibrations?
            get() = null

        /**
         * Whether the file of the game wasn't modified. Platforms that can't tell return true.
         */
        fun verifyFileIntegrity(): Boolean = true

        /**
         * Whether [checkForUpdates] is available.
         */
        val canCheckForUpdates: Boolean
            get() = false

        /**
         * Looks for a newer version of the game, telling the player only about a new one if [silently].
         */
        fun checkForUpdates(silently: Boolean) = Unit

        /**
         * Whether [exportPreferences] and [importPreferences] are available.
         */
        val canBackUpPreferences: Boolean
            get() = false

        /**
         * Saves the preferences to a file, returning whether it succeeded.
         */
        fun exportPreferences(): Boolean = false

        /**
         * Restores the preferences saved by [exportPreferences], returning whether it succeeded.
         */
        fun importPreferences(): Boolean = false

        /**
         * Whether the player can choose the language of the game, see [setLanguage].
         */
        val canChooseLanguage: Boolean
            get() = false

        /**
         * Changes the language of the game to the one with [tag] (such as `es`), restarting it if needed.
         */
        fun setLanguage(tag: String) = Unit

        /**
         * Copies [text] to the clipboard, under [label] where the platform names its entries.
         */
        fun copyToClipboard(label: String, text: String) = Unit

        /**
         * Opens the changelog of this version.
         */
        fun openChangelog() = Unit

        /**
         * Whether the device is a low end one, which gets lighter effects.
         */
        val isLowEndDevice: Boolean
            get() = false

        /**
         * Whether the display can be forced to its highest refresh rate, see [applyRefreshRateSetting].
         */
        val canForceMaxRefreshRate: Boolean
            get() = false

        /**
         * Applies the setting that forces the display to its highest refresh rate.
         */
        fun applyRefreshRateSetting(enabled: Boolean) = Unit

        /**
         * Whether the experimental Vulkan graphics backend can be chosen.
         */
        val isVulkanAvailable: Boolean
            get() = false

        /**
         * Lets the Vulkan backend start again after it crashed while starting.
         */
        fun resetVulkanCrashGuard() = Unit
    }


    /**
     * The progress of a task running in the background, see [Implementation.showProgressBadge].
     */
    interface ProgressBadge {
        var header: String
        var message: String
        var isIndeterminate: Boolean

        /**
         * The progress, from 0 to 100.
         */
        var progress: Int

        fun dismiss()
    }

    /**
     * The vibrations of gameplay, see [Implementation.vibrations].
     */
    interface GameplayVibrations {
        var isCircleVibrationEnabled: Boolean
        var isSliderVibrationEnabled: Boolean
        var isSpinnerVibrationEnabled: Boolean

        /**
         * The strength of the vibrations, from 1 to 255.
         */
        var intensity: Int

        fun circleVibration()
        fun sliderVibration()
        fun spinnerVibration()
    }
}
