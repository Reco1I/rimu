package com.osudroid.settings

import com.reco1l.verktex.settings.Settings
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Without a platform the settings are only kept in memory, which is enough for these.
 */
class OsuSettingsTest {

    private val folder = Files.createTempDirectory("osu-settings").toFile()

    @Before
    fun setUp() {
        Settings[OsuSettings.CorePath] = folder.path
    }

    @After
    fun tearDown() {
        folder.deleteRecursively()
        OsuSettings.all.values.forEach { Settings.remove(it) }
    }


    @Test
    fun `android preferences are imported with their types`() {
        OsuSettings.importLegacyPreferences(mapOf(
            "snakingInSliders" to false,
            "bgmvolume" to 40,
            "soundvolume" to 55.5f,
            "spinnerstyle" to "0",
            "version" to 1234L,
            "unknown" to "kept",
        ))

        assertEquals(false, Settings[OsuSettings.SnakingInSliders])
        assertEquals(40f, Settings[OsuSettings.MusicVolume])
        assertEquals(0.555f, OsuSettings.soundVolume, 1e-6f)
        assertEquals(0, OsuSettings.spinnerStyle)
        assertEquals(1234L, Settings[OsuSettings.Version])
        assertTrue("unknown" in Settings.toJson())
    }

    @Test
    fun `desktop preferences, all strings, are read as their types`() {
        OsuSettings.importLegacyPreferences(mapOf("snakingInSliders" to "false", "offset" to "-20", "bgbrightness" to "12.5"))

        assertEquals(false, Settings[OsuSettings.SnakingInSliders])
        assertEquals(-20, Settings[OsuSettings.Offset])
        assertEquals(12.5f, Settings[OsuSettings.BackgroundBrightness])
    }

    @Test
    fun `backups leave out sensitive settings and keep numbers in range`() {
        Settings[OsuSettings.OnlinePassword] = "secret"
        Settings[OsuSettings.CursorSize] = 80
        assertTrue(SettingsBackup.export())

        val backup = folder.resolve("osudroid.cfg")
        assertFalse("secret" in backup.readText())

        // A backup of the old preferences, with a value out of range and one of the wrong type.
        backup.writeText("""{ "cursorSize": 1000, "playfieldSize": "big", "onlinePassword": "other", "bgmvolume": 30 }""")
        assertTrue(SettingsBackup.import())

        assertEquals(300, Settings[OsuSettings.CursorSize])
        assertEquals(100, Settings[OsuSettings.PlayfieldSize])
        assertEquals("secret", Settings[OsuSettings.OnlinePassword])
        assertEquals(30f, Settings[OsuSettings.MusicVolume])
    }

    @Test
    fun `folders end with a slash and follow the game folder`() {
        assertEquals(folder.path + "/", GameFolders.corePath)
        assertEquals(folder.path + "/Songs/", GameFolders.beatmapPath)
        assertEquals(folder.path + "/Scores/", GameFolders.scorePath)
    }
}
