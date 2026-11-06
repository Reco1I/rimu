package com.osudroid.beatmaplisting

import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.osudroid.settings.GameFolders
import com.osudroid.android.AndroidGame
import com.osudroid.OsuPlatform
import java.io.File
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.view.View
import com.osudroid.resources.Res
import com.osudroid.online.DownloadProgress
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import okio.Path.Companion.toOkioPath
import com.osudroid.utils.mainThread
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.ui.v1.DownloadFragment
import com.osudroid.utils.async
import net.lingala.zip4j.ZipFile
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.helper.FileUtils
import java.io.IOException

object BeatmapDownloader {


    private lateinit var fragment: DownloadFragment

    private lateinit var currentFilename: String


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
        currentFilename = suggestedFilename.replace("[\"*/:<>?\\\\|]".toRegex(), "_")

        val file = context.getExternalFilesDir(DIRECTORY_DOWNLOADS)!!.resolve("$currentFilename.osz")

        fragment = DownloadFragment()
        fragment.setOnLoad {

            fragment.text.visibility = View.VISIBLE
            fragment.text.text = Res.string.beatmap_downloader_connecting.toString()

            fragment.button.visibility = View.VISIBLE
            fragment.button.text = Res.string.beatmap_downloader_cancel.toString()

            val download = async {
                mainThread {
                    fragment.text.text = Res.string.beatmap_downloader_downloading.format(currentFilename).toString()
                }

                try {
                    httpClient.downloadTo(url, file.toOkioPath(), configure = {
                        header(HttpHeaders.UserAgent, MIRROR_USER_AGENT)
                    }, onProgress = ::onDownloadUpdate)
                } catch (e: CancellationException) {
                    onDownloadCancel()
                    throw e
                } catch (e: Exception) {
                    onDownloadFail(e)
                    return@async
                }

                onDownloadEnd(file)
            }

            fragment.setDownload(download)
            fragment.button.setOnClickListener { download.cancel() }
        }
        fragment.show()
    }


    private fun onDownloadEnd(file: File) {
        mainThread {
            fragment.progressBar.visibility = View.GONE
            fragment.progressBar.isIndeterminate = true
            fragment.progressBar.visibility = View.VISIBLE

            fragment.text.text = Res.string.beatmap_downloader_importing.format(currentFilename).toString()
            fragment.button.visibility = View.GONE
        }

        try {
            ZipFile(file).use {
                if (!it.isValidZipFile) {
                    mainThread(fragment::dismiss)
                    Toasts.showLong("Import failed, invalid ZIP file.")
                    return
                }

                if (!FileUtils.extractZip(file.path, GameFolders.beatmapPath)) {
                    mainThread(fragment::dismiss)
                    Toasts.showLong("Import failed, failed to extract ZIP file.")
                    return
                }

                OsuPlatform.implementation.loadBeatmapLibrary()
            }
        } catch (e: IOException) {
            Toasts.showLong("Import failed:" + e.message)
        } finally {
            mainThread(fragment::dismiss)

            if (Multiplayer.isConnected) {
                Multiplayer.roomScene?.onRoomBeatmapChange(Multiplayer.room!!.beatmap)
            }

            isDownloading = false
        }
    }

    private fun onDownloadCancel() {
        Toasts.showLong("Download canceled.")

        mainThread(fragment::dismiss)
        isDownloading = false
    }

    private fun onDownloadUpdate(download: DownloadProgress) {
        val progress = download.percentage.toInt()
        val info = "\n%.3f mb/s (%d%%)".format(download.speedKbps / 1024, progress.coerceAtLeast(0))

        mainThread {
            fragment.text.text = Res.string.beatmap_downloader_downloading.format(currentFilename).toString() + info
            fragment.progressBar.isIndeterminate = progress < 0
            fragment.progressBar.progress = progress.coerceAtLeast(0)
        }
    }

    private fun onDownloadFail(exception: Exception) {
        Toasts.showLong("Download failed. " + exception.message)

        mainThread(fragment::dismiss)
        isDownloading = false
    }
}
