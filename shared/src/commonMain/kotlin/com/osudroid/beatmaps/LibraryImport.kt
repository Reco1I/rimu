package com.osudroid.beatmaps

import com.osudroid.resources.Res
import com.osudroid.settings.GameFolders
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.ui.v2.SplashScene
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.toast.Toasts
import kotlin.jvm.JvmStatic
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
 */
object LibraryImport {

    private const val TAG = "LibraryImport"

    // A set downloaded without its video lands in the folder of the one with it.
    private val NO_VIDEO_SUFFIX = Regex(" \\[no video]$", RegexOption.IGNORE_CASE)

    private val fileSystem: FileSystem
        get() = Platform.fileSystem


    /**
     * Imports the beatmap sets found, or only [openedFile] (a beatmap set or skin archive the game
     * was opened with) if there's one, then scans the beatmaps folder and loads the library. The sets
     * imported are scanned again even if the database has them.
     */
    suspend fun loadLibrary(openedFile: String? = null) {
        SplashScene.info = "Checking for new maps..."

        val importedSets = if (openedFile != null) importOpenedFile(openedFile.toPath()) else importBeatmapSets()

        LibraryManager.scanDirectory(importedSets)
        LibraryManager.loadLibrary()
    }

    /**
     * Imports an archive the game was opened with, a beatmap set or a skin, by its extension.
     *
     * @return The folder of the beatmap set imported, if it was one.
     */
    @JvmStatic
    fun importOpenedFile(file: Path): Set<String> = when (file.extension) {
        "osz" -> {
            Toasts.show(Res.string.library_importing)
            setOfNotNull(importBeatmapSet(file))
        }

        "osk" -> {
            Toasts.show(Res.string.library_skin_importing)
            importSkin(file)
            emptySet()
        }

        else -> emptySet()
    }

    /**
     * Imports the beatmap sets in the game's folder, the beatmaps folder and, if the setting allows
     * it, the downloads folder.
     *
     * @return The folders of the beatmap sets imported from the beatmaps and the downloads folders.
     */
    @JvmStatic
    fun importBeatmapSets(): Set<String> {
        val inGameFolder = findArchives(GameFolders.corePath.toPath(), "osz")
        val inBeatmapsFolder = findArchives(GameFolders.beatmapPath.toPath(), "osz")
        val inDownloads = downloadsFolder()?.let { findArchives(it, "osz") }.orEmpty()

        val archives = inGameFolder + inBeatmapsFolder + inDownloads
        if (archives.isEmpty()) {
            return emptySet()
        }

        Toasts.show(Res.string.library_importing_several.format(archives.size))

        val imported = archives.mapNotNull { archive -> importBeatmapSet(archive)?.let { archive to it } }.toMap()

        // As before, the sets of the game's folder aren't rescanned if the database has them.
        return (inBeatmapsFolder + inDownloads).mapNotNull { imported[it] }.toSet()
    }

    /**
     * Imports the skins in the skins folder and, if the setting allows it, the downloads folder.
     */
    @JvmStatic
    fun importSkins() {
        SplashScene.info = "Checking new skins..."

        val archives = findArchives(SkinFolders.topPath.toPath(), "osk") +
            downloadsFolder()?.let { findArchives(it, "osk") }.orEmpty()

        if (archives.isEmpty()) {
            return
        }

        Toasts.show(Res.string.library_skin_importing_several.format(archives.size))
        archives.forEach(::importSkin)
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
        val folderName = archive.name.substringBeforeLast('.')
        val folder = SkinFolders.topPath.toPath() / folderName

        if (!extract(archive, folder)) {
            return false
        }

        // The archive isn't needed anymore, the folder is the skin.
        deleteSafely(archive)

        Toasts.showLong(Res.string.library_imported.format(folderName))
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
            Toasts.show(Res.string.message_error.format("Invalid file"))
            Logger.e(TAG, "${archive.name} isn't a valid archive", e)
            renameSafely(archive, archive.name + ".badzip")
            return false
        }

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
            Logger.e(TAG, "Failed to extract ${archive.name}", e)
            renameSafely(archive, archive.name.substringBeforeLast('.') + ".bad" + archive.extension)
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
