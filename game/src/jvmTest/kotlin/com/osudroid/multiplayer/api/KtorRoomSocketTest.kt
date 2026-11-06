package com.osudroid.multiplayer.api

import com.reco1l.verktex.Platform
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Talks to a real Socket.IO v4 server, given with `-PsocketIoTestUrl`, which must behave as this:
 * namespaces like `/123`, accepting the
 * `auth` with uid 42, sending `initialConnection` on connection, echoing `chatMessage` as `echo`, and
 * disconnecting on `kickMe`, and pinging every 300 ms with a 300 ms timeout. Skipped without one.
 */
class KtorRoomSocketTest {

    private val serverUrl: String? = System.getProperty("socketIoTestUrl")

    private val events = LinkedBlockingQueue<Pair<String, List<Any?>>>()

    @Before
    fun setUp() {
        assumeTrue("No Socket.IO server given", serverUrl != null)

        Platform.implementation = object : Platform.Implementation {
            override val provider get() = throw UnsupportedOperationException()
            override val audioDriver get() = throw UnsupportedOperationException()
            override val storage = Platform.Storage("/tmp".toPath(), "/tmp".toPath(), null)
            override val currentDisplay = Platform.Display(1f)
            override val usesVirtualKeyboard = false
            override val uptimeMillis = 0L
            override val httpEngine = OkHttp
            override fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit) = throw UnsupportedOperationException()
            override fun openWebPage(url: String, userAgent: String?) = Unit
            override fun openUrl(url: String) = Unit
        }
    }

    private fun socket(uid: String) = KtorRoomSocket("$serverUrl/123", mapOf("uid" to uid, "gameSessionID" to "abc")).apply {
        for (event in listOf(RoomSocket.EVENT_CONNECT, RoomSocket.EVENT_CONNECT_ERROR, RoomSocket.EVENT_DISCONNECT, "initialConnection", "echo")) {
            on(event) { events += event to it.toList() }
        }
    }

    private fun nextEvent() = events.poll(10, TimeUnit.SECONDS) ?: throw AssertionError("No event in time")


    @Test
    fun `connects to the namespace with the auth and exchanges events`() {
        val socket = socket("42")
        socket.connect()

        assertEquals(RoomSocket.EVENT_CONNECT, nextEvent().first)

        val (name, args) = nextEvent()
        assertEquals("initialConnection", name)
        val info = args[0] as JsonObject
        assertEquals(JsonPrimitive("/123"), info["room"])
        assertEquals(JsonPrimitive("abc"), (info["auth"] as JsonObject)["gameSessionID"])
        assertEquals(listOf("text", 7, true, null), args.subList(1, 5))
        assertEquals(JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))), args[5])

        val payload = buildJsonObject { put("text", "héllo, \"world\"") }
        socket.emit("chatMessage", "plain", 3, payload)
        assertEquals("echo" to listOf("plain", 3, payload), nextEvent())

        socket.emit("kickMe")
        assertEquals(RoomSocket.EVENT_DISCONNECT to listOf("io server disconnect"), nextEvent())
    }

    @Test
    fun `a refused connection is a connect error`() {
        socket("1").connect()

        val (name, args) = nextEvent()
        assertEquals(RoomSocket.EVENT_CONNECT_ERROR, name)
        assertEquals("bad auth", ((args[0] as JsonObject)["message"] as JsonPrimitive).content)
    }

    @Test
    fun `disconnecting tells the listeners`() {
        val socket = socket("42")
        socket.connect()
        assertEquals(RoomSocket.EVENT_CONNECT, nextEvent().first)
        assertEquals("initialConnection", nextEvent().first)

        socket.disconnect()
        assertEquals(RoomSocket.EVENT_DISCONNECT to listOf("io client disconnect"), nextEvent())
    }

    @Test
    fun `answering the pings keeps the connection`() {
        val socket = socket("42")
        socket.connect()
        assertEquals(RoomSocket.EVENT_CONNECT, nextEvent().first)
        assertEquals("initialConnection", nextEvent().first)

        // Several ping intervals, without an answer the server would have closed it.
        Thread.sleep(2000)

        socket.emit("chatMessage", "still here")
        assertEquals("echo" to listOf("still here"), nextEvent())
        socket.disconnect()
    }
}
