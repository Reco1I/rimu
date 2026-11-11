package com.osudroid.beatmaps

import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.reco1l.verktex.settings.Settings
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Without a platform the settings are only kept in memory, and the folders are temporary ones.
 */
class LibraryImportTest {

    private lateinit var folder: File
    private lateinit var songs: File
    private lateinit var skins: File


    @Before
    fun setUp() {
        folder = Files.createTempDirectory("library-import").toFile()
        songs = File(folder, "Songs").apply { mkdirs() }
        skins = File(folder, "Skin").apply { mkdirs() }

        Settings[OsuSettings.CorePath] = folder.path
        Settings[OsuSettings.BeatmapPath] = songs.path
        Settings[OsuSettings.SkinTopPath] = skins.path
        Settings[OsuSettings.ScanDownload] = false
        Settings[OsuSettings.DeleteOsz] = true
    }

    @After
    fun tearDown() {
        folder.deleteRecursively()
        OsuSettings.all.values.forEach { Settings.remove(it) }
    }


    // A zip archive with the files, by their path inside it.
    private fun archive(file: File, entries: Map<String, String>): File {
        ZipOutputStream(file.outputStream()).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return file
    }

    private val beatmapSet = mapOf(
        "Artist - Title (Mapper) [Hard].osu" to "osu file format v14",
        "audio.mp3" to "audio",
        "sb/background.png" to "image",
    )


    @Test
    fun testImportsABeatmapSet() {
        val osz = archive(File(folder, "123 Artist - Title.osz"), beatmapSet)

        assertEquals("123 Artist - Title", LibraryImport.importBeatmapSet(osz.toOkioPath()))

        val set = File(songs, "123 Artist - Title")
        assertEquals("osu file format v14", File(set, "Artist - Title (Mapper) [Hard].osu").readText())
        assertEquals("image", File(set, "sb/background.png").readText())

        // Deleted with the setting on.
        assertFalse(osz.exists())
    }

    @Test
    fun testKeepsTheArchiveWithoutTheSetting() {
        Settings[OsuSettings.DeleteOsz] = false
        val osz = archive(File(folder, "123 Artist - Title.osz"), beatmapSet)

        LibraryImport.importBeatmapSet(osz.toOkioPath())

        assertTrue(osz.exists())
    }

    @Test
    fun testANoVideoSetGoesInTheFolderOfTheOneWithIt() {
        val osz = archive(File(folder, "123 Artist - Title [no video].osz"), beatmapSet)

        assertEquals("123 Artist - Title", LibraryImport.importBeatmapSet(osz.toOkioPath()))
        assertTrue(File(songs, "123 Artist - Title/audio.mp3").isFile)
    }

    @Test
    fun testRenamesAnInvalidArchive() {
        val osz = File(folder, "broken.osz").apply { writeText("not a zip") }

        assertNull(LibraryImport.importBeatmapSet(osz.toOkioPath()))

        assertFalse(osz.exists())
        assertTrue(File(folder, "broken.osz.badzip").exists())
        assertFalse(File(songs, "broken").exists())
    }

    @Test
    fun testImportsTheArchivesOfTheFolders() {
        archive(File(folder, "1 In The Game Folder.osz"), beatmapSet)
        archive(File(songs, "2 In The Songs Folder.osz"), beatmapSet)
        File(songs, "3 Not An Archive.osz").writeText("not a zip")

        val imported = LibraryImport.importBeatmapSets()

        assertTrue(File(songs, "1 In The Game Folder/audio.mp3").isFile)
        assertTrue(File(songs, "2 In The Songs Folder/audio.mp3").isFile)

        // Only the sets of the beatmaps folder are rescanned, as before.
        assertEquals(setOf("2 In The Songs Folder"), imported)

        // Not an archive, it's left as it is to be looked at.
        assertTrue(File(songs, "3 Not An Archive.osz").exists())
    }

    @Test
    fun testImportsASkin() {
        val osk = archive(File(skins, "My Skin.osk"), mapOf("skin.ini" to "[General]", "hitcircle.png" to "image"))

        assertTrue(LibraryImport.importSkin(osk.toOkioPath()))

        val skin = File(skins, "My Skin")
        assertEquals("[General]", File(skin, "skin.ini").readText())
        assertFalse(osk.exists())
        assertEquals(skin.toOkioPath().toString(), SkinFolders.skins["My Skin"])
    }

    @Test
    fun testOpenedFilesAreImportedByTheirExtension() {
        val osz = archive(File(folder, "9 Opened.osz"), beatmapSet)
        val osk = archive(File(folder, "Opened Skin.osk"), mapOf("skin.ini" to "[General]"))

        assertEquals(setOf("9 Opened"), LibraryImport.importOpenedFile(osz.toOkioPath()))
        assertEquals(emptySet<String>(), LibraryImport.importOpenedFile(osk.toOkioPath()))
        assertEquals(emptySet<String>(), LibraryImport.importOpenedFile(File(folder, "replay.odr").toOkioPath()))

        assertTrue(File(songs, "9 Opened/audio.mp3").isFile)
        assertTrue(File(skins, "Opened Skin/skin.ini").isFile)
    }
}
