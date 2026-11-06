package ru.nsu.ccfit.zuev.osu

import com.reco1l.verktex.ui.toast.Toasts
import com.reco1l.verktex.Platform
import com.osudroid.OsuPlatform
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.DifficultyCalculationManager
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.resources.Res
import com.reco1l.verktex.Logger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

object LibraryManager {


    private var library: MutableList<BeatmapSetInfo> = ArrayList()

    private var currentIndex = 0

    private var isCaching = true


    private val pendingBeatmaps = ArrayList<BeatmapInfo>()

    private val mutex = Object()


    private fun checkDirectory(path: String): Boolean {

        val directory = File(path)

        if (!directory.exists()) {

            if (!directory.mkdir()) {
                Toasts.showLong(StringTable.format(Res.string.message_error_createdir, directory.path))
                return false
            }

            try {
                File(directory, ".nomedia").createNewFile()

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
        if (GlobalManager.getInstance().isHeadless) {
            currentIndex = 0
            library = DatabaseManager.beatmapInfoTable.getBeatmapSetList().toMutableList()
            return
        }

        if (Platform.storage.externalDirectory == null || !checkDirectory(Config.getScorePath()) && !checkDirectory(Config.getBeatmapPath())) {
            return
        }

        currentIndex = 0
        library = DatabaseManager.beatmapInfoTable.getBeatmapSetList().toMutableList()

        DifficultyCalculationManager.calculateDifficulties()
    }

    /**
     * Scans the beatmap directory to find differences between the database and the file system. If any differences are
     * found, the database is updated accordingly.
     *
     * @param forceUpdateDirectories The directories to force update, even if they are already in the database.
     * Can be `null`.
     */
    @JvmStatic
    fun scanDirectory(forceUpdateDirectories: Collection<String>?) {

        if (!checkDirectory(Config.getBeatmapPath())) {
            return
        }

        val directories = File(Config.getBeatmapPath()).listFiles(File::isDirectory) ?: return

        LibraryDatabaseManager(directories, forceUpdateDirectories).start()

        // Wait for all threads to finish
        while (isCaching) {
            synchronized(mutex) {
                try {
                    mutex.wait()
                } catch (e: InterruptedException) {
                    Logger.e("LibraryManager", "Failed to wait for thread termination", e)
                }
            }
        }

        isCaching = true
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
        if (!GlobalManager.getInstance().isHeadless) {
            File(beatmapSet.path).deleteRecursively()
        }
        DatabaseManager.beatmapInfoTable.deleteBeatmapSet(beatmapSet.directory)
        BeatmapCache.invalidate(beatmapSet)
        loadLibrary()
    }

    private fun scanBeatmapSetFolder(directory: File) {

        val osuFiles = directory.listFiles { _, name -> name.endsWith(".osu") }

        if (osuFiles == null) {
            if (Config.isDeleteUnimportedBeatmaps()) {
                directory.deleteRecursively()
            }
            return
        }

        var beatmapsFound = 0

        for (osuFile in osuFiles) {
            try {
                val data = BeatmapParser(osuFile.path).parse(false)
                val beatmapInfo = BeatmapInfo(data, directory.lastModified(), false)

                pendingBeatmaps.add(beatmapInfo)
                beatmapsFound++
            } catch (e: Exception) {
                Logger.e("LibraryManager", "Failed to parse beatmap file: " + osuFile.path, e)
                if (Config.isDeleteUnimportedBeatmaps()) {
                    try {
                        osuFile.delete()
                    } catch (_: Exception) {}
                }
            }
        }

        if (Config.isDeleteUnimportedBeatmaps() && beatmapsFound == 0) {
            directory.deleteRecursively()
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
        private val directories: Array<File>,
        private val forceUpdateDirectories: Collection<String>?
    ) {

        private val executors: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())


        private var directoryCount = directories.size

        private var fileCached = 0


        fun start() {

            val optimalChunkSize = ceil(directoryCount.toDouble() / Runtime.getRuntime().availableProcessors()).toInt()

            var i = 0
            while (i < directories.size) {

                val directoriesSlice = directories.copyOfRange(i, min(i + optimalChunkSize, directories.size))

                executors.submit {

                    for (i1 in directoriesSlice.size - 1 downTo 0) {

                        val directory = directoriesSlice[i1]

                        if ((forceUpdateDirectories == null || !forceUpdateDirectories.contains(directory.name)) &&
                            DatabaseManager.beatmapInfoTable.isBeatmapSetImported(directory.name)) {
                            directoryCount--
                            continue
                        }

                        GlobalManager.getInstance().loadingProgress = 50 + 50 * fileCached / directoryCount
                        GlobalManager.getInstance().info = "Loading " + directory.name + "..."

                        fileCached++

                        // The executor keeps what a task throws in its future, which nothing reads.
                        try {
                            scanBeatmapSetFolder(directory)
                        } catch (t: Throwable) {
                            Logger.e("LibraryManager", "Failed to scan beatmap set folder: " + directory.path, t)
                        }
                    }
                }

                i += optimalChunkSize
            }

            executors.shutdown()

            try {
                if (executors.awaitTermination(1, TimeUnit.HOURS)) {

                    isCaching = false

                    synchronized(mutex) {
                        mutex.notify()
                    }

                } else {
                    Logger.e("LibraryManager", "Timeout while waiting for executor termination.")
                }

            } catch (e: InterruptedException) {
                Logger.e("LibraryManager", "Failed while waiting for executor termination.", e)
            }

            try {
                DatabaseManager.beatmapInfoTable.insertAll(pendingBeatmaps)
                pendingBeatmaps.clear()
            } catch (e: Exception) {
                Logger.e("LibraryManager", "Failed to insert beatmaps into database.", e)
                return
            }

            val missingDirectories = ArrayList(DatabaseManager.beatmapInfoTable.getBeatmapSetPaths())

            for (i in missingDirectories.size - 1 downTo 0) {

                if (File(Config.getBeatmapPath(), missingDirectories[i]).exists()) {
                    missingDirectories.removeAt(i)
                }
            }

            if (missingDirectories.isNotEmpty()) {
                DatabaseManager.beatmapInfoTable.deleteAllBeatmapSets(missingDirectories)
            }
        }

    }
}
