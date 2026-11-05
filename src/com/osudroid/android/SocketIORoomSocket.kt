package com.osudroid.android

import com.osudroid.multiplayer.api.RoomSocket
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * A [RoomSocket] over the socket.io Java client, which exchanges org.json values.
 */
class SocketIORoomSocket(private val socket: Socket) : RoomSocket {

    override fun on(event: String, listener: (Array<out Any?>) -> Unit) {
        socket.on(event) { args -> listener(Array(args.size) { fromOrgJson(args[it]) }) }
    }

    override fun off() {
        socket.off()
    }

    override fun emit(event: String, vararg args: Any?) {
        socket.emit(event, *Array(args.size) { toOrgJson(args[it]) })
    }

    override fun connect() {
        socket.connect()
    }

    override fun disconnect() {
        socket.disconnect()
    }


    companion object {

        /**
         * Creates sockets with the socket.io Java client.
         */
        @JvmField
        val factory = RoomSocket.Factory { url, auth ->
            SocketIORoomSocket(IO.socket(url, IO.Options().also {
                it.auth = auth

                // Explicitly not allow the socket to reconnect as we are using our own
                // reconnection system (the socket.io Java client does not support connection
                // state recovery).
                it.reconnection = false
            }))
        }


        @JvmStatic
        fun fromOrgJson(value: Any?): Any? = when (value) {
            is JSONObject -> toJsonElement(value)
            is JSONArray -> toJsonElement(value)
            JSONObject.NULL -> null
            else -> value
        }

        @JvmStatic
        fun toOrgJson(value: Any?): Any? = when (value) {
            is JsonObject -> JSONObject(value.toString())
            is JsonArray -> JSONArray(value.toString())
            is JsonNull -> JSONObject.NULL
            is JsonPrimitive -> if (value.isString) value.content else value.booleanOrNull ?: value.longOrNull ?: value.doubleOrNull
            else -> value
        }

        private fun toJsonElement(value: Any?): JsonElement = when (value) {
            null, JSONObject.NULL -> JsonNull
            is JSONObject -> JsonObject(value.keys().asSequence().associateWith { toJsonElement(value.opt(it)) })
            is JSONArray -> JsonArray(List(value.length()) { toJsonElement(value.opt(it)) })
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    }
}
