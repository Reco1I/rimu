package com.osudroid.beatmaplisting

import com.osudroid.beatmaps.LibraryImport
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.online.DownloadProgress
import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.async
import com.reco1l.verktex.Platform
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.toast.Toasts
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import okio.Path

/**
 * Downloads beatmap sets and imports them, showing the progress in a toast.
 *
 * Sets are downloaded to the cache directory ([com.reco1l.verktex.PlatformStorage.cacheDirectory]),
 * where the library doesn't look for archives, and imported from there.
 */
object BeatmapDownloader {

    // TODO: We should adapt the system to allow multiple downloads at a time.
    private var isDownloading = false

    private val downloadsFolder: Path
        get() = Platform.storage.cacheDirectory / "downloads"


    /**
     * Downloads the beatmap set with [setId] from the selected [BeatmapMirror] and imports it, saving it
     * as [filename].
     */
    fun downloadBeatmapSet(setId: Long, filename: String) {
        val url = BeatmapMirror.selected.download.request(setId, !Settings[OsuSettings.PreferNoVideoDownloads])
        download(url.toString(), filename)
    }

    fun download(url: String, suggestedFilename: String) {

        if (isDownloading) {
            return
        }
        isDownloading = true

        // Remove illegal characters in FAT32 file system.
        // We may extract the beatmap to an EXT4 file system, but that's the least to worry about.
        val filename = suggestedFilename.replace("[\"*/:<>?\\\\|]".toRegex(), "_")

        val file = downloadsFolder / "$filename.osz"

        val toast = Toasts.showPersistent(Res.string.beatmap_downloader_connecting, isIndeterminate = true)

        val download = async {
            toast.text = Res.string.beatmap_downloader_downloading.format(filename)

            try {
                Platform.fileSystem.createDirectories(downloadsFolder)

                httpClient.downloadTo(url, file, configure = {
                    header(HttpHeaders.UserAgent, MIRROR_USER_AGENT)
                }, onProgress = { onDownloadUpdate(toast, filename, it) })
            } catch (e: CancellationException) {
                toast.dismiss()
                Toasts.showLong("Download canceled.")
                isDownloading = false
                throw e
            } catch (e: Exception) {
                toast.dismiss()
                Toasts.showLong("Download failed. " + e.message)
                isDownloading = false
                return@async
            }

            onDownloadEnd(toast, filename, file)
        }

        toast.setAction(Res.string.beatmap_downloader_cancel) { download.cancel() }
    }


    private suspend fun onDownloadEnd(toast: Toasts.Toast, filename: String, file: Path) {
        toast.setAction(null)
        toast.isIndeterminate = true
        toast.text = Res.string.beatmap_downloader_importing.format(filename)

        try {
            if (LibraryImport.importBeatmapSet(file) == null) {
                Toasts.showLong("Import failed, failed to extract ZIP file.")
                return
            }

            LibraryImport.loadLibrary()
        } finally {
            // The archive was only kept to be imported, and one that failed was renamed by the import.
            // There's one download at a time, so nothing else is in the folder.
            Platform.fileSystem.deleteRecursively(downloadsFolder, mustExist = false)

            toast.dismiss()

            if (Multiplayer.isConnected) {
                Multiplayer.roomScene?.onRoomBeatmapChange(Multiplayer.room!!.beatmap)
            }

            isDownloading = false
        }
    }

    private fun onDownloadUpdate(toast: Toasts.Toast, filename: String, download: DownloadProgress) {
        val progress = download.percentage.toInt()
        val info = formatString("\n%.3f mb/s (%d%%)", download.speedKbps / 1024, progress.coerceAtLeast(0))

        toast.text = Res.string.beatmap_downloader_downloading.format(filename).toString() + info
        toast.isIndeterminate = progress < 0
        toast.progress = progress.coerceAtLeast(0) / 100f
    }
}
