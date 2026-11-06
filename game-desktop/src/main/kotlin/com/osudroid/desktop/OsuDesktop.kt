package com.osudroid.desktop

import com.osudroid.OsuPlatform
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.ui.v2.SplashScene
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.settings.RESOLUTION_SCALE_KEY
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.async
import com.osudroid.utils.delayed
import com.reco1l.framework.net.Http
import com.reco1l.framework.net.OkHttpClientImplementation
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.DesktopGameHost
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
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
) : DesktopGameHost(title = "osu!droid", vsync = true) {

    override fun onEngineInitialized() {
        initializeDesktopPlatform()

        createGameDirectories()
        OnlineManager.getInstance().init()
        DesktopDatabase.load()

        GlobalManager.getInstance().isHeadless = false
    }

    override fun onEngineStarted() {
        Verktex.pushScene(SplashScene)

        if (screenshot != null) {
            captureScreenshot(screenshot, screenshotDelaySeconds)
        }

        async {
            GlobalManager.getInstance().init()

            // The "Show FPS" setting shows the engine's performance overlay.
            Settings[VerktexSettings.ShowPerformanceOverlay] = Config.isShowFPS()

            // The resolution scale setting is stored as a percentage.
            Settings[VerktexSettings.ResolutionScale] = Config.getInt(RESOLUTION_SCALE_KEY, 100) / 100f

            GlobalManager.getInstance().loadingProgress = 50
            Config.loadSkins()
            DifficultyCalculationManager.checkForOutdatedStarRatings()
            OsuPlatform.implementation.loadBeatmapLibrary()

            SplashScene.playWelcomeAnimation()

            delayed(2500) {
                GlobalManager.getInstance().info = ""
                GlobalManager.getInstance().loadingProgress = 100
                Verktex.pushScene(MainScene)
                SongSelect.loadBeatmaps()
            }
        }
    }

    private fun createGameDirectories() {
        File(Config.getBeatmapPath()).mkdirs()
        File(Config.getSkinTopPath()).mkdirs()
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
 * Sets what the game needs from desktop, before anything of the game is used.
 */
fun initializeDesktopPlatform() {
    OsuPlatform.implementation = DesktopOsuPlatform()
    Http.implementation = OkHttpClientImplementation()
    Config.loadConfig()

}

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
}
