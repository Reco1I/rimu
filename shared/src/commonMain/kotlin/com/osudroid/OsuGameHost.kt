package com.osudroid

import com.reco1l.verktex.GameHost
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.ITextInput

/**
 * What the game needs from the host it runs in, beyond the engine's [com.reco1l.verktex.Platform].
 * Each feature defaults to what a host without it does: the game hides what opens it, or does
 * without it.
 *
 * The game's hosts implement it (Android's `AndroidOsuGameHost`, desktop's `OsuDesktop`, the UI test
 * browsers), and the game reaches it through [current].
 */
interface OsuGameHost : GameHost {

    /**
     * The type of the build, as Android names them: `debug`, `release`, `pre_release` or `profile`.
     */
    val buildType: String
        get() = "debug"

    /**
     * Subscribes to or unsubscribes from the announcements the game receives as notifications.
     */
    fun setAnnouncementsSubscribed(subscribed: Boolean) = Unit

    /**
     * Logs an analytics event, such as `login`, with its parameters.
     */
    fun logAnalyticsEvent(name: String, parameters: Map<String, String>) = Unit

    /**
     * Opens the game's database, in the background while the game starts ([OsuGame.start]).
     */
    fun openDatabase() = Unit

    /**
     * Called once the game has started and shows the main menu, for what the host does then, such as
     * playing a file the game was opened with.
     */
    fun onGameStarted() = Unit

    /**
     * Imports the beatmap sets waiting to be imported (`.osz` files in the game's folders, or opened
     * with the game) and loads the library.
     */
    suspend fun loadBeatmapLibrary() = Unit

    /**
     * Imports the skins waiting to be imported (`.osk` files in the game's folders).
     */
    fun checkNewSkins() = Unit

    /**
     * The folder the device's downloads are saved in, where [com.osudroid.beatmaps.LibraryImport] also
     * looks for archives, `null` if there's none.
     */
    val downloadsFolder: String?
        get() = null

    /**
     * Checks a texture of a skin folder: an [optional] one is created transparent when it's missing,
     * other ones are deleted when they're empty or transparent, so the default skin's are used
     * instead. Hosts that can't read images leave them as they are.
     */
    fun checkSkinTexture(path: String, optional: Boolean) = Unit

    /**
     * Whether [showBeatmapListing] is available.
     */
    val hasBeatmapListing: Boolean
        get() = false

    /**
     * Shows the listing to search and download beatmaps.
     */
    fun showBeatmapListing() = Unit

    /**
     * Whether [downloadBeatmapSet] is available.
     */
    val canDownloadBeatmapSets: Boolean
        get() = false

    /**
     * Downloads and imports the beatmap set with [setId], saving it as [filename].
     */
    fun downloadBeatmapSet(setId: Long, filename: String) = Unit

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
