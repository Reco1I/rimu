package com.osudroid.multiplayer.api

/**
 * A Socket.IO connection to a multiplayer room, made by the platform's client.
 *
 * Event arguments are strings, numbers, booleans, null, and kotlinx `JsonObject`s and `JsonArray`s.
 */
interface RoomSocket {

    /**
     * Calls [listener] with the arguments of each [event] received.
     */
    fun on(event: String, listener: (Array<out Any?>) -> Unit)

    /**
     * Removes all the listeners.
     */
    fun off()

    /**
     * Sends [event] with [args].
     */
    fun emit(event: String, vararg args: Any?)

    fun connect()

    fun disconnect()


    /**
     * Creates the sockets of the platform.
     */
    fun interface Factory {

        /**
         * Creates a socket to [url] that sends [auth] when connecting and doesn't reconnect by itself.
         */
        fun create(url: String, auth: Map<String, String>): RoomSocket
    }

    companion object {
        const val EVENT_CONNECT = "connect"
        const val EVENT_CONNECT_ERROR = "connect_error"
        const val EVENT_DISCONNECT = "disconnect"

        /**
         * The factory of the current platform.
         */
        lateinit var factory: Factory

        /**
         * Whether the current platform has set a [factory], without which there's no multiplayer.
         */
        val isAvailable
            get() = ::factory.isInitialized
    }
}
