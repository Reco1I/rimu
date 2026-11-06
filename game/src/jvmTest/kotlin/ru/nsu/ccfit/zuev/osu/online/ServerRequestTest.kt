package ru.nsu.ccfit.zuev.osu.online

import com.osudroid.online.configureGameClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.SecurityUtils

/**
 * How [OnlineManager] posts to the osu!droid server, over canned responses.
 */
class ServerRequestTest {

    private val requests = mutableListOf<String>()

    private fun client(vararg responses: Pair<HttpStatusCode, String>): HttpClient {
        var next = 0
        return HttpClient(MockEngine { request ->
            requests += request.body.toByteArray().decodeToString()
            val (status, body) = responses[minOf(next++, responses.lastIndex)]
            respond(body, status)
        }) { configureGameClient() }
    }

    @After
    fun tearDown() {
        SecurityUtils.appSignature = null
    }


    @Test
    fun `fields are posted as a form with their signature`() = runBlocking {
        SecurityUtils.appSignature = "0123456789abcdef0123456789abcdef01234567"
        val fields = listOf("username" to "a b", "version" to "65")

        val lines = OnlineManager.getInstance().post("https://server/login.php", fields, client = client(HttpStatusCode.OK to "SUCCESS\n1 2 3\n"))

        assertEquals(listOf("SUCCESS", "1 2 3"), lines)
        val signature = SecurityUtils.signRequest("a+b_65")
        assertEquals("username=a+b&version=65&sign=$signature", requests.single())
    }

    @Test
    fun `a replay is sent as a multipart form`() = runBlocking {
        val replay = Files.createTempFile("replay", ".odr").also { Files.writeString(it, "replay data") }.toOkioPath()

        OnlineManager.getInstance().post("https://server/submit.php", listOf("hash" to "abc"), replay, client(HttpStatusCode.OK to "SUCCESS"))

        val body = requests.single()
        assertTrue(body, "name=hash" in body && "abc" in body)
        assertTrue(body, "name=replayFile" in body && "filename=\"${replay.name}\"" in body && "replay data" in body)
        Files.delete(replay.toNioPath())
    }

    @Test
    fun `it's tried again until the server answers`() = runBlocking {
        val lines = OnlineManager.getInstance().post("https://server/gettop.php", listOf("playID" to "1"),
            client = client(HttpStatusCode.InternalServerError to "oops", HttpStatusCode.OK to "FAIL\nNo such play"))

        assertEquals(listOf("FAIL", "No such play"), lines)
        assertEquals(2, requests.size)
    }
}
