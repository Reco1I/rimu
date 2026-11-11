package com.osudroid.beatmaplisting

import android.os.Environment.DIRECTORY_DOWNLOADS
import com.osudroid.OsuGameHost
import com.osudroid.android.AndroidGame
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.online.DownloadProgress
import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.osudroid.resources.Res
import com.osudroid.utils.async
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.ui.toast.Toasts
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import java.io.File
import kotlinx.coroutines.CancellationException
import okio.Path.Companion.toOkioPath
import com.osudroid.beatmaps.LibraryImport

/**
 * Downloads beatmap sets and imports them, showing the progress in a toast.
 */
object BeatmapDownloader {


    private val context = AndroidGame.mainActivity!!

    // TODO: We should adapt the system to allow multiple downloads at a time.
    private var isDownloading = false


    fun download(url: String, suggestedFilename: String) {

        if (isDownloading) {
            return
        }
        isDownloading = true

        // Remove illegal characters in FAT32 file system.
        // We may extract the beatmap to an EXT4 file system, but that's the least to worry about.
        val filename = suggestedFilename.replace("[\"*/:<>?\\\\|]".toRegex(), "_")

        val file = context.getExternalFilesDir(DIRECTORY_DOWNLOADS)!!.resolve("$filename.osz")

        val toast = Toasts.showPersistent(Res.string.beatmap_downloader_connecting, isIndeterminate = true)

        val download = async {
            toast.text = Res.string.beatmap_downloader_downloading.format(filename)

            try {
                httpClient.downloadTo(url, file.toOkioPath(), configure = {
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


    private fun onDownloadEnd(toast: Toasts.Toast, filename: String, file: File) {
        toast.setAction(null)
        toast.isIndeterminate = true
        toast.text = Res.string.beatmap_downloader_importing.format(filename)

        try {
            if (LibraryImport.importBeatmapSet(file.toOkioPath()) == null) {
                Toasts.showLong("Import failed, failed to extract ZIP file.")
                return
            }

            OsuGameHost.current.loadBeatmapLibrary()
        } finally {
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
