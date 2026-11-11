package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.downloadTo
import com.osudroid.online.httpClient
import com.reco1l.verktex.Platform
import io.ktor.client.request.header
import kotlin.jvm.JvmStatic
import kotlin.time.Instant
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.coroutines.CancellationException
import okio.IOException
import okio.Path.Companion.toPath

import ru.nsu.ccfit.zuev.osu.helper.Debug

object OnlineFileOperator {

    /**
     * Downloads [urlstr] into [filename], unless it exists. With [checkModificationDate] an existing
     * file is downloaded again if the server has a newer one.
     *
     * @return Whether the file is there, downloaded or not.
     */
    suspend fun downloadFile(urlstr: String, filename: String, checkModificationDate: Boolean = false): Boolean {
        Debug.i("Starting download $urlstr")
        val file = filename.toPath()
        try {
            val lastModified = Platform.fileSystem.metadataOrNull(file)?.lastModifiedAtMillis
            val exists = Platform.fileSystem.exists(file)

            if (!checkModificationDate && exists) {
                Debug.i(file.name + " already exists")
                return true
            }
            // Cheching for errors
            Debug.i("Connected to $urlstr")

            httpClient.downloadTo(urlstr, file, configure = {
                if (checkModificationDate && exists) {
                    header("If-Modified-Since", formatHttpDate(lastModified ?: 0L))
                }
            })

            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Debug.e("downloadFile IOException " + e.message, e)
            return false
        } catch (e: Exception) {
            Debug.e("downloadFile Exception " + e.message, e)
            return false
        }
    }

    private val HTTP_DATE = DateTimeComponents.Format {
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
        chars(", ")
        day()
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        year()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
        chars(" GMT")
    }

    /**
     * Formats [millis] since the epoch as an HTTP date, such as `Thu, 01 Jan 1970 00:00:00 GMT`.
     */
    @JvmStatic
    fun formatHttpDate(millis: Long): String = Instant.fromEpochMilliseconds(millis).format(HTTP_DATE)
}
