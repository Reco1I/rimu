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
