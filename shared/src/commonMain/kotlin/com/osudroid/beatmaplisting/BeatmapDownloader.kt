package com.osudroid.beatmaplisting

import com.osudroid.beatmaps.LibraryImport
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.notifications.Notification
import com.osudroid.notifications.Notifications
import com.osudroid.online.DownloadProgress
import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.async
import com.reco1l.verktex.Platform
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.settings.Settings
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.Path

/**
 * Downloads beatmap sets and imports them, showing the progress in a notification and in [current].
 *
 * Sets are downloaded to the cache directory ([com.reco1l.verktex.PlatformStorage.cacheDirectory]),
 * where the library doesn't look for archives, and imported from there.
 */
object BeatmapDownloader {

    /**
     * A download, from connecting to the mirror until the set is imported.
     *
     * @property beatmapSetId The set being downloaded, null when it isn't known.
     * @property progress From 0 to 1, null while it isn't known.
     */
    data class Download(val beatmapSetId: Long?, val phase: Phase, val progress: Float? = null)

    enum class Phase {
        Connecting,
        Downloading,
        Importing
    }


    // TODO: We should adapt the system to allow multiple downloads at a time.
    private val currentDownload = MutableStateFlow<Download?>(null)

    /**
     * The download in progress, null when there's none.
     */
    val current: StateFlow<Download?> = currentDownload.asStateFlow()

    private val importedSets = MutableSharedFlow<Long>(extraBufferCapacity = 16)

    /**
     * The sets imported once downloaded, when their id is known.
     */
    val imported: SharedFlow<Long> = importedSets.asSharedFlow()

    /**
     * Whether a set is being downloaded or imported. There's one download at a time.
     */
    val isDownloading
        get() = currentDownload.value != null

    private val downloadsFolder: Path
        get() = Platform.storage.cacheDirectory / "downloads"


    /**
     * Downloads the beatmap set with [setId] from the selected [BeatmapMirror] and imports it, saving it
     * as [filename].
     */
    fun downloadBeatmapSet(setId: Long, filename: String) {
        val url = BeatmapMirror.selected.download.request(setId, !Settings[OsuSettings.PreferNoVideoDownloads])
        download(url, filename, setId)
    }

    /**
     * Downloads the beatmap set archive at [url] and imports it, saving it as [suggestedFilename].
     * Nothing happens while another set is being downloaded.
     */
    fun download(url: String, suggestedFilename: String, beatmapSetId: Long? = null) {

        if (isDownloading) {
            return
        }
        currentDownload.value = Download(beatmapSetId, Phase.Connecting)

        // Remove illegal characters in FAT32 file system.
        // We may extract the beatmap to an EXT4 file system, but that's the least to worry about.
        val filename = suggestedFilename.replace("[\"*/:<>?\\\\|]".toRegex(), "_")

        val file = downloadsFolder / "$filename.osz"

        val toast = Notifications.startTask(filename, Res.string.beatmap_downloader_connecting, isIndeterminate = true)

        val download = async {
            toast.text = Res.string.beatmap_downloader_downloading.format(filename)

            try {
                Platform.fileSystem.createDirectories(downloadsFolder)

                httpClient.downloadTo(url, file, configure = {
                    header(HttpHeaders.UserAgent, MIRROR_USER_AGENT)
                }, onProgress = { onDownloadUpdate(toast, filename, beatmapSetId, it) })
            } catch (e: CancellationException) {
                toast.fail(Res.string.notification_download_canceled)
                currentDownload.value = null
                throw e
            } catch (e: Exception) {
                toast.fail(Res.string.notification_download_failed.format(e.message ?: e::class.simpleName))
                currentDownload.value = null
                return@async
            }

            onDownloadEnd(toast, filename, beatmapSetId, file)
        }

        toast.setAction(Res.string.beatmap_downloader_cancel) { download.cancel() }
    }


    private suspend fun onDownloadEnd(toast: Notification, filename: String, beatmapSetId: Long?, file: Path) {
        toast.setAction(null)
        toast.isIndeterminate = true
        toast.text = Res.string.beatmap_downloader_importing.format(filename)

        currentDownload.value = Download(beatmapSetId, Phase.Importing)

        var imported = false
        try {
            // After the refresh running, if any: they'd scan the same folder.
            if (!LibraryImport.importDownloaded(file)) {
                return
            }
            imported = true

            if (beatmapSetId != null) {
                importedSets.emit(beatmapSetId)
            }
        } finally {
            // The archive was only kept to be imported, and one that failed was renamed by the import.
            // There's one download at a time, so nothing else is in the folder.
            Platform.fileSystem.deleteRecursively(downloadsFolder, mustExist = false)

            if (imported) {
                toast.complete(Res.string.notification_download_done)
            } else {
                toast.fail(Res.string.notification_download_import_failed)
            }

            if (Multiplayer.isConnected) {
                Multiplayer.roomScene?.onRoomBeatmapChange(Multiplayer.room!!.beatmap)
            }

            currentDownload.value = null
        }
    }

    private fun onDownloadUpdate(toast: Notification, filename: String, beatmapSetId: Long?, download: DownloadProgress) {
        val progress = download.percentage.toInt()
        val info = formatString("\n%.3f mb/s (%d%%)", download.speedKbps / 1024, progress.coerceAtLeast(0))

        toast.text = Res.string.beatmap_downloader_downloading.format(filename).toString() + info
        toast.isIndeterminate = progress < 0
        toast.progress = progress.coerceAtLeast(0) / 100f

        currentDownload.value = Download(beatmapSetId, Phase.Downloading, if (progress < 0) null else progress / 100f)
    }
}
