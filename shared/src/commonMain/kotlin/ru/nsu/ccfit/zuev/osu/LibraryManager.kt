package ru.nsu.ccfit.zuev.osu

import com.osudroid.OsuGame
import com.osudroid.ui.v2.SplashScene
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.audio.MusicService

import com.reco1l.verktex.ui.toast.Toasts
import com.reco1l.verktex.Platform
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.resources.Res
import com.reco1l.verktex.Logger
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.jvm.JvmStatic
import kotlin.math.ceil
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

object LibraryManager : MusicService.Playlist {


    private var library: MutableList<BeatmapSetInfo> = ArrayList()

    private var currentIndex = 0


    // Filled by the scanning coroutines, which run in parallel.
    private val pendingBeatmaps = ArrayList<BeatmapInfo>()

    private val pendingBeatmapsLock = SynchronizedObject()


    private fun checkDirectory(path: String): Boolean {

        val directory = path.toPath()
        val fileSystem = Platform.fileSystem

        if (!fileSystem.exists(directory)) {

            try {
                fileSystem.createDirectory(directory)
            } catch (e: IOException) {
                Toasts.showLong(Res.string.message_error_createdir.format(directory.toString()))
                return false
            }

            try {
                fileSystem.write(directory / ".nomedia") {}

            } catch (e: IOException) {
                Logger.e("LibraryManager", "Failed to create .nomedia file", e)
            }
        }

        return true
    }


    @JvmStatic
    fun loadLibrary() {

        library = ArrayList()

        // Without the main activity the library is whatever is in the (in-memory) database, there
        // are no folders to check nor difficulties to calculate.
        if (OsuGame.isHeadless) {
            currentIndex = 0
            library = DatabaseManager.beatmapInfoTable.getBeatmapSetList().toMutableList()
            return
        }

        if (Platform.storage.externalDirectory == null || !checkDirectory(GameFolders.scorePath) && !checkDirectory(GameFolders.beatmapPath)) {
            return
        }

        currentIndex = 0
        library = DatabaseManager.beatmapInfoTable.getBeatmapSetList().toMutableList()

        DifficultyCalculationManager.calculateDifficulties()
    }

    /**
     * Scans the beatmap directory to find differences between the database and the file system. If any differences are
     * found, the database is updated accordingly. Returns once the database is up to date.
     *
     * @param forceUpdateDirectories The directories to force update, even if they are already in the database.
     * Can be `null`.
     */
    suspend fun scanDirectory(forceUpdateDirectories: Collection<String>?) {

        if (!checkDirectory(GameFolders.beatmapPath)) {
            return
        }

        val fileSystem = Platform.fileSystem
        val directories = fileSystem.listOrNull(GameFolders.beatmapPath.toPath())
            ?.filter { fileSystem.metadataOrNull(it)?.isDirectory == true }
            ?: return

        LibraryDatabaseManager(directories, forceUpdateDirectories).start()
    }


    @JvmStatic
    fun clearDatabase() {
        DatabaseManager.beatmapInfoTable.deleteAll()
        loadLibrary()
        currentIndex = 0
    }

    @JvmStatic
    fun deleteBeatmapSet(beatmapSet: BeatmapSetInfo) {
        // Headless, the beatmap sets don't come from the beatmap folder, don't touch any file.
        if (!OsuGame.isHeadless) {
            deleteRecursivelySafely(beatmapSet.path.toPath())
        }
        DatabaseManager.beatmapInfoTable.deleteBeatmapSet(beatmapSet.directory)
        BeatmapCache.invalidate(beatmapSet)
        loadLibrary()
    }

    // As File.deleteRecursively, which deletes what it can and doesn't throw.
    private fun deleteRecursivelySafely(path: Path) {
        try {
            Platform.fileSystem.deleteRecursively(path, mustExist = false)
        } catch (e: IOException) {
            Logger.e("LibraryManager", "Failed to delete $path", e)
        }
    }

    private fun scanBeatmapSetFolder(directory: Path) {

        val fileSystem = Platform.fileSystem
        val osuFiles = fileSystem.listOrNull(directory)?.filter { it.name.endsWith(".osu") }

        if (osuFiles == null) {
            if (Settings[OsuSettings.DeleteUnimportedBeatmaps]) {
                deleteRecursivelySafely(directory)
            }
            return
        }

        val lastModified = fileSystem.metadataOrNull(directory)?.lastModifiedAtMillis ?: 0L
        var beatmapsFound = 0

        for (osuFile in osuFiles) {
            try {
                val data = BeatmapParser(osuFile.toString()).parse(false)
                val beatmapInfo = BeatmapInfo(data, lastModified, false)

                synchronized(pendingBeatmapsLock) {
                    pendingBeatmaps.add(beatmapInfo)
                }
                beatmapsFound++
            } catch (e: Exception) {
                Logger.e("LibraryManager", "Failed to parse beatmap file: $osuFile", e)
                if (Settings[OsuSettings.DeleteUnimportedBeatmaps]) {
                    try {
                        fileSystem.delete(osuFile)
                    } catch (_: Exception) {}
                }
            }
        }

        if (Settings[OsuSettings.DeleteUnimportedBeatmaps] && beatmapsFound == 0) {
            deleteRecursivelySafely(directory)
        }
    }


    @JvmStatic
    fun getLibrary(): List<BeatmapSetInfo> {
        return library
    }

    @JvmStatic
    fun shuffleLibrary() {
        library.shuffle()
        currentIndex = 0
    }

    @JvmStatic
    fun getSizeOfBeatmaps(): Int {
        return library.size
    }


    @JvmStatic
    fun getCurrentBeatmapSet(): BeatmapSetInfo {
        return library[currentIndex]
    }

    @JvmStatic
    fun selectNextBeatmapSet(): BeatmapSetInfo? {

        if (library.isEmpty()) {
            currentIndex = 0
            return null
        }

        currentIndex = ++currentIndex % library.size
        return library[currentIndex]
    }

    @JvmStatic
    fun selectPreviousBeatmapSet(): BeatmapSetInfo? {

        if (library.isEmpty()) {
            currentIndex = 0
            return null
        }

        currentIndex = if (currentIndex == 0) library.size - 1 else --currentIndex
        return library[currentIndex]
    }

    override fun nextBeatmapSet() = selectNextBeatmapSet()

    override fun previousBeatmapSet() = selectPreviousBeatmapSet()


    @JvmStatic
    fun findBeatmapSetIndex(info: BeatmapInfo) {

        for (i in library.indices) {

            if (library[i].directory == info.setDirectory) {
                currentIndex = i
                return
            }
        }

        currentIndex = 0
    }


    @JvmStatic
    fun findBeatmapByMD5(md5: String?): BeatmapInfo? {

        if (md5 == null) {
            return null
        }

        for (i in library.size - 1 downTo 0) {
            val beatmaps = library[i].beatmaps

            for (j in beatmaps.size - 1 downTo 0) {
                val beatmap = beatmaps[j]

                if (md5 == beatmap.md5) {
                    return beatmap
                }
            }
        }

        return null
    }


    private class LibraryDatabaseManager(
        private val directories: List<Path>,
        private val forceUpdateDirectories: Collection<String>?
    ) {

        private val directoryCount = AtomicInt(directories.size)

        private val fileCached = AtomicInt(0)


        suspend fun start() {

            val optimalChunkSize = ceil(directories.size.toDouble() / Platform.processorCount).toInt()

            // Returns once every chunk is scanned.
            coroutineScope {
                for (directoriesSlice in directories.chunked(optimalChunkSize.coerceAtLeast(1))) {

                    launch(Dispatchers.Default) {

                        for (i1 in directoriesSlice.size - 1 downTo 0) {

                            val directory = directoriesSlice[i1]

                            if ((forceUpdateDirectories == null || !forceUpdateDirectories.contains(directory.name)) &&
                                DatabaseManager.beatmapInfoTable.isBeatmapSetImported(directory.name)) {
                                directoryCount.decrementAndFetch()
                                continue
                            }

                            SplashScene.progress = 50 + 50 * fileCached.fetchAndIncrement() / directoryCount.load().coerceAtLeast(1)
                            SplashScene.info = "Loading " + directory.name + "..."

                            // A folder that fails is logged and doesn't stop the others.
                            try {
                                scanBeatmapSetFolder(directory)
                            } catch (t: Throwable) {
                                Logger.e("LibraryManager", "Failed to scan beatmap set folder: $directory", t)
                            }
                        }
                    }
                }
            }

            try {
                synchronized(pendingBeatmapsLock) {
                    DatabaseManager.beatmapInfoTable.insertAll(pendingBeatmaps)
                    pendingBeatmaps.clear()
                }
            } catch (e: Exception) {
                Logger.e("LibraryManager", "Failed to insert beatmaps into database.", e)
                return
            }

            val missingDirectories = ArrayList(DatabaseManager.beatmapInfoTable.getBeatmapSetPaths())
            val beatmapPath = GameFolders.beatmapPath.toPath()

            for (i in missingDirectories.size - 1 downTo 0) {

                if (Platform.fileSystem.exists(beatmapPath / missingDirectories[i])) {
                    missingDirectories.removeAt(i)
                }
            }

            if (missingDirectories.isNotEmpty()) {
                DatabaseManager.beatmapInfoTable.deleteAllBeatmapSets(missingDirectories)
            }
        }

    }
}
