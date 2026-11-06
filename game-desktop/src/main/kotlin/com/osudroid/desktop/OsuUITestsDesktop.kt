package com.osudroid.desktop

import com.osudroid.ui.v2.Skin
import com.osudroid.ui.v2.test.OsuGameplayUITests
import com.osudroid.ui.v2.test.OsuSceneUITests
import com.osudroid.ui.v2.test.OsuUITests
import com.osudroid.ui.v2.test.TestGameContext
import com.reco1l.verktex.desktop.testing.UITestApplication
import com.reco1l.verktex.testing.VerktexUITests
import ru.nsu.ccfit.zuev.osu.Config
import java.io.File
import kotlin.system.exitProcess

/**
 * The UI test browser of osu!droid on desktop, the counterpart of Android's `OsuUITestActivity`:
 *
 * ```
 * ./gradlew :game-desktop:runUITests --args="--test 'osu!/Hit circles' --screenshot build/test.png"
 * ```
 */
class OsuUITestsDesktop(initialTest: String?, screenshot: File?) : UITestApplication(initialTest, screenshot) {

    override fun onEngineInitialized() {
        initializeDesktopPlatform()
        Skin.load(Config.getSkinPath())

        // Before any suite is created, the components under test reach the game through it.
        TestGameContext.install { DesktopDatabase.loadInMemory() }
    }

    override fun createSuites() = listOf(VerktexUITests, OsuUITests, OsuSceneUITests, OsuGameplayUITests)
}

fun main(args: Array<String>) {
    var test: String? = null
    var screenshot: File? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--test" -> test = args.getOrNull(++i)
            "--screenshot" -> screenshot = args.getOrNull(++i)?.let(::File)
            else -> {
                System.err.println("Unknown argument: ${args[i]}. Usage: [--test <suite/test>] [--screenshot <file.png>]")
                exitProcess(2)
            }
        }
        i++
    }

    OsuUITestsDesktop(test, screenshot).run()
}
