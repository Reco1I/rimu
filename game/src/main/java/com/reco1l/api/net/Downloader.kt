package com.reco1l.api.net

import android.net.Uri
import com.reco1l.api.net.SpeedMeasure.BPS
import com.reco1l.api.net.SpeedMeasure.KBPS
import com.reco1l.api.net.SpeedMeasure.MBPS
import com.reco1l.api.net.request.Requester
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Download a file in the defined `outputPath`.
 *
 * While the file is being downloaded its file extension will be set to `.temp`.
 */
class Downloader(client: OkHttpClient, uri: Uri) : Requester(client, uri)
{

    /**
     * The observer that will be use along the download.
     */
    var observer: IDownloaderObserver? = null

    /**
     * Indicates that a current download is in progress.
     */
    var isDownloading = false

    /**
     * The output file where the download stream will write in.
     */
    var file: File? = null
        set(value)
        {
            if (isDownloading)
                throw IllegalStateException("Cannot change output file while downloading!")

            field = value
        }

    /**
     * The current download progress in a range of 0 to 1, it'll be `-1` if a download hasn't been
     * started yet.
     */
    var progress = -1f
        private set

    /**
     * The content length. Keep in mind some servers doesn't provide this, in that case this will
     * always equal to the downloaded data length.
     */
    var length = 0L
        private set


    private var speed = 0f

    private var bytesRead = 0

    private var bytesTotal = 0

    private var startTimeNano = 0L

    private var elapsedTimeNano = 0L


    private var bodyInputStream: InputStream? = null

    private var fileOutputStream: FileOutputStream? = null

    private var bufferedInputStream: BufferedInputStream? = null


    /**
     * Returns the current download speed in the desired measure.
     *
     * @param measure The measurement for the return value, default is Byte/s.
     * @see SpeedMeasure
     */
    fun getSpeed(measure: SpeedMeasure = BPS): Float = when (measure)
    {
        KBPS -> (speed / 1024 * 1e9).toFloat()
        MBPS -> (speed / (1024 * 1024) * 1e9).toFloat()
        BPS -> speed
    }


    override fun close()
    {
        super.close()

        if (!isDownloading)
        {
            bodyInputStream?.close()
            fileOutputStream?.close()
            bufferedInputStream?.close()

            bodyInputStream = null
            fileOutputStream = null
            bufferedInputStream = null
        }
    }

    override fun onResponseError(exception: Exception)
    {
        isDownloading = false
        observer?.onDownloadError(this, exception)
    }

    override fun onResponseSuccess(response: Response)
    {
        requireNotNull(file) { "The output file cannot be null!" }

        // Creating streams
        fileOutputStream = file!!.outputStream()
        bodyInputStream = response.body!!.byteStream()
        bufferedInputStream = bodyInputStream!!.buffered(DEFAULT_BUFFER_SIZE)

        // Setting values
        length = response.body!!.contentLength()

        isDownloading = true
        bytesTotal = 0
        bytesRead = 0

        startTimeNano = System.nanoTime()
        elapsedTimeNano = 0

        // We create the byte array outside the loop to recycle it between iterations.
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

        while (isDownloading)
        {
            bytesRead = bufferedInputStream!!.read(buffer)

            // If read() returns '-1' means there's no more bytes to read
            if (bytesRead < 0)
                break

            // Writing file
            fileOutputStream!!.write(buffer, 0, bytesRead)
            bytesTotal += bytesRead

            // Counting elapsed time
            elapsedTimeNano = System.nanoTime() - startTimeNano

            // Measuring
            progress = bytesTotal / length.toFloat()
            speed = bytesTotal / (elapsedTimeNano % 1e+9f)

            observer?.onDownloadUpdate(this)
        }

        if (isDownloading || progress == 1f)
        {
            isDownloading = false
            close()
            observer?.onDownloadEnd(this)
            return
        }
        isDownloading = false
        close()

        observer?.onDownloadCancel(this)
    }

    override fun execute(): Downloader
    {
        if (isDownloading)
            throw IllegalStateException("Cannot call execute while a file is already downloading!")

        return super.execute() as Downloader
    }

    /**
     * Keep in mind if the file completed its download while this is called the [IDownloaderObserver.onDownloadCancel]
     * callback will not run.
     */
    fun cancel()
    {
        isDownloading = false
    }
}
