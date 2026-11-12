package com.osudroid.desktop

import com.osudroid.OsuGame
import com.osudroid.OsuGameHost
import com.osudroid.beatmaps.LibraryImport
import com.osudroid.debug.QuickPlay
import com.reco1l.verktex.PlatformApplication
import com.reco1l.verktex.desktop.DesktopGameHost
import java.io.File

/**
 * The game's window on desktop, which starts the game ([OsuGame.start]).
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

    override val platformApplication = DESKTOP_APPLICATION

    override suspend fun loadBeatmapLibrary() = LibraryImport.loadLibrary()

    override fun checkNewSkins() = LibraryImport.importSkins()

    override fun onEngineInitialized() = OsuGame.start()

    override fun onEngineStarted() {
        if (screenshot != null) {
            captureScreenshot(screenshot, screenshotDelaySeconds)
        }
    }

    override fun onGameStarted() {
        // TEMPORARY: debug shortcut straight into gameplay.
        quickPlay?.let { QuickPlay.start(it, quickPlayAutoplay) }
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
internal val DESKTOP_APPLICATION = PlatformApplication(versionName = "desktop", buildType = "debug", isDebug = true)

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
