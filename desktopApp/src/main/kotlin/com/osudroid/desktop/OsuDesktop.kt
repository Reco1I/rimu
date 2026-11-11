package com.osudroid.desktop

import com.osudroid.OsuGame
import ru.nsu.ccfit.zuev.osu.LibraryManager
import com.reco1l.verktex.PlatformApplication
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
import kotlinx.coroutines.runBlocking
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.DesktopGameHost
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import java.io.File
import com.osudroid.debug.QuickPlay

/**
 * The game's window on desktop, which starts the game as `MainActivity` does on Android.
 *
 * @param screenshot When set, the frame shown [screenshotDelaySeconds] after starting is saved to this
 * PNG and the window closes, e.g. for smoke checks.
 * @param quickPlay When set, the beatmap it matches is played once the game has started, see
 * [QuickPlay], with autoplay unless [quickPlayAutoplay] is false.
 */
class OsuDesktop(
    private val screenshot: File? = null,
    private val screenshotDelaySeconds: Double = 10.0,
    private val quickPlay: String? = null,
    private val quickPlayAutoplay: Boolean = true,
) : DesktopGameHost(title = "osu!droid", vsync = true), OsuGameHost {

    override val application = DESKTOP_APPLICATION

    // Beatmap sets are imported from their folders only, .osz files aren't extracted yet.
    override fun loadBeatmapLibrary() {
        // It runs on a background thread, which waits for the scan.
        runBlocking { LibraryManager.scanDirectory(emptySet()) }
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
        val splashScene = SplashScene()
        Verktex.pushScene(splashScene)

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

            splashScene.playWelcomeAnimation()

            delayed(2500) {
                SplashScene.info = ""
                SplashScene.progress = 100
                // The splash can't be gone back to.
                Verktex.resetScenes(MainScene())

                // TEMPORARY: debug shortcut straight into gameplay.
                quickPlay?.let { QuickPlay.start(it, quickPlayAutoplay) }
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
 * ./gradlew :desktopApp:run --args="--screenshot build/game.png --delay 10"
 * ./gradlew :desktopApp:run --args="--quickplay 'abracadabra normal' --no-autoplay"
 * ```
 *
 * `--screenshot` saves the frame shown `--delay` seconds (10 by default) after starting and exits.
 * `--quickplay` plays the beatmap that matches its words once the game has started, with autoplay
 * unless `--no-autoplay` is given, see [QuickPlay].
 */
/**
 * The game on desktop: a debug build, without a version of its own.
 */
internal val DESKTOP_APPLICATION = PlatformApplication(versionName = "desktop", isDebug = true)

fun main(args: Array<String>) {
    var screenshot: File? = null
    var delay = 10.0
    var quickPlay: String? = null
    var quickPlayAutoplay = true

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--screenshot" -> screenshot = args.getOrNull(++i)?.let(::File)
            "--delay" -> delay = args.getOrNull(++i)?.toDoubleOrNull() ?: delay
            "--quickplay" -> quickPlay = args.getOrNull(++i).orEmpty()
            "--no-autoplay" -> quickPlayAutoplay = false
            else -> {
                System.err.println(
                    "Unknown argument: ${args[i]}. Usage: [--screenshot <file.png>] [--delay <seconds>] " +
                        "[--quickplay <beatmap words>] [--no-autoplay]"
                )
                kotlin.system.exitProcess(2)
            }
        }
        i++
    }

    OsuDesktop(screenshot, delay, quickPlay, quickPlayAutoplay).run()

    // Idle connection pools would keep the process alive for a while.
    kotlin.system.exitProcess(0)
}
