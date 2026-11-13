package com.osudroid.desktop

import com.osudroid.settings.OsuSettings
import com.osudroid.OsuGameHost
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.ui.v2.Skin
import com.osudroid.ui.v2.test.OsuGameplayUITests
import com.osudroid.ui.v2.test.OsuSceneUITests
import com.osudroid.ui.v2.test.OsuUITests
import com.osudroid.ui.v2.test.TestGameContext
import com.reco1l.verktex.desktop.testing.UITestApplication
import com.reco1l.verktex.testing.VerktexUITests
import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

/**
 * The UI test browser of osu!droid on desktop, the counterpart of Android's `OsuUITestActivity`:
 *
 * ```
 * ./gradlew :desktopApp:runUITests --args="--test 'osu!/Hit circles' --screenshot build/test.png --delay 3"
 * ```
 */
class OsuUITestsDesktop(
    initialTest: String?,
    screenshot: File?,
    screenshotDelay: Double,
) : UITestApplication(initialTest, screenshot, screenshotDelay = screenshotDelay), OsuGameHost {

    override val platformApplication = DESKTOP_APPLICATION

    override fun onEngineInitialized() {
        OsuSettings.load()
        Skin.load(SkinFolders.path)

        // Before any suite is created, the components under test reach the game through it.
        // Before the suites are created, which read the database it seeds.
        runBlocking { TestGameContext.install() }
    }

    override fun createSuites() = listOf(VerktexUITests, OsuUITests, OsuSceneUITests, OsuGameplayUITests)
}

fun main(args: Array<String>) {
    var test: String? = null
    var screenshot: File? = null
    var delay = UITestApplication.SCREENSHOT_DELAY_SECONDS

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--test" -> test = args.getOrNull(++i)
            "--screenshot" -> screenshot = args.getOrNull(++i)?.let(::File)
            "--delay" -> delay = args.getOrNull(++i)?.toDoubleOrNull() ?: delay
            else -> {
                System.err.println("Unknown argument: ${args[i]}. Usage: [--test <suite/test>] [--screenshot <file.png> [--delay <seconds>]]")
                exitProcess(2)
            }
        }
        i++
    }

    OsuUITestsDesktop(test, screenshot, delay).run()
}
