package com.osudroid.multiplayer.api

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray

import com.osudroid.BuildSettings
import com.osudroid.debug.MockRoom
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.parseGameplaySettings
import com.osudroid.online.httpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

object LobbyAPI {

    /**
     * The hostname.
     */
    const val HOST = "https://multi.osudroid.moe"

    /**
     * The invite link host.
     */
    const val INVITE_HOST = "https://odmp"


    /**
     * Endpoint to get a rooms list.
     */
    private const val GET_ROOMS = "/getrooms"

    /**
     * Endpoint to create a room.
     */
    private const val CREATE_ROOM = "/createroom"


    /**
     * Get room list.
     */
    suspend fun getRooms(query: String?, uid: Long, sessionId: String, sign: String?): List<Room> {

        if (BuildSettings.MOCK_MULTIPLAYER) {
            return listOf(
                MockRoom(),
                MockRoom(),
                MockRoom()
            )
        }

        // Read as JSON, the rooms are made as the socket's events make them.
        val rooms = httpClient.get("$HOST$GET_ROOMS") {
            if (sign != null || query != null) {
                url {
                    // A null value is sent as a name alone, as it always was.
                    parameters.appendAll("sign", listOfNotNull(sign))
                    parameters.appendAll("query", listOfNotNull(query))
                    parameters.append("uid", uid.toString())
                    parameters.append("sessionId", sessionId)
                }
            }
        }.body<JsonArray>()

        return List(rooms.size) { i ->

            val json = rooms.optJsonObject(i) ?: return@List null

            try {
                Room(
                    id = json.getLong("id"),
                    name = json.getString("name"),
                    isLocked = json.getBoolean("isLocked"),
                    maxPlayers = json.getInt("maxPlayers"),
                    mods = RoomMods(json.getJsonArray("mods").toString()),
                    gameplaySettings = parseGameplaySettings(json.getJsonObject("gameplaySettings")),
                    teamMode = TeamMode.fromWire(json.getString("teamMode")) ?: TeamMode.HeadToHead,
                    winCondition = WinCondition.fromWire(json.getString("winCondition")) ?: WinCondition.ScoreV1,
                    playerCount = json.getInt("playerCount"),
                    playerNames = json.optString("playerNames", ""),
                    status = RoomStatus.fromWire(json.getString("status"))
                )
            } catch (_: Exception) {
                null
            }

        }.filterNotNull()
    }

    /**
     * Create room and get the ID.
     */
    suspend fun createRoom(name: String, beatmap: RoomBeatmap?, hostUID: Long, sessionId: String, sign: String?, password: String? = null, maxPlayers: Int = 8): Long {

        if (BuildSettings.MOCK_MULTIPLAYER) {
            return 1
        }

        val request = CreateRoomRequest(
            hostUid = hostUID,
            name = name,
            maxPlayers = maxPlayers,
            beatmap = beatmap?.let { CreateRoomRequest.Beatmap(it.md5, it.title, it.artist, it.creator, it.version) },
            password = password?.takeUnless { it.isBlank() },
            version = RoomAPI.API_VERSION,
            sessionId = sessionId,
            sign = sign,
        )

        return httpClient.post("$HOST$CREATE_ROOM") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CreateRoomResponse>().id.toLong()
    }


    /**
     * The room to create, as the server takes it. Null values are left out.
     */
    @Serializable
    private class CreateRoomRequest(
        val hostUid: Long,
        val name: String,
        val maxPlayers: Int,
        val beatmap: Beatmap?,
        val password: String?,
        val version: Int,
        val sessionId: String,
        val sign: String?,
    ) {
        @Serializable
        class Beatmap(val md5: String?, val title: String?, val artist: String?, val creator: String?, val version: String?)
    }

    /**
     * The room created, of which only the id is read. It's a number the server may send as text.
     */
    @Serializable
    private class CreateRoomResponse(val id: String)
}
