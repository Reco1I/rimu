package com.osudroid.replay

import androidx.room.Room
import com.osudroid.data.DatabaseManager
import com.osudroid.data.OsuDatabase
import com.osudroid.data.ScoreInfo
import com.osudroid.data.buildOsuDatabase
import com.osudroid.math.Vector2
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okio.FileNotFoundException
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class ReplayImporterTest {

    private lateinit var folder: File
    private var previousCorePath = ""
    private lateinit var database: OsuDatabase


    @Before
    fun setUp() {
        folder = Files.createTempDirectory("replay-import").toFile()
        File(folder, "Scores").mkdirs()

        // The setting itself: the folder it resolves to without one needs a host.
        previousCorePath = Settings[OsuSettings.CorePath]
        Settings[OsuSettings.CorePath] = folder.path + "/"

        database = Room.inMemoryDatabaseBuilder<OsuDatabase>().buildOsuDatabase()
        DatabaseManager.database = database
    }

    @After
    fun tearDown() {
        database.close()
        Settings[OsuSettings.CorePath] = previousCorePath
        folder.deleteRecursively()
    }


    // The scores the importer stored, of the beatmap the replays are of.
    private fun storedScores(): List<ScoreInfo> = runBlocking {
        DatabaseManager.scoreInfoTable.getBeatmapScores(MD5)
    }

    private fun import(file: File) = runBlocking { ReplayImporter.import(file.toOkioPath()) }

    private fun savedReplay(name: String): File {
        val replay = Replay(true)
        replay.setBeatmap("Artist - Title", "Hard", MD5)
        replay.setObjectCount(1)
        replay.addPress(100, Vector2(10f, 20f), 0)
        replay.addUp(200, 0)
        replay.stat = StatisticV2().apply {
            time = 1_700_000_000_000L
            hit300 = 1
            totalScore = 300
            scoreMaxCombo = 1
            playerName = "player"
        }

        val file = File(folder, name)
        replay.save(file.path)
        return file
    }

    @Test
    fun testImportsReplay() {
        val file = savedReplay("my replay.odr")

        import(file)

        val score = storedScores().single()
        assertEquals("player", score.playerName)
        assertEquals(300L, score.score)
        assertEquals("my replay.odr", score.replayFilename)

        // Copied into the scores folder, the original kept.
        assertTrue(File(folder, "Scores/my replay.odr").isFile)
        assertTrue(file.isFile)
    }

    @Test
    fun testNamesTemporaryFilesAfterThePlay() {
        val file = savedReplay("picked123.tmp")

        import(file)

        val name = storedScores().single().replayFilename
        assertTrue(name, name.startsWith("player_Artist - Title_Hard_") && name.endsWith(".odr"))
        assertTrue(File(folder, "Scores/$name").isFile)
    }

    @Test
    fun testDoesNotCopyAReplayAlreadyInTheScoresFolder() {
        val file = savedReplay("Scores/already.odr")

        import(file)

        assertEquals("already.odr", storedScores().single().replayFilename)
        assertTrue(file.isFile)
    }

    @Test(expected = FileNotFoundException::class)
    fun testMissingFile() {
        import(File(folder, "missing.odr"))
    }

    @Test
    fun testRejectsOtherFiles() {
        val file = File(folder, "notes.odr").apply { writeText("not a replay") }

        val error = runCatching { import(file) }.exceptionOrNull()

        assertTrue(error.toString(), error is IllegalArgumentException)
        assertTrue(storedScores().isEmpty())
        assertFalse(File(folder, "Scores/notes.odr").exists())
    }

    private companion object {
        const val MD5 = "0123456789abcdef0123456789abcdef"
    }
}
