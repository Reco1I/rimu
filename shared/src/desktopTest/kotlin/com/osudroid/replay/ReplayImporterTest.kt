package com.osudroid.replay

import com.osudroid.data.DatabaseManager
import com.osudroid.data.GameDatabase
import com.osudroid.data.IScoreInfoDAO
import com.osudroid.data.ScoreInfo
import com.osudroid.math.Vector2
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import java.io.File
import java.lang.reflect.Proxy
import java.nio.file.Files
import kotlin.reflect.KClass
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
    private var previousDatabase: GameDatabase? = null

    // What the importer inserts.
    private val insertedScores = mutableListOf<ScoreInfo>()


    // A DAO that only records the scores inserted into it.
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> fake(type: KClass<T>, handler: (String, Array<Any?>?) -> Any?): T =
        Proxy.newProxyInstance(type.java.classLoader, arrayOf(type.java)) { _, method, args ->
            handler(method.name, args)
        } as T

    @Before
    fun setUp() {
        folder = Files.createTempDirectory("replay-import").toFile()
        File(folder, "Scores").mkdirs()

        // The setting itself: the folder it resolves to without one needs a host.
        previousCorePath = Settings[OsuSettings.CorePath]
        GameFolders.corePath = folder.path + "/"

        previousDatabase = runCatching { DatabaseManager.database }.getOrNull()

        val scores = fake(IScoreInfoDAO::class) { name, args ->
            when (name) {
                "insertScore" -> {
                    insertedScores += args!![0] as ScoreInfo
                    insertedScores.size.toLong()
                }
                else -> throw UnsupportedOperationException(name)
            }
        }

        DatabaseManager.database = fake(GameDatabase::class) { name, _ ->
            when (name) {
                "getScoreInfoTable" -> scores
                else -> throw UnsupportedOperationException(name)
            }
        }
    }

    @After
    fun tearDown() {
        Settings[OsuSettings.CorePath] = previousCorePath
        previousDatabase?.let { DatabaseManager.database = it }
        folder.deleteRecursively()
    }


    private fun savedReplay(name: String): File {
        val replay = Replay(true)
        replay.setBeatmap("Artist - Title", "Hard", "0123456789abcdef0123456789abcdef")
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

        ReplayImporter.import(file.toOkioPath())

        assertEquals(1, insertedScores.size)
        val score = insertedScores[0]
        assertEquals("player", score.playerName)
        assertEquals(300L, score.score)
        assertEquals("0123456789abcdef0123456789abcdef", score.beatmapMD5)
        assertEquals("my replay.odr", score.replayFilename)

        // Copied into the scores folder, the original kept.
        assertTrue(File(folder, "Scores/my replay.odr").isFile)
        assertTrue(file.isFile)
    }

    @Test
    fun testNamesTemporaryFilesAfterThePlay() {
        val file = savedReplay("picked123.tmp")

        ReplayImporter.import(file.toOkioPath())

        val name = insertedScores.single().replayFilename
        assertTrue(name, name.startsWith("player_Artist - Title_Hard_") && name.endsWith(".odr"))
        assertTrue(File(folder, "Scores/$name").isFile)
    }

    @Test
    fun testDoesNotCopyAReplayAlreadyInTheScoresFolder() {
        val file = savedReplay("Scores/already.odr")

        ReplayImporter.import(file.toOkioPath())

        assertEquals("already.odr", insertedScores.single().replayFilename)
        assertTrue(file.isFile)
    }

    @Test(expected = FileNotFoundException::class)
    fun testMissingFile() {
        ReplayImporter.import(File(folder, "missing.odr").toOkioPath())
    }

    @Test
    fun testRejectsOtherFiles() {
        val file = File(folder, "notes.odr").apply { writeText("not a replay") }

        val error = runCatching { ReplayImporter.import(file.toOkioPath()) }.exceptionOrNull()

        assertTrue(error.toString(), error is IllegalArgumentException)
        assertFalse(insertedScores.isNotEmpty())
        assertFalse(File(folder, "Scores/notes.odr").exists())
    }
}
