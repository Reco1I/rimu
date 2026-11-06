package com.osudroid.desktop

import com.osudroid.OsuGame
import ru.nsu.ccfit.zuev.osu.LibraryManager
import java.util.prefs.Preferences
import com.reco1l.verktex.Platform
import com.osudroid.OsuGameHost
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.ui.v2.SplashScene
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.async
import com.osudroid.utils.delayed
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.DesktopGameHost
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import java.io.File

/**
 * The game's window on desktop, which starts the game as `MainActivity` does on Android.
 *
 * @param screenshot When set, the frame shown [screenshotDelaySeconds] after starting is saved to this
 * PNG and the window closes, e.g. for smoke checks.
 */
class OsuDesktop(
    private val screenshot: File? = null,
    private val screenshotDelaySeconds: Double = 10.0,
) : DesktopGameHost(title = "osu!droid", vsync = true), OsuGameHost {

    override val application = DESKTOP_APPLICATION

    // The values are kept as strings, which the settings read as their type.
    override fun legacyPreferences(): Map<String, Any?> {
        val node = Preferences.userRoot().node("osu!droid")
        return node.keys().associateWith { node.get(it, null) }
    }

    // Beatmap sets are imported from their folders only, .osz files aren't extracted yet.
    override fun loadBeatmapLibrary() {
        LibraryManager.scanDirectory(emptySet())
        LibraryManager.loadLibrary()
    }

    override fun onEngineInitialized() {
        OsuSettings.load()

        createGameDirectories()
        OnlineManager.getInstance().init()
        DesktopDatabase.load()

        OsuGame.isHeadless = false
    }

    override fun onEngineStarted() {
        Verktex.pushScene(SplashScene)

        if (screenshot != null) {
            captureScreenshot(screenshot, screenshotDelaySeconds)
        }

        async {
            OsuGame.init()

            // The "Show FPS" setting shows the engine's performance overlay.
            Settings[VerktexSettings.ShowPerformanceOverlay] = Settings[OsuSettings.ShowFps]

            // The resolution scale setting is stored as a percentage.
            Settings[VerktexSettings.ResolutionScale] = Settings[OsuSettings.ResolutionScale] / 100f

            SplashScene.progress = 50
            SkinFolders.load()
            DifficultyCalculationManager.checkForOutdatedStarRatings()
            OsuGameHost.current.loadBeatmapLibrary()

            SplashScene.playWelcomeAnimation()

            delayed(2500) {
                SplashScene.info = ""
                SplashScene.progress = 100
                Verktex.pushScene(MainScene)
                SongSelect.loadBeatmaps()
            }
        }
    }

    private fun createGameDirectories() {
        File(GameFolders.beatmapPath).mkdirs()
        File(SkinFolders.topPath).mkdirs()
    }
}

/**
 * Starts the game on desktop:
 *
 * ```
 * ./gradlew :game-desktop:run --args="--screenshot build/game.png --delay 10"
 * ```
 *
 * `--screenshot` saves the frame shown `--delay` seconds (10 by default) after starting and exits.
 */
/**
 * The game on desktop: a debug build, without a version of its own.
 */
internal val DESKTOP_APPLICATION = Platform.Application(versionName = "desktop", isDebug = true)

fun main(args: Array<String>) {
    var screenshot: File? = null
    var delay = 10.0

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--screenshot" -> screenshot = args.getOrNull(++i)?.let(::File)
            "--delay" -> delay = args.getOrNull(++i)?.toDoubleOrNull() ?: delay
            else -> {
                System.err.println("Unknown argument: ${args[i]}. Usage: [--screenshot <file.png>] [--delay <seconds>]")
                kotlin.system.exitProcess(2)
            }
        }
        i++
    }

    OsuDesktop(screenshot, delay).run()

    // Idle connection pools would keep the process alive for a while.
    kotlin.system.exitProcess(0)
}
