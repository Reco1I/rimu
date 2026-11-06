package com.reco1l.framework.net

import com.reco1l.verktex.Platform
import kotlin.time.TimeSource
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

class FileRequest(val path: String, url: String): WebRequest(url) {


    /**
     * The download observer.
     */
    var observer: IFileRequestObserver? = null

    /**
     * Whether the download is in progress, this is set to true once [execute] is called.
     */
    var isDownloading = false
        private set

    /**
     * Whether the download has been completed successfully.
     */
    val isCompleted: Boolean
        get() = progress == 100.0

    /**
     * The current download speed in kilobytes per second.
     */
    var speedKbps = 0f
        private set

    /**
     * The current download progress in 0 to 1 range.
     */
    var progress = -1.0
        private set

    /**
     * The download buffer size, by default 8 KiB.
     */
    var bufferSize = 8192



    override fun onResponseSuccess(response: HttpResponse) {

        val length = response.contentLength * 1.0
        val file = path.toPath()

        val bytes = ByteArray(bufferSize)

        var totalBytes = 0L
        var bytesRead = 0

        response.body.use { buffer ->

            Platform.fileSystem.sink(file).buffer().use { out ->

                isDownloading = true
                val startTime = TimeSource.Monotonic.markNow()

                while (isDownloading && buffer.read(bytes).also { bytesRead = it } != -1) {

                    out.write(bytes, 0, bytesRead)
                    totalBytes += bytesRead.toLong()

                    val elapsedMilliseconds = startTime.elapsedNow().inWholeMilliseconds.toFloat()

                    progress = totalBytes / length * 100
                    speedKbps = totalBytes / elapsedMilliseconds / 1024f * 1000f

                    observer?.onDownloadUpdate(this)
                }

                if (isDownloading || isCompleted) {
                    isDownloading = false
                    observer?.onDownloadEnd(this)
                    return
                }

                // This mean the file wasn't fully downloaded.
                if (Platform.fileSystem.exists(file)) {
                    Platform.fileSystem.delete(file)
                }
                observer?.onDownloadCancel(this)
            }
        }

    }

    override fun onResponseFail(exception: Exception) {

        isDownloading = false
        observer?.onDownloadFail(this, exception) ?: throw exception
    }


    override fun cancel() {
        isDownloading = false
        super.cancel()
    }

    override fun execute(): FileRequest {
        try {
            super.execute()
        } catch (e: Exception) {
            observer?.onDownloadFail(this, e) ?: throw e
        }
        return this
    }

}


interface IFileRequestObserver {
    fun onDownloadEnd(request: FileRequest) = Unit
    fun onDownloadCancel(request: FileRequest) = Unit
    fun onDownloadUpdate(request: FileRequest) = Unit
    fun onDownloadFail(request: FileRequest, exception: Exception) = Unit
}
