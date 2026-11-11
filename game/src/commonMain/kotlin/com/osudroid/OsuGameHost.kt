package com.osudroid

import com.reco1l.verktex.GameHost
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.ITextInput

/**
 * What the game needs from the host it runs in, beyond the engine's [com.reco1l.verktex.Platform].
 * Each feature defaults to what a host without it does: the game hides what opens it, or does
 * without it.
 *
 * The game's hosts implement it (Android's `MainActivity`, desktop's `OsuDesktop`, the UI test
 * browsers), and the game reaches it through [current].
 */
interface OsuGameHost : GameHost {

    /**
     * The type of the build, as Android names them: `debug`, `release`, `pre_release` or `profile`.
     */
    val buildType: String
        get() = "debug"

    /**
     * The preferences the game kept before its settings were stored by Verktex, by name, imported on
     * the first start with them.
     */
    fun legacyPreferences(): Map<String, Any?> = emptyMap()

    /**
     * Subscribes to or unsubscribes from the announcements the game receives as notifications.
     */
    fun setAnnouncementsSubscribed(subscribed: Boolean) = Unit

    /**
     * Logs an analytics event, such as `login`, with its parameters.
     */
    fun logAnalyticsEvent(name: String, parameters: Map<String, String>) = Unit

    /**
     * Imports the beatmap sets waiting to be imported (`.osz` files in the game's folders, or opened
     * with the game) and loads the library.
     */
    fun loadBeatmapLibrary() = Unit

    /**
     * Imports the skins waiting to be imported (`.osk` files in the game's folders).
     */
    fun checkNewSkins() = Unit

    /**
     * Checks a texture of a skin folder: an [optional] one is created transparent when it's missing,
     * other ones are deleted when they're empty or transparent, so the default skin's are used
     * instead. Hosts that can't read images leave them as they are.
     */
    fun checkSkinTexture(path: String, optional: Boolean) = Unit

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
     * Whether the file of the game wasn't modified. Hosts that can't tell return true.
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
     * Opens the changelog of this version.
     */
    fun openChangelog() = Unit


    companion object {

        /**
         * The host of the game, or one without any feature when there's none, e.g. in unit tests.
         */
        val current: OsuGameHost
            get() = (if (Verktex.isInitialized) Verktex.host as? OsuGameHost else null) ?: None
    }

    // A host without any feature, for code running without one.
    private object None : OsuGameHost {
        override fun onEngineInitialized() = Unit
        override fun onEngineStarted() = Unit
        override fun attachTextInput(textInput: ITextInput) = Unit
        override fun detachTextInput() = Unit
    }
}
