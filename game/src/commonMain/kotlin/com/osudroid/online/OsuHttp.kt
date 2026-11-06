package com.osudroid.online

import com.osudroid.OsuPlatform
import com.osudroid.utils.JsonException
import com.osudroid.utils.LenientJson
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.network.Network
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.content.TextContent
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlin.time.TimeSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okio.IOException
import okio.Path
import okio.buffer
import okio.use

/**
 * The game's HTTP client and the requests it makes, over Verktex's [Network]. Every request is a
 * suspend function, made from a coroutine (e.g. [com.osudroid.utils.async]).
 */
object OsuHttp {

    /**
     * The user agent the beatmap mirrors are asked with.
     */
    const val MIRROR_USER_AGENT = "Chrome/Android"

    /**
     * The client, which doesn't throw on unsuccessful responses: the helpers below check them.
     */
    val client: HttpClient by lazy {
        Network.createClient {
            expectSuccess = false
        }
    }


    /**
     * Gets [url] as a JSON object.
     *
     * @throws HttpStatusException If the response isn't successful.
     * @throws JsonException If it isn't a JSON object.
     */
    suspend fun getJsonObject(url: String, configure: HttpRequestBuilder.() -> Unit = {}): JsonObject =
        parseObject(client.get(url, configure).successfulText())

    /**
     * Gets [url] as a JSON array.
     *
     * @throws HttpStatusException If the response isn't successful.
     * @throws JsonException If it isn't a JSON array.
     */
    suspend fun getJsonArray(url: String, configure: HttpRequestBuilder.() -> Unit = {}): JsonArray =
        parseArray(client.get(url, configure).successfulText())

    /**
     * Posts [body] as JSON to [url], and reads the response as a JSON object.
     *
     * @throws HttpStatusException If the response isn't successful.
     * @throws JsonException If it isn't a JSON object.
     */
    suspend fun postJson(url: String, body: JsonObject): JsonObject {
        val response = client.post(url) {
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }
        return parseObject(response.successfulText())
    }

    /**
     * Posts [fields] as a URL encoded form to [url], returning the response's text.
     *
     * @throws HttpStatusException If the response isn't successful.
     */
    suspend fun postForm(url: String, fields: List<Pair<String, String>>): String {
        val parameters = Parameters.build { fields.forEach { (name, value) -> append(name, value) } }
        return client.submitForm(url, parameters).successfulText()
    }

    /**
     * Posts [parts] as a multipart form to [url], returning the response's text.
     *
     * @throws HttpStatusException If the response isn't successful.
     */
    suspend fun postMultipart(url: String, parts: List<FormPart>): String {
        val data = formData {
            for (part in parts) {
                when (part) {
                    is FormPart.Value -> append(part.name, part.value)
                    is FormPart.File -> append(part.name, Platform.fileSystem.read(part.path) { readByteArray() }, Headers.build {
                        append(HttpHeaders.ContentType, part.contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${part.filename}\"")
                    })
                }
            }
        }
        return client.submitFormWithBinaryData(url, data).successfulText()
    }

    /**
     * Gets [url] as bytes.
     *
     * @throws HttpStatusException If the response isn't successful.
     */
    suspend fun getBytes(url: String, configure: HttpRequestBuilder.() -> Unit = {}): ByteArray {
        val response = client.get(url, configure)
        response.checkSuccess()
        return response.readRawBytes()
    }

    /**
     * Downloads [url] into [path], streaming it, and calls [onProgress] as it goes. A download that
     * doesn't finish (it fails or its coroutine is cancelled) leaves no file behind.
     *
     * @return The response, whose body was read, e.g. to check for a `304 Not Modified`.
     * @throws HttpStatusException If the response isn't successful, other than a 304.
     */
    suspend fun download(
        url: String,
        path: Path,
        configure: HttpRequestBuilder.() -> Unit = {},
        onProgress: (DownloadProgress) -> Unit = {},
    ): HttpResponse = client.prepareGet(url, configure).execute { response ->
        if (response.status.value == 304) {
            return@execute response
        }
        response.checkSuccess()

        val total = response.contentLength() ?: -1L
        val channel = response.bodyAsChannel()
        val buffer = ByteArray(BUFFER_SIZE)
        val start = TimeSource.Monotonic.markNow()
        var read = 0L
        var isComplete = false

        try {
            Platform.fileSystem.sink(path).buffer().use { sink ->
                while (true) {
                    val count = channel.readAvailable(buffer, 0, buffer.size)
                    if (count == -1) break

                    sink.write(buffer, 0, count)
                    read += count

                    val seconds = start.elapsedNow().inWholeMilliseconds / 1000f
                    onProgress(DownloadProgress(read, total, if (seconds > 0f) read / 1024f / seconds else 0f))
                }
            }
            isComplete = true
        } finally {
            if (!isComplete) {
                Platform.fileSystem.delete(path, mustExist = false)
            }
        }

        response
    }


    private suspend fun HttpResponse.successfulText(): String {
        checkSuccess()
        return bodyAsText()
    }

    private fun HttpResponse.checkSuccess() {
        if (!status.isSuccess()) {
            throw HttpStatusException(status.value, status.description)
        }
    }

    private fun parseObject(text: String): JsonObject {
        val trimmed = text.trim()
        if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) {
            throw JsonException("Response is not a JSON object:\n$text")
        }
        val json = LenientJson.parseToJsonElement(trimmed) as? JsonObject ?: throw JsonException("Response is not a JSON object:\n$text")
        log(json)
        return json
    }

    private fun parseArray(text: String): JsonArray {
        val trimmed = text.trim()
        if (!trimmed.startsWith('[') || !trimmed.endsWith(']')) {
            throw JsonException("Response is not a JSON array:\n$text")
        }
        val json = LenientJson.parseToJsonElement(trimmed) as? JsonArray ?: throw JsonException("Response is not a JSON array:\n$text")
        log(json)
        return json
    }

    private fun log(json: Any) {
        if (OsuPlatform.isInitialized && OsuPlatform.implementation.isDebug) {
            Logger.i("OsuHttp", "Received JSON: $json")
        }
    }

    private const val BUFFER_SIZE = 8192
}

/**
 * A part of a multipart form, see [OsuHttp.postMultipart].
 */
sealed class FormPart(val name: String) {

    class Value(name: String, val value: String) : FormPart(name)

    class File(name: String, val filename: String, val contentType: String, val path: Path) : FormPart(name)
}

/**
 * How far a download of [OsuHttp.download] is.
 *
 * @param bytesRead The bytes downloaded so far.
 * @param totalBytes The size of the file, or -1 if the server didn't tell.
 * @param speedKbps The average speed, in kilobytes per second.
 */
class DownloadProgress(val bytesRead: Long, val totalBytes: Long, val speedKbps: Float) {

    /**
     * The percentage downloaded, from 0 to 100, or -1 if the size is unknown.
     */
    val percentage: Double
        get() = if (totalBytes > 0) bytesRead * 100.0 / totalBytes else -1.0
}

/**
 * A response whose status isn't successful.
 */
class HttpStatusException(val code: Int, message: String) : IOException("HTTP $code $message")
