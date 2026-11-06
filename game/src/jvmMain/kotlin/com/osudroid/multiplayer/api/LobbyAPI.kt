package com.osudroid.multiplayer.api

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import com.osudroid.BuildSettings
import com.osudroid.debug.MockRoom
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.parseGameplaySettings
import com.osudroid.online.OsuHttp

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

        val rooms = OsuHttp.getJsonArray("$HOST$GET_ROOMS") {
            if (sign != null || query != null) {
                url {
                    // A null value is sent as a name alone, as it always was.
                    parameters.appendAll("sign", listOfNotNull(sign))
                    parameters.appendAll("query", listOfNotNull(query))
                    parameters.append("uid", uid.toString())
                    parameters.append("sessionId", sessionId)
                }
            }
        }

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

        val body = buildJsonObject {
            put("hostUid", hostUID)
            put("name", name)
            put("maxPlayers", maxPlayers)

            if (beatmap != null) {
                put("beatmap", buildJsonObject {
                    putIfNotNull("md5", beatmap.md5)
                    putIfNotNull("title", beatmap.title)
                    putIfNotNull("artist", beatmap.artist)
                    putIfNotNull("creator", beatmap.creator)
                    putIfNotNull("version", beatmap.version)
                })
            }

            if (!password.isNullOrBlank()) {
                put("password", password)
            }

            put("version", RoomAPI.API_VERSION)
            put("sessionId", sessionId)
            putIfNotNull("sign", sign)
        }

        return OsuHttp.postJson("$HOST$CREATE_ROOM", body).getString("id").toLong()
    }
}
