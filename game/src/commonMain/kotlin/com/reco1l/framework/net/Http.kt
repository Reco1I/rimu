package com.reco1l.framework.net

import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import okio.BufferedSource

/**
 * Makes HTTP requests through the platform's client (OkHttp on Android). Calls are blocking, so
 * they're made from background threads.
 */
object Http {

    /**
     * The implementation of the current platform.
     */
    lateinit var implementation: Implementation

    /**
     * Creates a call for [request], which runs once it's executed.
     */
    @JvmStatic
    fun newCall(request: HttpRequest): HttpCall = implementation.newCall(request)


    interface Implementation {

        /**
         * Creates a call for [request], which runs once it's executed.
         */
        fun newCall(request: HttpRequest): HttpCall
    }
}

/**
 * A request ready to be executed.
 */
interface HttpCall {

    /**
     * Sends the request and waits for the response, which must be closed once read.
     *
     * @throws okio.IOException If the request couldn't be sent or the response couldn't be read.
     */
    fun execute(): HttpResponse

    /**
     * Cancels the call, a response being read fails then.
     */
    fun cancel()
}

/**
 * The response to a request. Its body can be read once.
 */
interface HttpResponse : AutoCloseable {

    /**
     * The status code.
     */
    val code: Int

    /**
     * The status message.
     */
    val message: String

    /**
     * Whether the code is in the 200-299 range.
     */
    val isSuccessful: Boolean
        get() = code in 200..299

    /**
     * The length of the body in bytes, or -1 if unknown.
     */
    val contentLength: Long

    /**
     * The body.
     */
    val body: BufferedSource

    /**
     * The value of the header [name], or null if it isn't there.
     */
    fun header(name: String): String?
}


/**
 * An HTTP request.
 */
class HttpRequest(
    @JvmField val url: String,
    @JvmField val method: String,
    @JvmField val headers: List<Pair<String, String>>,
    @JvmField val body: HttpBody?
) {

    fun newBuilder() = HttpRequestBuilder(url).also {
        it.method = method
        it.headers += headers
        it.body = body
    }
}

/**
 * Builds an [HttpRequest], a GET to [url] unless a body is posted.
 */
class HttpRequestBuilder(var url: String) {

    internal var method = "GET"
    internal val headers = mutableListOf<Pair<String, String>>()
    internal var body: HttpBody? = null


    fun url(url: String) = apply { this.url = url }

    /**
     * Adds a header, keeping the ones with the same name.
     */
    fun header(name: String, value: String) = apply { headers += name to value }

    fun addHeader(name: String, value: String) = header(name, value)

    fun post(body: HttpBody) = apply {
        method = "POST"
        this.body = body
    }

    fun build() = HttpRequest(url, method, headers.toList(), body)
}

/**
 * The body of a request.
 */
sealed class HttpBody {

    /**
     * Raw bytes of a [contentType], such as `application/json; charset=utf-8`.
     */
    class Bytes(@JvmField val contentType: String, @JvmField val bytes: ByteArray) : HttpBody()

    /**
     * A URL encoded form (`application/x-www-form-urlencoded`).
     */
    class Form(@JvmField val fields: List<Pair<String, String>>) : HttpBody()

    /**
     * A multipart form (`multipart/form-data`).
     */
    class Multipart(@JvmField val parts: List<Part>) : HttpBody()

    /**
     * A part of a [Multipart] form: a plain value, or a file when it has a [filename].
     */
    class Part @JvmOverloads constructor(
        @JvmField val name: String,
        @JvmField val value: String? = null,
        @JvmField val filename: String? = null,
        @JvmField val contentType: String? = null,
        @JvmField val filePath: String? = null
    )

    companion object {
        const val JSON_UTF_8 = "application/json; charset=utf-8"
    }
}

/**
 * Builds a URL by adding query parameters to [url], percent-encoded.
 */
class HttpUrlBuilder(private val url: String) {

    private val parameters = mutableListOf<Pair<String, String?>>()


    /**
     * Adds a query parameter, without a value if it's null.
     */
    fun addQueryParameter(name: String, value: String?) = apply { parameters += name to value }

    fun build(): String {
        if (parameters.isEmpty()) {
            return url
        }

        val fragmentStart = url.indexOf('#').takeIf { it >= 0 } ?: url.length
        val base = url.substring(0, fragmentStart)
        val fragment = url.substring(fragmentStart)

        return buildString {
            append(base)
            append(if ('?' !in base) '?' else if (base.endsWith('?') || base.endsWith('&')) "" else '&')

            parameters.forEachIndexed { i, (name, value) ->
                if (i > 0) append('&')
                append(encodeQueryComponent(name))
                if (value != null) {
                    append('=')
                    append(encodeQueryComponent(value))
                }
            }

            append(fragment)
        }
    }


    companion object {

        private const val HEX = "0123456789ABCDEF"

        /**
         * Percent-encodes [value] as UTF-8, keeping only the unreserved characters.
         */
        @JvmStatic
        fun encodeQueryComponent(value: String) = buildString {
            for (byte in value.encodeToByteArray()) {
                val c = byte.toInt() and 0xFF
                if (c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code || c in '0'.code..'9'.code
                    || c == '-'.code || c == '.'.code || c == '_'.code || c == '~'.code) {
                    append(c.toChar())
                } else {
                    append('%')
                    append(HEX[c shr 4])
                    append(HEX[c and 0x0F])
                }
            }
        }
    }
}
