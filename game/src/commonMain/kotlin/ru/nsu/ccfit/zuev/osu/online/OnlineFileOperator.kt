package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.utils.SystemFileSystem
import com.reco1l.framework.net.Http
import com.reco1l.framework.net.HttpRequestBuilder
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

import ru.nsu.ccfit.zuev.osu.helper.Debug

object OnlineFileOperator {

    @JvmStatic
    @JvmOverloads
    fun downloadFile(urlstr: String, filename: String, checkModificationDate: Boolean = false): Boolean {
        Debug.i("Starting download $urlstr")
        val file = filename.toPath()
        try {
            val lastModified = SystemFileSystem.metadataOrNull(file)?.lastModifiedAtMillis
            val exists = SystemFileSystem.exists(file)

            if (!checkModificationDate && exists) {
                Debug.i(file.name + " already exists")
                return true
            }
            // Cheching for errors
            Debug.i("Connected to $urlstr")

            val builder = HttpRequestBuilder(urlstr)

            if (checkModificationDate && exists) {
                builder.addHeader("If-Modified-Since", formatHttpDate(lastModified ?: 0L))
            }

            val request = builder.build()
            Http.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SystemFileSystem.sink(file).buffer().use { sink ->
                        sink.writeAll(response.body)
                    }
                }
            }

            return true
        } catch (e: IOException) {
            Debug.e("downloadFile IOException " + e.message, e)
            return false
        } catch (e: Exception) {
            Debug.e("downloadFile Exception " + e.message, e)
            return false
        }
    }

    private val DAYS = arrayOf("Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Wed")
    private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /**
     * Formats [millis] since the epoch as an HTTP date, such as `Thu, 01 Jan 1970 00:00:00 GMT`.
     */
    @JvmStatic
    fun formatHttpDate(millis: Long): String {
        val seconds = millis.floorDiv(1000L)
        val days = seconds.floorDiv(86400L)
        val secondOfDay = seconds.mod(86400L)

        // Converts days since the epoch to a civil date, see http://howardhinnant.github.io/date_algorithms.html.
        val z = days + 719468
        val era = z.floorDiv(146097L)
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = doy - (153 * mp + 2) / 5 + 1
        val month = if (mp < 10) mp + 3 else mp - 9
        val year = yoe + era * 400 + if (month <= 2) 1 else 0

        fun pad(value: Long) = value.toString().padStart(2, '0')

        return "${DAYS[days.mod(7L).toInt()]}, ${pad(day)} ${MONTHS[(month - 1).toInt()]} $year " +
            "${pad(secondOfDay / 3600)}:${pad(secondOfDay % 3600 / 60)}:${pad(secondOfDay % 60)} GMT"
    }
}
