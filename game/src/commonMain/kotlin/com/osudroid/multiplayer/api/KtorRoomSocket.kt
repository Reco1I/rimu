package com.osudroid.multiplayer.api

import com.reco1l.verktex.Logger
import com.reco1l.verktex.network.Network
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * A [RoomSocket] speaking Socket.IO v4 (Engine.IO v4) over a Ktor WebSocket, on every platform.
 *
 * It covers what the rooms use: connecting to the namespace in the URL's path with an `auth`
 * payload, events with JSON arguments, the server's pings, and disconnections. It doesn't
 * reconnect by itself, acknowledge events or exchange binary data.
 *
 * Listeners are called from the connection's coroutine, one event at a time, in order.
 */
class KtorRoomSocket(private val url: String, private val auth: Map<String, String>) : RoomSocket {

    private val lock = SynchronizedObject()

    private var listeners = mapOf<String, List<(Array<out Any?>) -> Unit>>()

    private val namespace = Url(url).encodedPath.ifEmpty { "/" }

    // Written before a namespace's packets, none for the main one.
    private val namespacePrefix = if (namespace == "/") "" else "$namespace,"

    // The packets to send, in order.
    private val pending = Channel<String>(Channel.UNLIMITED)

    private var job: Job? = null

    @Volatile
    private var isConnected = false

    // Why the connection ended, given to the disconnect listeners.
    @Volatile
    private var disconnectReason = "transport close"


    override fun on(event: String, listener: (Array<out Any?>) -> Unit) {
        synchronized(lock) {
            listeners = listeners + (event to (listeners[event].orEmpty() + listener))
        }
    }

    override fun off() {
        synchronized(lock) { listeners = emptyMap() }
    }

    override fun emit(event: String, vararg args: Any?) {
        val packet = JsonArray(listOf(JsonPrimitive(event)) + args.map(::toJson))
        pending.trySend("$MESSAGE$EVENT$namespacePrefix$packet")
    }

    override fun connect() {
        if (job != null) return
        job = scope.launch { run() }
    }

    override fun disconnect() {
        if (job == null) return
        disconnectReason = "io client disconnect"

        // The sender closes the WebSocket once the channel is drained.
        if (isConnected) {
            pending.trySend("$MESSAGE$DISCONNECT$namespacePrefix")
        }
        pending.close()
    }


    private suspend fun run() {
        val target = Url(url)
        val scheme = if (target.protocol == URLProtocol.HTTPS) "wss" else "ws"
        val endpoint = "$scheme://${target.host}:${target.port}/socket.io/?EIO=4&transport=websocket"

        try {
            client.webSocket(endpoint) {
                var lastPing = TimeSource.Monotonic.markNow()
                var pingDeadline = DEFAULT_PING_DEADLINE

                val sender = launch {
                    for (message in pending) {
                        send(Frame.Text(message))
                    }
                    close()
                }

                // The server pings every pingInterval, a connection silent for longer is dead.
                val watchdog = launch {
                    while (isActive) {
                        delay(1000)
                        if (lastPing.elapsedNow() > pingDeadline) {
                            disconnectReason = "ping timeout"
                            close()
                        }
                    }
                }

                try {
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()

                        when (text.firstOrNull()) {
                            OPEN -> {
                                val handshake = Json.parseToJsonElement(text.substring(1)).jsonObject
                                val interval = handshake["pingInterval"]?.jsonPrimitive?.longOrNull ?: 25000
                                val timeout = handshake["pingTimeout"]?.jsonPrimitive?.longOrNull ?: 20000
                                pingDeadline = (interval + timeout).milliseconds
                                lastPing = TimeSource.Monotonic.markNow()

                                val payload = JsonObject(auth.mapValues { JsonPrimitive(it.value) })
                                send(Frame.Text("$MESSAGE$CONNECT$namespacePrefix$payload"))
                            }

                            PING -> {
                                lastPing = TimeSource.Monotonic.markNow()
                                send(Frame.Text(PONG.toString()))
                            }

                            CLOSE -> close()

                            MESSAGE -> if (!onPacket(text.substring(1))) close()
                        }
                    }
                } finally {
                    sender.cancel()
                    watchdog.cancel()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isConnected) {
                Logger.e("KtorRoomSocket", "Couldn't connect to $url", e)
                dispatch(RoomSocket.EVENT_CONNECT_ERROR, arrayOf(e.message ?: e.toString()))
                return
            }
            disconnectReason = "transport error"
        }

        if (isConnected) {
            isConnected = false
            dispatch(RoomSocket.EVENT_DISCONNECT, arrayOf(disconnectReason))
        }
    }

    /**
     * Handles a Socket.IO packet, returning whether the connection goes on.
     */
    private fun onPacket(packet: String): Boolean {
        val type = packet.firstOrNull() ?: return true
        var i = 1

        // The namespace, when it isn't the main one.
        if (packet.getOrNull(i) == '/') {
            val end = packet.indexOf(',', i)
            val packetNamespace = if (end < 0) packet.substring(i) else packet.substring(i, end)
            if (packetNamespace != namespace) return true
            i = if (end < 0) packet.length else end + 1
        }

        // An acknowledgement id, which this client doesn't answer.
        while (i < packet.length && packet[i].isDigit()) i++

        val data = packet.substring(i).takeIf { it.isNotEmpty() }?.let(Json::parseToJsonElement)

        when (type) {
            CONNECT -> {
                isConnected = true
                dispatch(RoomSocket.EVENT_CONNECT, emptyArray())
            }

            DISCONNECT -> {
                disconnectReason = "io server disconnect"
                return false
            }

            EVENT -> {
                val array = data as? JsonArray ?: return true
                val event = (array.firstOrNull() as? JsonPrimitive)?.content ?: return true
                dispatch(event, Array(array.size - 1) { fromJson(array[it + 1]) })
            }

            CONNECT_ERROR -> {
                dispatch(RoomSocket.EVENT_CONNECT_ERROR, arrayOf(data?.let(::fromJson)))
                return false
            }
        }
        return true
    }

    private fun dispatch(event: String, args: Array<out Any?>) {
        val listeners = synchronized(lock) { listeners[event] } ?: return
        for (listener in listeners) {
            try {
                listener(args)
            } catch (e: Exception) {
                Logger.e("KtorRoomSocket", "A listener of \"$event\" failed", e)
            }
        }
    }


    companion object {

        /**
         * Creates [KtorRoomSocket]s.
         */
        val factory = RoomSocket.Factory { url, auth -> KtorRoomSocket(url, auth) }

        private val client: HttpClient by lazy { Network.createClient { install(WebSockets) } }

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        private val DEFAULT_PING_DEADLINE = 45000.milliseconds

        // Engine.IO packet types.
        private const val OPEN = '0'
        private const val CLOSE = '1'
        private const val PING = '2'
        private const val PONG = '3'
        private const val MESSAGE = '4'

        // Socket.IO packet types.
        private const val CONNECT = '0'
        private const val DISCONNECT = '1'
        private const val EVENT = '2'
        private const val CONNECT_ERROR = '4'

        /**
         * [value] as the listeners get it: strings, numbers, booleans and null as themselves, objects
         * and arrays as [JsonObject]s and [JsonArray]s.
         */
        internal fun fromJson(value: JsonElement): Any? = when (value) {
            JsonNull -> null
            is JsonPrimitive -> if (value.isString) value.content else value.booleanOrNull ?: value.intOrNull ?: value.longOrNull ?: value.doubleOrNull
            else -> value
        }

        internal fun toJson(value: Any?): JsonElement = when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    }
}
