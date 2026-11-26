package com.osudroid.beatmaps

import com.osudroid.multiplayer.Multiplayer
import com.osudroid.notifications.Notification
import com.osudroid.notifications.Notifications
import com.osudroid.resources.Res
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.async
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.settings.Settings
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip
import ru.nsu.ccfit.zuev.osu.LibraryManager

/**
 * Imports beatmap set (`.osz`) and skin (`.osk`) archives: each is extracted into a folder of its
 * own, named as the archive, in the beatmaps or the skins folder.
 *
 * Archives are found in the game's folders and, with [OsuSettings.ScanDownload], in the host's
 * [downloads directory][com.reco1l.verktex.PlatformStorage.downloadsDirectory]. One that isn't a valid archive is renamed to `<name>.badzip`, and one
 * that fails to extract to `<name>.bad<extension>`, so they aren't tried again.
 *
 * The library is kept up to date in the background ([refresh]), one refresh at a time, telling
 * its progress through [Notifications].
 */
object LibraryImport {

    private const val TAG = "LibraryImport"

    // A set downloaded without its video lands in the folder of the one with it.
    private val NO_VIDEO_SUFFIX = Regex(" \\[no video]$", RegexOption.IGNORE_CASE)

    private val fileSystem: FileSystem
        get() = Platform.fileSystem

    // One refresh or import of a download at a time, in the order they were asked for.
    private val mutex = Mutex()

    private val jobsLock = SynchronizedObject()

    // The refreshes running or waiting for the mutex.
    private val jobs = mutableSetOf<Job>()

    // A refresh of the whole library waiting for the mutex, which another one asked for joins.
    private var queuedRefresh: Job? = null

    // The outdated star ratings are reset by the first refresh, before the difficulties are calculated.
    @Volatile
    private var hasCheckedStarRatings = false


    /**
     * Brings the library up to date in the background: imports the skins and the beatmap sets
     * found, or only [openedFile] (a beatmap set or skin archive the game was opened with) if
     * there's one, scans the beatmaps folder, loads the library and calculates the difficulties it
     * lacks. The sets imported are scanned again even if the database has them.
     *
     * Refreshes run one after the other; one asked for while another of the whole library is
     * still waiting to run is that one.
     */
    @JvmStatic
    @JvmOverloads
    fun refresh(openedFile: String? = null): Job = synchronized(jobsLock) {
        if (openedFile == null) {
            queuedRefresh?.let { if (it.isActive) return it }
        }

        // The job waits for the lock to run, so it sees itself queued.
        val job = async {
            mutex.withLock {
                val self = currentCoroutineContext()[Job]
                synchronized(jobsLock) {
                    if (queuedRefresh === self) {
                        queuedRefresh = null
                    }
                }
                runRefresh(openedFile?.toPath())
            }
        }

        jobs += job
        if (openedFile == null) {
            queuedRefresh = job
        }
        job.invokeOnCompletion { synchronized(jobsLock) { jobs -= job } }
        job
    }

    /**
     * Waits for the refreshes running or waiting to run, e.g. to find a beatmap the game was
     * started with.
     */
    suspend fun awaitIdle() {
        while (true) {
            val running = synchronized(jobsLock) { jobs.toList() }
            if (running.isEmpty()) {
                return
            }
            running.joinAll()
        }
    }

    /**
     * Whether a refresh runs or waits to run.
     */
    val isRefreshing: Boolean
        get() = synchronized(jobsLock) { jobs.isNotEmpty() }

    /**
     * Imports a beatmap set downloaded to [file], once no refresh runs, and adds it to the
     * library.
     *
     * @return Whether it was imported.
     */
    suspend fun importDownloaded(file: Path): Boolean = mutex.withLock {
        val folder = importBeatmapSet(file) ?: return false

        LibraryManager.scanDirectory(setOf(folder))
        LibraryManager.loadLibrary()
        notifyLibraryChanged()
        true
    }


    private suspend fun runRefresh(openedFile: Path?) {
        // Archives take a while to extract: listed in the notification center while they do,
        // without popping up, and told at once when they're all imported. Scanning is silent.
        var notification: Notification? = null

        fun report(text: CharSequence, done: Int, total: Int) {
            val progress = if (total > 0) done.toFloat() / total else 0f
            val current = notification

            if (current == null) {
                notification = Notifications.startTask(Res.string.notification_library_title, text, isIndeterminate = false, showToast = false).also {
                    it.progress = progress
                }
            } else {
                current.text = text
                current.progress = progress
            }
        }

        val importedSkins = ArrayList<String>()
        val importedSets = ArrayList<String>()

        try {
            val forceUpdate = when (openedFile?.extension) {
                null -> {
                    importSkins(
                        onProgress = { done, total -> report(Res.string.notification_importing_skins.format(done, total), done, total) },
                        onImported = { importedSkins += it }
                    )
                    importBeatmapSets(
                        onProgress = { done, total -> report(Res.string.notification_importing_beatmaps.format(done, total), done, total) },
                        onImported = { importedSets += it }
                    )
                }

                "osk" -> {
                    if (importSkin(openedFile)) importedSkins += skinFolderName(openedFile)
                    emptySet()
                }

                "osz" -> setOfNotNull(importBeatmapSet(openedFile)).also { importedSets += it }

                else -> emptySet()
            }

            notification?.dismiss()
            notification = null

            if (!hasCheckedStarRatings) {
                hasCheckedStarRatings = true
                DifficultyCalculationManager.checkForOutdatedStarRatings()
            }

            LibraryManager.scanDirectory(forceUpdate)
            LibraryManager.loadLibrary()
            notifyLibraryChanged()

            notifyImported(importedSets, importedSkins)
        } catch (e: CancellationException) {
            notification?.dismiss()
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to refresh the library", e)
            notification?.dismiss()
            Notifications.post(Res.string.notification_library_title, Res.string.message_error.format(e.message ?: e::class.simpleName), isError = true)
        }
    }

    // One notification for what a refresh imported, naming it if it's a single beatmap set or skin.
    private fun notifyImported(sets: List<String>, skins: List<String>) {
        val title = Res.string.notification_library_title

        when {
            sets.isEmpty() && skins.isEmpty() -> return
            sets.size == 1 && skins.isEmpty() -> Notifications.post(Res.string.notification_beatmap_set_imported, sets[0])
            sets.isEmpty() && skins.size == 1 -> Notifications.post(Res.string.notification_skin_imported, skins[0])
            skins.isEmpty() -> Notifications.post(title, Res.string.notification_beatmap_sets_imported.format(sets.size))
            sets.isEmpty() -> Notifications.post(title, Res.string.notification_skins_imported.format(skins.size))
            else -> Notifications.post(title, Res.string.notification_beatmap_sets_and_skins_imported.format(sets.size, skins.size))
        }
    }

    // Song select and the menu show the library again.
    private fun notifyLibraryChanged() {
        if (!Verktex.isInitialized) {
            return
        }

        Verktex.scheduleOnUpdate {
            when (val scene = Verktex.scene) {
                is SongSelect -> scene.loadBeatmaps()
                is MainScene -> scene.onLibraryChanged()
            }

            // The room's beatmap may be one that was just imported.
            val room = Multiplayer.room
            if (Multiplayer.isConnected && room != null) {
                Multiplayer.roomScene?.onRoomBeatmapChange(room.beatmap)
            }
        }
    }


    /**
     * Imports an archive the game was opened with, a beatmap set or a skin, by its extension.
     *
     * @return The folder of the beatmap set imported, if it was one.
     */
    @JvmStatic
    fun importOpenedFile(file: Path): Set<String> = when (file.extension) {
        "osz" -> setOfNotNull(importBeatmapSet(file))

        "osk" -> {
            importSkin(file)
            emptySet()
        }

        else -> emptySet()
    }

    /**
     * Imports the beatmap sets in the game's folder, the beatmaps folder and, if the setting allows
     * it, the downloads folder.
     *
     * @param onProgress Called before each archive and after the last one, with how many were
     * imported and how many there are.
     * @param onImported Called with the folder of each beatmap set imported.
     * @return The folders of the beatmap sets imported from the beatmaps and the downloads folders.
     */
    @JvmStatic
    @JvmOverloads
    fun importBeatmapSets(
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        onImported: (folder: String) -> Unit = {}
    ): Set<String> {
        val inGameFolder = findArchives(GameFolders.corePath.toPath(), "osz")
        val inBeatmapsFolder = findArchives(GameFolders.beatmapPath.toPath(), "osz")
        val inDownloads = downloadsFolder()?.let { findArchives(it, "osz") }.orEmpty()

        val archives = inGameFolder + inBeatmapsFolder + inDownloads
        if (archives.isEmpty()) {
            return emptySet()
        }

        val imported = archives.withIndex().mapNotNull { (i, archive) ->
            onProgress(i, archives.size)
            importBeatmapSet(archive)?.let { onImported(it); archive to it }
        }.toMap()
        onProgress(archives.size, archives.size)

        // As before, the sets of the game's folder aren't rescanned if the database has them.
        return (inBeatmapsFolder + inDownloads).mapNotNull { imported[it] }.toSet()
    }

    /**
     * Imports the skins in the skins folder and, if the setting allows it, the downloads folder.
     *
     * @param onProgress Called before each archive and after the last one, with how many were
     * imported and how many there are.
     * @param onImported Called with the name of each skin imported.
     * @return How many were imported.
     */
    @JvmStatic
    @JvmOverloads
    fun importSkins(
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        onImported: (name: String) -> Unit = {}
    ): Int {
        val archives = findArchives(SkinFolders.topPath.toPath(), "osk") +
            downloadsFolder()?.let { findArchives(it, "osk") }.orEmpty()

        if (archives.isEmpty()) {
            return 0
        }

        val imported = archives.withIndex().count { (i, archive) ->
            onProgress(i, archives.size)
            importSkin(archive).also { if (it) onImported(skinFolderName(archive)) }
        }
        onProgress(archives.size, archives.size)
        return imported
    }

    /**
     * Extracts the beatmap set [archive] into the beatmaps folder.
     *
     * @return The name of its folder, `null` if it couldn't be imported.
     */
    @JvmStatic
    fun importBeatmapSet(archive: Path): String? {
        val folderName = archive.name.substringBeforeLast('.').replace(NO_VIDEO_SUFFIX, "")
        val folder = GameFolders.beatmapPath.toPath() / folderName

        if (!extract(archive, folder)) {
            return null
        }

        if (Settings[OsuSettings.DeleteOsz]) {
            deleteSafely(archive)
        }

        return folderName
    }

    /**
     * Extracts the skin [archive] into the skins folder, and adds it to the skins.
     *
     * @return Whether it was imported.
     */
    @JvmStatic
    fun importSkin(archive: Path): Boolean {
        val folderName = skinFolderName(archive)
        val folder = SkinFolders.topPath.toPath() / folderName

        if (!extract(archive, folder)) {
            return false
        }

        // The archive isn't needed anymore, the folder is the skin.
        deleteSafely(archive)

        SkinFolders.add(folderName, folder.toString())
        return true
    }

    /**
     * Extracts every file of the zip [archive] into [folder].
     */
    private fun extract(archive: Path, folder: Path): Boolean {
        val zip = try {
            fileSystem.openZip(archive)
        } catch (e: IOException) {
            Notifications.post(Res.string.notification_import_failed_title, Res.string.notification_import_invalid.format(archive.name), isError = true)
            Logger.e(TAG, "${archive.name} isn't a valid archive", e)
            renameSafely(archive, archive.name + ".badzip")
            return false
        }

        // A folder this import creates is removed if it fails, it would be an empty beatmap set.
        val folderExisted = fileSystem.exists(folder)

        try {
            fileSystem.createDirectories(folder)

            val root = "/".toPath()

            for (entry in zip.listRecursively(root)) {
                // The entries are relative to the archive's root, normalized, so they stay in the folder.
                val target = folder / entry.relativeTo(root)

                if (zip.metadata(entry).isDirectory) {
                    fileSystem.createDirectories(target)
                } else {
                    target.parent?.let(fileSystem::createDirectories)
                    zip.source(entry).use { source -> fileSystem.write(target) { writeAll(source) } }
                }
            }
        } catch (e: IOException) {
            Notifications.post(Res.string.notification_import_failed_title, Res.string.notification_import_extract_failed.format(archive.name), isError = true)
            Logger.e(TAG, "Failed to extract ${archive.name}", e)
            renameSafely(archive, archive.name.substringBeforeLast('.') + ".bad" + archive.extension)

            if (!folderExisted) {
                try {
                    fileSystem.deleteRecursively(folder, mustExist = false)
                } catch (e: IOException) {
                    Logger.e(TAG, "Failed to delete $folder", e)
                }
            }
            return false
        }

        return true
    }

    // The archives of a folder with the extension (lowercase), which open as zip files.
    private fun findArchives(folder: Path, extension: String): List<Path> {
        val files = fileSystem.listOrNull(folder) ?: return emptyList()

        return files.filter { file ->
            file.extension == extension &&
                fileSystem.metadataOrNull(file)?.isRegularFile == true &&
                runCatching { fileSystem.openZip(file) }.isSuccess
        }
    }

    private fun skinFolderName(archive: Path) = archive.name.substringBeforeLast('.')

    private fun downloadsFolder(): Path? {
        if (!Settings[OsuSettings.ScanDownload]) {
            return null
        }
        return Platform.storage.downloadsDirectory
    }

    private fun deleteSafely(file: Path) = try {
        fileSystem.delete(file, mustExist = false)
    } catch (e: IOException) {
        Logger.e(TAG, "Failed to delete $file", e)
    }

    private fun renameSafely(file: Path, name: String) = try {
        fileSystem.atomicMove(file, file.parent?.div(name) ?: name.toPath())
    } catch (e: IOException) {
        Logger.e(TAG, "Failed to rename $file", e)
    }

    private val Path.extension
        get() = name.substringAfterLast('.', "").lowercase()
}
