package com.reco1l.verktex.desktop.testing

import com.reco1l.verktex.testing.VerktexUITests
import java.io.File
import kotlin.system.exitProcess

/**
 * Runs Verktex's own UI tests on desktop:
 *
 * ```
 * ./gradlew :verktex:run --args="--test 'Verktex/<test>' --screenshot build/test.png"
 * ```
 *
 * `--test` opens a test first (`suite/test` or the test name), `--screenshot` saves a frame after a
 * moment and exits, `--vsync` caps the frame rate to the display's refresh rate.
 */
fun main(args: Array<String>) {
    var test: String? = null
    var screenshot: File? = null
    var vsync = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--test" -> test = args.getOrNull(++i)
            "--screenshot" -> screenshot = args.getOrNull(++i)?.let(::File)
            "--vsync" -> vsync = true
            else -> {
                System.err.println("Unknown argument: ${args[i]}. Usage: [--test <suite/test>] [--screenshot <file.png>] [--vsync]")
                exitProcess(2)
            }
        }
        i++
    }

    object : UITestApplication(test, screenshot, vsync) {
        override fun createSuites() = listOf(VerktexUITests)
    }.run()
}
