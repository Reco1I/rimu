package ru.nsu.ccfit.zuev.osu

import com.osudroid.OsuGame
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
import com.osudroid.notifications.Notifications
import com.osudroid.resources.Res
import com.reco1l.verktex.Logger
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.incrementAndFetch
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

    // The folders scanned without a beatmap, by name, with when they were last modified: they
    // aren't in the database, so they're only scanned again once they change. Under the lock above.
    private val foldersWithoutBeatmaps = HashMap<String, Long>()


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


    /**
     * Loads the library from the database, then calculates the difficulties it lacks.
     */
    suspend fun loadLibrary() {
        if (loadFromDatabase()) {
            DifficultyCalculationManager.calculateDifficulties()
        }
    }

    /**
     * Loads the library from the database alone, as the game starts: the beatmaps folder is
     * scanned afterwards, in the background (see [com.osudroid.beatmaps.LibraryImport.refresh]).
     *
     * @return Whether it was loaded, false headless or without the game's folders.
     */
    suspend fun loadFromDatabase(): Boolean {

        library = ArrayList()

        // Without the main activity the library is whatever is in the (in-memory) database, there
        // are no folders to check nor difficulties to calculate.
        if (OsuGame.isHeadless) {
            currentIndex = 0
            library = DatabaseManager.beatmapInfoTable.getBeatmapSetList().toMutableList()
            return false
        }

        if (Platform.storage.externalDirectory == null || !checkDirectory(GameFolders.scorePath) && !checkDirectory(GameFolders.beatmapPath)) {
            return false
        }

        currentIndex = 0
        library = DatabaseManager.beatmapInfoTable.getBeatmapSetList().toMutableList()
        return true
    }

    /**
     * Scans the beatmap directory to find differences between the database and the file system. If any differences are
     * found, the database is updated accordingly. Returns once the database is up to date.
     *
     * @param forceUpdateDirectories The directories to force update, even if they are already in the database.
     * Can be `null`.
     * @param onStart Called once the folders to scan are known, with how many there are, which
     * can be none.
     * @param onProgress Called each time a folder is scanned, with how many were, from any thread.
     */
    suspend fun scanDirectory(
        forceUpdateDirectories: Collection<String>?,
        onStart: (total: Int) -> Unit = {},
        onProgress: (scanned: Int, total: Int) -> Unit = { _, _ -> }
    ) {

        if (!checkDirectory(GameFolders.beatmapPath)) {
            return
        }

        val fileSystem = Platform.fileSystem
        val directories = fileSystem.listOrNull(GameFolders.beatmapPath.toPath())
            ?.filter { fileSystem.metadataOrNull(it)?.isDirectory == true }
            ?: return

        LibraryDatabaseManager(directories, forceUpdateDirectories).start(onStart, onProgress)
    }


    suspend fun clearDatabase() {
        DatabaseManager.beatmapInfoTable.deleteAll()
        loadLibrary()
        currentIndex = 0
    }

    suspend fun deleteBeatmapSet(beatmapSet: BeatmapSetInfo) {
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

    /**
     * Scans a beatmap set folder, adding its beatmaps to those to insert.
     *
     * @return Whether it's broken: none of its beatmaps could be loaded, and it was kept.
     */
    private fun scanBeatmapSetFolder(directory: Path): Boolean {

        val fileSystem = Platform.fileSystem
        val lastModified = fileSystem.metadataOrNull(directory)?.lastModifiedAtMillis ?: 0L
        val osuFiles = fileSystem.listOrNull(directory)?.filter { it.name.endsWith(".osu") }

        if (osuFiles == null) {
            return keepOrDeleteBroken(directory, lastModified)
        }
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

        return beatmapsFound == 0 && keepOrDeleteBroken(directory, lastModified)
    }

    // A folder without a beatmap that loads is deleted if the setting says so, otherwise it's
    // remembered so that it isn't scanned again until it changes. Returns whether it was kept.
    private fun keepOrDeleteBroken(directory: Path, lastModified: Long): Boolean {
        if (Settings[OsuSettings.DeleteUnimportedBeatmaps]) {
            deleteRecursivelySafely(directory)
            return false
        }
        synchronized(pendingBeatmapsLock) { foldersWithoutBeatmaps[directory.name] = lastModified }
        return true
    }

    // Tells of the folders kept without a beatmap that loads, in one notification.
    private fun notifyBrokenFolders(names: List<String>) {
        if (names.isEmpty()) {
            return
        }

        val text = if (names.size == 1) {
            Res.string.notification_broken_beatmap_set.format(names[0])
        } else {
            Res.string.notification_broken_beatmap_sets.format(names.size, names.sorted().joinToString("\n"))
        }
        Notifications.post(Res.string.notification_library_title, text, isError = true)
    }

    // Whether the folder was scanned without a beatmap and hasn't changed since.
    private fun isKnownWithoutBeatmaps(directory: Path): Boolean {
        val scannedModified = synchronized(pendingBeatmapsLock) { foldersWithoutBeatmaps[directory.name] } ?: return false
        return Platform.fileSystem.metadataOrNull(directory)?.lastModifiedAtMillis == scannedModified
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

        private val scannedCount = AtomicInt(0)

        // The folders kept without a beatmap that loads, under the lock of the pending beatmaps.
        private val brokenFolders = ArrayList<String>()


        suspend fun start(onStart: (Int) -> Unit, onProgress: (Int, Int) -> Unit) {

            // Only the folders the database doesn't have, or that are to be updated, are scanned.
            val pending = directories.filter { directory ->
                forceUpdateDirectories?.contains(directory.name) == true ||
                    !isKnownWithoutBeatmaps(directory) && !DatabaseManager.beatmapInfoTable.isBeatmapSetImported(directory.name)
            }
            onStart(pending.size)

            // Half the processors, as the difficulty calculation, so that the game stays smooth
            // while the library is scanned in the background.
            val threadCount = ceil(Platform.processorCount / 2f).toInt().coerceAtLeast(1)
            val dispatcher = Dispatchers.Default.limitedParallelism(threadCount)
            val chunkSize = ceil(pending.size.toDouble() / threadCount).toInt().coerceAtLeast(1)

            // Returns once every chunk is scanned.
            coroutineScope {
                for (directoriesSlice in pending.chunked(chunkSize)) {

                    launch(dispatcher) {

                        for (i1 in directoriesSlice.size - 1 downTo 0) {

                            val directory = directoriesSlice[i1]

                            // A folder that fails is logged and doesn't stop the others.
                            try {
                                if (scanBeatmapSetFolder(directory)) {
                                    synchronized(pendingBeatmapsLock) { brokenFolders += directory.name }
                                }
                            } catch (t: Throwable) {
                                Logger.e("LibraryManager", "Failed to scan beatmap set folder: $directory", t)
                            }

                            onProgress(scannedCount.incrementAndFetch(), pending.size)
                        }
                    }
                }
            }

            notifyBrokenFolders(synchronized(pendingBeatmapsLock) { brokenFolders.toList() })

            // Taken under the lock, inserted outside it: the insert suspends.
            val scanned = synchronized(pendingBeatmapsLock) {
                pendingBeatmaps.toList().also { pendingBeatmaps.clear() }
            }

            try {
                DatabaseManager.beatmapInfoTable.insertAll(scanned)
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
