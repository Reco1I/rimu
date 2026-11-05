package com.reco1l.verktex.desktop.testing

import com.reco1l.verktex.testing.VerktexUITests
import java.io.File
import kotlin.system.exitProcess

/**
 * Runs Verktex's own UI tests on desktop:
 *
 * ```
 * ./gradlew :verktex:desktop:run --args="--test 'Verktex/<test>' --screenshot build/test.png"
 * ```
 *
 * `--test` opens a test first (`suite/test` or the test name), `--screenshot` saves a frame after a
 * moment and exits.
 */
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

    object : UITestApplication(test, screenshot) {
        override fun createSuites() = listOf(VerktexUITests)
    }.run()
}
