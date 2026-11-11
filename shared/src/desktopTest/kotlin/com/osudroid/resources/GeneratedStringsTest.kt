package com.osudroid.resources

import com.reco1l.verktex.localization.JsonStringSource
import com.reco1l.verktex.localization.Localization
import com.reco1l.verktex.localization.formatString
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Checks the strings generated from the language pack by `:shared:generateStringResources`.
 */
class GeneratedStringsTest {

    private val directory = File("build/generated/resources/assets/strings")

    private val source = object : JsonStringSource() {
        override fun read(locale: String) = directory.resolve("$locale.json").takeIf { it.isFile }?.readBytes()
        override fun readNames() = directory.list()?.toList()
    }

    @Before
    fun setUp() = Localization.addSource(source)

    @After
    fun tearDown() {
        Localization.removeSource(source)
        Localization.locale = Localization.DEFAULT_LOCALE
    }


    @Test
    fun `every language of the pack is generated`() {
        assertTrue(Languages.names.keys.map(Localization::normalize).toSet() - source.locales == emptySet<String>())
    }

    @Test
    fun `strings resolve and follow the locale`() {
        Localization.locale = "en"
        val english = Res.string.accessibility_detector_exit.toString()
        assertEquals("Exit game", english)

        Localization.locale = "es"
        assertTrue(Res.string.accessibility_detector_exit.toString() != english)

        assertEquals(listOf("osu!droid", "osu!standard"), Res.array.difficulty_algorithm_names.values)
    }

    @Test
    fun `every pattern formats in every language`() {
        val specifier = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?([a-zA-Z%])")

        for (locale in source.locales) {
            val json = directory.resolve("$locale.json").readText()
            val keys = Regex("\"([^\"]+)\": \"").findAll(json).map { it.groupValues[1] }

            for (key in keys) {
                val pattern = source.get(locale, key) ?: continue
                // Each argument of the type its conversion takes, by position when it has one.
                val args = ArrayList<Any?>()
                var next = 0
                for (match in specifier.findAll(pattern)) {
                    val value: Any = when (match.groupValues[3]) {
                        "%", "n" -> continue
                        "d", "x", "X" -> 1
                        "f" -> 1.5
                        else -> "a"
                    }
                    val index = match.groupValues[1].removeSuffix("$").toIntOrNull()?.minus(1) ?: next++
                    while (args.size <= index) args += null
                    args[index] = value
                }

                try {
                    formatString(pattern, *args.toTypedArray())
                } catch (e: IllegalArgumentException) {
                    throw AssertionError("$locale/$key: \"$pattern\"", e)
                }
            }
        }
    }
}
