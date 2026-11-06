package com.osudroid.debug

import com.osudroid.settings.OsuSettings
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomGameplaySettings
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomSocket
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Creates a mock multiplayer room.
 */
fun MockRoom() = Room(
    id = 1,
    name = "Test Room",
    isLocked = false,
    maxPlayers = 8,
    mods = RoomMods(),
    gameplaySettings = RoomGameplaySettings(isFreeMod = true, isRemoveSliderLock = false),
    teamMode = TeamMode.HeadToHead,
    winCondition = WinCondition.ScoreV1,
    playerCount = 0,
    playerNames = "",
    status = RoomStatus.Idle
)

/**
 * Creates a mock multiplayer socket.
 */
class MockSocket(private val uid: Long) : RoomSocket {

    private val listeners = mutableMapOf<String, MutableList<(Array<out Any?>) -> Unit>>()


    override fun on(event: String, listener: (Array<out Any?>) -> Unit) {
        listeners.getOrPut(event) { mutableListOf() } += listener
    }

    override fun off() {
        listeners.clear()
    }

    override fun emit(event: String, vararg args: Any?) {

        // Emulating the server response to the client's events.
        val responseEvent: Pair<String, Array<out Any?>> = when (event) {

            // In these events the client expects a JSONArray as a response.
            "scoreSubmission" -> "allPlayersScoreSubmitted" to arrayOf(JsonArray(listOf(args[0] as JsonElement)))
            "liveScoreData" -> "liveScoreData" to arrayOf(JsonArray(listOf(
                // The username is expected to be added by the server.
                JsonObject((args[0] as JsonObject) + ("username" to JsonPrimitive(OsuSettings.onlineUsername)))
            )))

            // Simulating the server response when all players emit these events.
            "beatmapLoadComplete" -> "allPlayersBeatmapLoadComplete" to arrayOf()
            "skipRequested" -> "allPlayersSkipRequested" to arrayOf()

            // In these events the client expects to receive the uid of the player as the first argument.
            "chatMessage" -> "chatMessage" to arrayOf(uid.toString(), args[0])
            "teamChanged" -> "teamChanged" to arrayOf(uid.toString(), args[0])
            "playerModsChanged" -> "playerModsChanged" to arrayOf(uid.toString(), args[0])
            "playerStatusChanged" -> "playerStatusChanged" to arrayOf(uid.toString(), args[0])

            // In these events, the client expects to receive an integer as the first argument.
            "maxPlayersChanged" -> "maxPlayersChanged" to arrayOf(args[0].toString())

            // These events require special handling
            "roomGameplaySettingsChanged" -> "roomGameplaySettingsChanged" to arrayOf((args[0] as JsonObject).let { json ->
                Multiplayer.room!!.gameplaySettings.let {
                    buildJsonObject {
                        put("isFreeMod", it.isFreeMod)
                        put("isRemoveSliderLock", it.isRemoveSliderLock)
                        json.forEach { (key, value) -> put(key, value) }
                    }
                }
            })

            else -> event to args
        }


        val (key, arguments) = responseEvent

        listeners[key]?.toList()?.forEach {
            it(arguments)
        }
    }


    override fun connect() {
        emit(RoomSocket.EVENT_CONNECT)

        // This event is emitted by the server when the client connects.
        emit("initialConnection", buildJsonObject {

            put("id", 1)
            put("name", "Test room")
            put("isLocked", false)
            put("maxPlayers", 8)
            put("teamMode", TeamMode.HeadToHead.ordinal)
            put("winCondition", WinCondition.ScoreV1.ordinal)
            put("playerCount", 1)
            put("playerNames", OsuSettings.onlineUsername)
            put("sessionId", "")
            put("status", RoomStatus.Idle.ordinal)
            put("beatmap", JsonNull)

            put("host", buildJsonObject {
                put("id", uid)
            })

            put("mods", JsonArray(emptyList()))

            put("gameplaySettings", buildJsonObject {
                put("isFreeMod", true)
                put("isRemoveSliderLock", false)
            })

            put("players", buildJsonArray {
                add(buildJsonObject {
                    put("id", uid)
                    put("username", OsuSettings.onlineUsername)
                    put("rank", 1)
                    put("status", PlayerStatus.NotReady.ordinal)
                    put("team", JsonNull)
                    put("mods", JsonArray(emptyList()))
                })
            })

        })
    }

    override fun disconnect() {
        emit(RoomSocket.EVENT_DISCONNECT, "io server disconnect")
    }

}
