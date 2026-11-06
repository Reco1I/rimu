package com.reco1l.verktex.settings

import com.reco1l.verktex.Platform
import java.nio.file.Files
import kotlinx.serialization.json.JsonPrimitive
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration

class SettingsTest {

    private val flag = setting("test_flag", false)
    private val volume = setting("test_volume", 100f)
    private val names = setting("test_names", listOf("a"))

    private lateinit var directory: Path

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("settings").toOkioPath()
        Platform.implementation = object : Platform.Implementation {
            override val provider get() = throw UnsupportedOperationException()
            override val audioDriver get() = throw UnsupportedOperationException()
            override val storage = Platform.Storage(directory, directory, null)
            override val currentDisplay = Platform.Display(1f)
            override val usesVirtualKeyboard = false
            override val uptimeMillis = 0L
            override fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit) = throw UnsupportedOperationException()
            override fun openWebPage(url: String, userAgent: String?) = Unit
            override fun openUrl(url: String) = Unit
        }
        Settings.reset()
        Settings.isReadOnly = false
    }

    @After
    fun tearDown() {
        Settings.reset()
        Platform.fileSystem.deleteRecursively(directory)
    }


    @Test
    fun `values are saved and read again`() {
        assertTrue(Settings.isNew)
        assertEquals(false, Settings[flag])

        Settings[flag] = true
        Settings[volume] = 42.5f
        Settings[names] = listOf("x", "y")
        Settings.flush()

        Settings.reset()
        assertFalse(Settings.isNew)
        assertEquals(true, Settings[flag])
        assertEquals(42.5f, Settings[volume])
        assertEquals(listOf("x", "y"), Settings[names])
    }

    @Test
    fun `unknown keys are kept and bad values read as the default`() {
        Platform.fileSystem.write(directory / "settings.json") {
            writeUtf8("""{ "someone_elses": 3, "test_flag": "maybe", "test_volume": 50 }""")
        }

        assertEquals(false, Settings[flag])
        assertEquals(50f, Settings[volume])

        Settings[flag] = true
        Settings.flush()

        val saved = Platform.fileSystem.read(directory / "settings.json") { readUtf8() }
        assertTrue("someone_elses" in saved)
    }

    @Test
    fun `values from elsewhere are read as their key's type`() {
        Settings.putAll(mapOf("test_flag" to JsonPrimitive("true"), "test_volume" to JsonPrimitive("75")))

        assertEquals(true, Settings[flag])
        assertEquals(75f, Settings[volume])
    }

    @Test
    fun `read only settings aren't saved`() {
        Settings.isReadOnly = true
        Settings[flag] = true
        Settings.flush()

        assertFalse(Platform.fileSystem.exists(directory / "settings.json"))
    }

    @Test
    fun `observers get the new values`() {
        var observed: Boolean? = null
        val observer = Settings.observe(flag) { observed = it }

        Settings[flag] = true
        assertEquals(true, observed)

        Settings.putAll(mapOf("test_flag" to JsonPrimitive(false)))
        assertEquals(false, observed)

        Settings.removeObserver(flag, observer)
    }
}
