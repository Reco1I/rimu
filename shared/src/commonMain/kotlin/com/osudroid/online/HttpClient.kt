package com.osudroid.online

import com.osudroid.utils.LenientJson
import com.reco1l.verktex.Platform
import com.reco1l.verktex.network.Network
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readAvailable
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import okio.Path
import okio.buffer
import okio.use

/**
 * How the servers' JSON is read: as leniently as [LenientJson], ignoring the fields the game
 * doesn't declare, and with the declared defaults for missing or null ones.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
val NetworkJson = Json(LenientJson) {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * The game's HTTP client, over Verktex's [Network]. Requests are suspend functions, made from a
 * coroutine (e.g. [com.osudroid.utils.async]):
 *
 * ```
 * val update = httpClient.get(url) { parameter("buildType", type) }.body<UpdateInfo>()
 * ```
 *
 * Bodies are read and written as `@Serializable` classes with [NetworkJson], whatever content type
 * the server says (some answer JSON as `text/html`). An unsuccessful response throws a
 * [ResponseException].
 */
val httpClient: HttpClient by lazy { Network.createClient { configureGameClient() } }

/**
 * How [httpClient] is configured, also for clients over other engines in tests.
 */
internal fun HttpClientConfig<*>.configureGameClient() {
    expectSuccess = true

    install(ContentNegotiation) {
        json(NetworkJson, ContentType.Any)
    }
}


/**
 * How far a download of [downloadTo] is.
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
 * Downloads [url] into [path], streaming it, and calls [onProgress] as it goes. A download that
 * doesn't finish (it fails or its coroutine is cancelled) leaves no file behind.
 *
 * @return Whether the file was downloaded: false for a `304 Not Modified`, e.g. after an
 * `If-Modified-Since` header.
 * @throws ResponseException If the response is unsuccessful.
 */
suspend fun HttpClient.downloadTo(
    url: String,
    path: Path,
    configure: HttpRequestBuilder.() -> Unit = {},
    onProgress: (DownloadProgress) -> Unit = {},
): Boolean = prepareGet(url) {
    // A 304 isn't a failure here.
    expectSuccess = false
    configure()
}.execute { response ->
    if (response.status == HttpStatusCode.NotModified) {
        return@execute false
    }
    if (response.status.value !in 200..299) {
        throw ResponseException(response, "Download of $url failed: ${response.status}")
    }

    val total = response.contentLength() ?: -1L
    val channel = response.bodyAsChannel()
    val buffer = ByteArray(8192)
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

    true
}
