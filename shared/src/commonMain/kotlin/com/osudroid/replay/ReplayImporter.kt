package com.osudroid.replay

import com.osudroid.data.DatabaseManager
import com.osudroid.data.toScoreInfo
import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.Platform
import kotlin.jvm.JvmStatic
import okio.FileNotFoundException
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.scoring.Replay

/**
 * Imports replay files into the local scores: the score it records goes into the database, and the
 * file into the scores folder.
 */
object ReplayImporter {

    /**
     * Imports the replay at [path].
     *
     * @throws FileNotFoundException If there's no file at [path].
     * @throws IOException If it isn't a file, or it can't be copied.
     * @throws IllegalArgumentException If it isn't a replay, or it doesn't record a score.
     * @throws IllegalStateException If the score can't be added to the database.
     */
    @JvmStatic
    fun import(path: Path) {
        val fileSystem = Platform.fileSystem
        val metadata = fileSystem.metadataOrNull(path) ?: throw FileNotFoundException("File does not exist: $path")

        if (!metadata.isRegularFile) {
            throw IOException("Path is not a file: $path")
        }

        val replay = Replay()

        if (!replay.load(path.toString(), false)) {
            throw IllegalArgumentException("Unsupported file. Please choose a valid replay file")
        }

        val stat = replay.stat ?: throw IllegalArgumentException("The replay doesn't record a score")
        val scoreInfo = stat.toScoreInfo()

        // A file that isn't named as a replay (such as a picked file's temporary copy) is named after
        // the play, which doesn't conflict with other replays.
        if (!scoreInfo.replayFilename.endsWith(".odr", ignoreCase = true)) {
            val name = "${scoreInfo.playerName}_${replay.beatmapsetName}_${replay.beatmapName}_${currentTimeMillis()}"
            scoreInfo.replayFilename = name.replace(ILLEGAL_CHARACTERS, "_") + ".odr"
        }

        if (DatabaseManager.scoreInfoTable.insertScore(scoreInfo) < 0) {
            throw IllegalStateException("Failed to insert score to score database")
        }

        val replayFile = scoreInfo.replayPath.toPath()

        if (fileSystem.canonicalize(path) != fileSystem.canonicalizeOrNull(replayFile)) {
            fileSystem.copy(path, replayFile)
        }
    }

    // Characters file systems don't allow in names.
    private val ILLEGAL_CHARACTERS = Regex("[\"*/:<>?\\\\|]")

    private fun okio.FileSystem.canonicalizeOrNull(path: Path) = try {
        canonicalize(path)
    } catch (e: IOException) {
        null
    }
}
