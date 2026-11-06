package com.osudroid.desktop

import com.osudroid.OsuPlatform
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.russhwolf.settings.PreferencesSettings
import ru.nsu.ccfit.zuev.osu.LibraryManager
import java.io.File
import java.util.prefs.Preferences

/**
 * The game's [OsuPlatform] on desktop. The features only Android has keep their defaults, so the
 * game doesn't offer them.
 */
class DesktopOsuPlatform : OsuPlatform.Implementation {

    override val buildType = "debug"

    override val versionName = "desktop"

    override val isDebug = true

    override val preferences = PreferencesSettings(Preferences.userRoot().node("osu!droid"))

    override val displayWidth: Int
        get() = Platform.currentDisplay.width

    override val displayHeight: Int
        get() = Platform.currentDisplay.height

    override val isInForeground = true


    override fun setAnnouncementsSubscribed(subscribed: Boolean) = Unit

    // There are no platform views on desktop, what the game runs on the main thread goes to the
    // update thread with the rest of the UI.
    override fun runOnMainThread(block: () -> Unit) {
        Verktex.scheduleOnUpdate(block)
    }

    override fun handleUncaughtException(throwable: Throwable) {
        Logger.e("Execution", "Uncaught exception in a task", throwable)
    }

    override fun logAnalyticsEvent(name: String, parameters: Map<String, String>) = Unit

    // Beatmap sets are imported from their folders only, .osz files aren't extracted yet.
    override fun loadBeatmapLibrary() {
        LibraryManager.scanDirectory(emptySet())
        LibraryManager.loadLibrary()
    }

    override fun exitGame() {
        Verktex.dispose()
        kotlin.system.exitProcess(0)
    }
}
