package ru.nsu.ccfit.zuev.osu.online

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import ru.nsu.ccfit.zuev.osu.SecurityUtils

import ru.nsu.ccfit.zuev.osu.helper.Debug

import kotlin.jvm.JvmStatic

abstract class PostBuilder {
    private val values = StringBuilder()

    fun addParam(key: String, value: String) {
        if (values.isNotEmpty()) {
            values.append("_")
        }
        addParamInternal(key, value)
        values.append(urlEncode(value))
    }

    @Throws(RequestException::class)
    suspend fun requestWithAttempts(scriptUrl: String, attempts: Int): ArrayList<String> {
        var response: ArrayList<String>? = null
        val signature = SecurityUtils.signRequest(values.toString())

        if (signature != null) {
            addParam("sign", signature)
        }
        for (i in 0 until attempts) {
            try {
                response = request(scriptUrl)
            } catch (e: RequestException) {
                if (e.cause?.let { it::class.simpleName } == "UnknownHostException") {
                    Debug.e("Cannot resolve server name")
                    break
                }
                Debug.e("Received error, continuing... ", e)
                response = null
            }

            if (response == null || response.isEmpty() || response[0].isEmpty()
                    || !(response[0] == "FAIL" || response[0] == "SUCCESS")) {
                delay(3.seconds)
                continue
            }
            break
        }

        if (response == null) response = ArrayList()

        if (response.isEmpty()) {
            response.add("")
        }
        return response
    }

    @Throws(RequestException::class)
    private suspend fun request(scriptUrl: String): ArrayList<String> {
        val response = ArrayList<String>()

        try {
            val text = post(scriptUrl)

            Debug.i("request url=$scriptUrl")
            Debug.i("request --------Content---------")
            for (line in readLines(text)) {
                Debug.i("request [${response.size}]: $line")
                response.add(line)
            }
            Debug.i("request url=$scriptUrl")
            Debug.i("request -----End of content-----")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Debug.e(e.message, e)
        }

        if (response.isEmpty()) {
            response.add("")
        }
        return response
    }

    protected abstract fun addParamInternal(key: String, value: String)

    /**
     * Posts the parameters to [url], returning the response's text.
     */
    protected abstract suspend fun post(url: String): String

    class RequestException(cause: Throwable) : Exception(cause)

    companion object {

        private const val HEX = "0123456789ABCDEF"

        /**
         * Encodes [value] as `java.net.URLEncoder` does with UTF-8: letters, digits and `.-*_` are kept,
         * a space becomes `+`, and everything else is percent-encoded.
         */
        @JvmStatic
        fun urlEncode(value: String) = buildString {
            for (byte in value.encodeToByteArray()) {
                val c = byte.toInt() and 0xFF
                when {
                    c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code
                        || c == '.'.code || c == '-'.code || c == '*'.code || c == '_'.code -> append(c.toChar())
                    c == ' '.code -> append('+')
                    else -> {
                        append('%')
                        append(HEX[c shr 4])
                        append(HEX[c and 0x0F])
                    }
                }
            }
        }

        /**
         * Splits [text] in lines as `BufferedReader.readLine` does.
         */
        private fun readLines(text: String): List<String> {
            if (text.isEmpty()) {
                return emptyList()
            }
            val lines = text.lines()
            return if (text.endsWith('\n') || text.endsWith('\r')) lines.dropLast(1) else lines
        }
    }
}
