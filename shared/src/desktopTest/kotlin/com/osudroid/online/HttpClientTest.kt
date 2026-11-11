package com.osudroid.online

import com.osudroid.beatmaplisting.mirrors.MirrorBeatmapSet
import com.osudroid.online.model.LeaderboardResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The game's client configuration ([configureGameClient]) over canned responses.
 */
class HttpClientTest {

    private var lastRequestBody = ""

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, contentType: String = "text/html; charset=UTF-8", body: String) =
        HttpClient(MockEngine { request ->
            lastRequestBody = request.body.toByteArray().decodeToString()
            respond(body, status, headersOf(HttpHeaders.ContentType, contentType))
        }) { configureGameClient() }


    @Serializable
    private class Room(val id: String, val name: String? = null, val players: Int = 0)

    @Serializable
    private class NewRoom(val name: String, val password: String?)


    @Test
    fun `json is read whatever the content type, ignoring unknown fields and coercing values`() = runBlocking {
        val room = client(body = """{ "id": 15, "name": null, "players": null, "extra": [1] }""").get("https://a.b").body<Room>()

        assertEquals("15", room.id)
        assertEquals(null, room.name)
        assertEquals(0, room.players)
    }

    @Test
    fun `request bodies leave nulls out`() = runBlocking {
        client(body = "{}").post("https://a.b") {
            contentType(ContentType.Application.Json)
            setBody(NewRoom("room", null))
        }

        assertEquals("""{"name":"room"}""", lastRequestBody)
    }

    @Test
    fun `an unsuccessful response throws`() {
        assertThrows(ClientRequestException::class.java) {
            runBlocking { client(HttpStatusCode.NotFound, body = "nope").get("https://a.b").body<String>() }
        }
    }

    @Test
    fun `the leaderboard and the mirrors are read`() = runBlocking {
        val leaderboard = client(body = """{ "Leaderboard": [ { "Id": "3", "Username": "a", "Mods": [], "Score": 10,
            "Pp": 1.5, "Combo": 2, "Accuracy": 0.9, "Avatar": null } ] }""").get("https://a.b").body<LeaderboardResponse>()
        assertEquals(3L, leaderboard.leaderboard[0].toModel().scoreId)
        assertEquals("", leaderboard.leaderboard[0].mark)

        val sets = client(contentType = "application/json", body = """[ { "id": 1, "title": "t", "title_unicode": "t", "artist": "a",
            "artist_unicode": "a", "ranked": 1, "creator": "c", "video": false, "covers": { "card": "x" }, "beatmaps": [
            { "id": 2, "version": "v", "difficulty_rating": 5.1, "ar": 9, "cs": 4, "drain": 5, "accuracy": 8, "bpm": 180,
              "hit_length": 90, "count_circles": 10, "count_sliders": 5, "count_spinners": 1, "mode": "osu" } ] } ]""")
            .get("https://a.b").body<List<MirrorBeatmapSet>>()
        val model = sets[0].toModel(sets[0].covers?.card)
        assertEquals("x", model.thumbnail)
        assertEquals(5.1, model.beatmaps[0].starRating, 0.0)
    }

    @Test
    fun `downloads write the file and a 304 leaves it`() = runBlocking {
        val file = Files.createTempFile("download", ".bin").toOkioPath()

        val progress = mutableListOf<DownloadProgress>()
        assertTrue(client(body = "content").downloadTo("https://a.b", file, onProgress = { progress += it }))
        assertEquals("content", okio.FileSystem.SYSTEM.read(file) { readUtf8() })
        assertEquals(7L, progress.last().bytesRead)

        assertFalse(client(HttpStatusCode.NotModified, body = "").downloadTo("https://a.b", file))
        assertEquals("content", okio.FileSystem.SYSTEM.read(file) { readUtf8() })

        assertThrows(Exception::class.java) { runBlocking { client(HttpStatusCode.NotFound, body = "x").downloadTo("https://a.b", file) } }
        okio.FileSystem.SYSTEM.delete(file, mustExist = false)
    }
}
