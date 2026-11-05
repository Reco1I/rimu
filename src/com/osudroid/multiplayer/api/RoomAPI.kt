package com.osudroid.multiplayer.api

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray

import kotlinx.serialization.json.JsonObject
import com.osudroid.BuildSettings
import com.osudroid.debug.MockSocket
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.Room
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomStatus
import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.TeamMode
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.multiplayer.api.data.parseBeatmap
import com.osudroid.multiplayer.api.data.parseGameplaySettings
import com.osudroid.multiplayer.api.data.parsePlayer
import com.osudroid.multiplayer.api.data.parsePlayers
import com.osudroid.ui.v2.multi.RoomScene
import com.osudroid.utils.updateThread
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

object RoomAPI {

    private inline fun Listener(noinline block: (Array<out Any?>) -> Unit) = block

    /**
     * The API version.
     */
    const val API_VERSION = 10


    /**
     * The listener for player events.
     */
    var playerEventListener: IPlayerEventListener? = null

    /**
     * The listener for room events.
     */
    var roomEventListener: IRoomEventListener? = null


    @Volatile
    private var socket: RoomSocket? = null

    @Volatile
    internal var lastRoomPassword: String? = null


    // https://gist.github.com/Rian8337/ceab4d3b179cbeee7dd548cfcf145b95
    // Back-to-back events

    private val beatmapChanged = Listener {
        Multiplayer.log("RECEIVED: beatmapChanged -> ${it.contentToString()}")

        val json = it[0] as? JsonObject
        val beatmap = parseBeatmap(json)

        roomEventListener?.onRoomBeatmapChange(beatmap)
    }

    private val hostChanged = Listener {

        Multiplayer.log("RECEIVED: hostChanged -> ${it.contentToString()}")
        val uid = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: hostChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomHostChange(uid)
    }

    private val playerKicked = Listener {

        Multiplayer.log("RECEIVED: playerKicked -> ${it.contentToString()}")
        val uid = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerKicked — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        playerEventListener?.onPlayerKick(uid)
    }

    private val playerModsChanged = Listener {
        Multiplayer.log("RECEIVED: playerModsChanged -> ${it.contentToString()}")

        val id = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerModsChanged — unexpected id type: ${it.contentToString()}")
            return@Listener
        }
        val mods = it[1] as? JsonArray ?: run {
            Multiplayer.log("WARNING: playerModsChanged — unexpected mods type: ${it.contentToString()}")
            return@Listener
        }

        playerEventListener?.onPlayerModsChange(id, RoomMods(mods.toString()))
    }

    private val roomModsChanged = Listener {
        Multiplayer.log("RECEIVED: roomModsChanged -> ${it.contentToString()}")

        val mods = it[0] as? JsonArray ?: run {
            Multiplayer.log("WARNING: roomModsChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomModsChange(RoomMods(mods.toString()))
    }

    private val roomGameplaySettingsChanged = Listener {
        Multiplayer.log("RECEIVED: roomGameplaySettingsChanged -> ${it.contentToString()}")

        val json = it[0] as? JsonObject ?: run {
            Multiplayer.log("WARNING: roomGameplaySettingsChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomGameplaySettingsChange(parseGameplaySettings(json))
    }

    private val playerStatusChanged = Listener {
        //Multiplayer.log("RECEIVED: playerStatusChanged -> ${it.contentToString()}")

        val id = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unexpected id type: ${it.contentToString()}")
            return@Listener
        }
        val statusString = it[1] as? String ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unexpected status type: ${it.contentToString()}")
            return@Listener
        }
        val status = PlayerStatus.fromWire(statusString) ?: run {
            Multiplayer.log("WARNING: playerStatusChanged — unknown PlayerStatus value $statusString, ignoring event")
            return@Listener
        }

        playerEventListener?.onPlayerStatusChange(id, status)
    }

    private val teamModeChanged = Listener {
        Multiplayer.log("RECEIVED: teamModeChanged -> ${it.contentToString()}")

        val value = it[0] as? String ?: run {
            Multiplayer.log("WARNING: teamModeChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val mode = TeamMode.fromWire(value) ?: run {
            Multiplayer.log("WARNING: teamModeChanged — unknown TeamMode value $value, ignoring event")
            return@Listener
        }
        roomEventListener?.onRoomTeamModeChange(mode)
    }

    private val winConditionChanged = Listener {
        Multiplayer.log("RECEIVED: winConditionChanged -> ${it.contentToString()}")

        val value = it[0] as? String ?: run {
            Multiplayer.log("WARNING: winConditionChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val condition = WinCondition.fromWire(value) ?: run {
            Multiplayer.log("WARNING: winConditionChanged — unknown WinCondition value $value, ignoring event")
            return@Listener
        }
        roomEventListener?.onRoomWinConditionChange(condition)
    }

    private val teamChanged = Listener {
        Multiplayer.log("RECEIVED: teamChanged -> ${it.contentToString()}")

        val id = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: teamChanged — unexpected id type: ${it.contentToString()}")
            return@Listener
        }
        val team = when {
            it[1] == null -> null
            it[1] is String -> {
                val s = it[1] as String
                RoomTeam.fromWire(s) ?: run {
                    Multiplayer.log("WARNING: teamChanged — unknown RoomTeam value $s, ignoring event")
                    return@Listener
                }
            }
            else -> {
                Multiplayer.log("WARNING: teamChanged — unexpected team type: ${it.contentToString()}")
                return@Listener
            }
        }

        playerEventListener?.onPlayerTeamChange(id, team)
    }

    private val roomNameChanged = Listener {

        Multiplayer.log("RECEIVED: roomNameChanged -> ${it.contentToString()}")
        val name = it[0] as? String ?: run {
            Multiplayer.log("WARNING: roomNameChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomNameChange(name)
    }

    private val maxPlayersChanged = Listener {

        Multiplayer.log("RECEIVED: maxPlayersChanged -> ${it.contentToString()}")
        val maxPlayers = (it[0] as? String)?.toIntOrNull() ?: run {
            Multiplayer.log("WARNING: maxPlayersChanged — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomMaxPlayersChange(maxPlayers)
    }

    private val playBeatmap = Listener {

        Multiplayer.log("RECEIVED: playBeatmap -> ${it.contentToString()}")
        roomEventListener?.onRoomMatchPlay()
    }

    private val chatMessage = Listener {

        //Multiplayer.log("RECEIVED: chatMessage -> ${it.contentToString()}")

        val message = it[1] as? String ?: run {
            Multiplayer.log("WARNING: chatMessage — unexpected message type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onRoomChatMessage((it[0] as? String)?.toLongOrNull(), message)
    }

    private val liveScoreData = Listener {

        val json = it[0] as JsonArray

        //Multiplayer.log("RECEIVED: liveScoreData -> ${it.contentToString()}")
        roomEventListener?.onRoomLiveLeaderboard(json)
    }

    // Server-to-client events

    private fun createInitialConnectionListener(expectedSocket: RoomSocket) = Listener {
        if (socket !== expectedSocket) {
            return@Listener
        }

        val json = it[0] as? JsonObject ?: run {
            Multiplayer.log("WARNING: initialConnection — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }

        Multiplayer.log("RECEIVED: initialConnection\n${json}")

        try {
            val players = parsePlayers(json.getJsonArray("players"), json.getInt("maxPlayers"))
            val activePlayers = players.filterNotNull()

            val room = Room(
                id = json.getString("id").toLong(),
                name = json.getString("name"),
                isLocked = json.getBoolean("isLocked"),
                maxPlayers = json.getInt("maxPlayers"),
                mods = RoomMods(json.getJsonArray("mods").toString()),
                gameplaySettings = parseGameplaySettings(json.getJsonObject("gameplaySettings")),
                teamMode = TeamMode.fromWire(json.getString("teamMode")) ?: TeamMode.HeadToHead,
                winCondition = WinCondition.fromWire(json.getString("winCondition")) ?: WinCondition.ScoreV1,
                playerCount = activePlayers.size,
                playerNames = activePlayers.joinToString(separator = ", ") { p -> p.name },
                sessionID = json.getString("sessionId")
            )

            room.players = players
            room.host = json.getJsonObject("host").getString("id").toLong()
            room.beatmap = parseBeatmap(json.optJsonObject("beatmap"))
            room.status = RoomStatus.fromWire(json.getString("status"))

            val localPlayer = room.playersMap[OnlineManager.getInstance().userId]

            if (localPlayer == null) {
                Multiplayer.log("ERROR: initialConnection — local player UID not found in server player list. Disconnecting.")
                roomEventListener?.onRoomConnectFail("Server did not include local player in room state.")

                socket = null

                expectedSocket.off()
                expectedSocket.disconnect()
                return@Listener
            }

            expectedSocket.apply {
                on("beatmapChanged", beatmapChanged)
                on("hostChanged", hostChanged)
                on("playerKicked", playerKicked)
                on("playerModsChanged", playerModsChanged)
                on("roomModsChanged", roomModsChanged)
                on("roomGameplaySettingsChanged", roomGameplaySettingsChanged)
                on("playerStatusChanged", playerStatusChanged)
                on("teamModeChanged", teamModeChanged)
                on("winConditionChanged", winConditionChanged)
                on("teamChanged", teamChanged)
                on("roomNameChanged", roomNameChanged)
                on("maxPlayersChanged", maxPlayersChanged)
                on("playBeatmap", playBeatmap)
                on("chatMessage", chatMessage)
                on("liveScoreData", liveScoreData)
                on("playerJoined", playerJoined)
                on("playerLeft", playerLeft)
                on("allPlayersBeatmapLoadComplete", allPlayersBeatmapLoadComplete)
                on("allPlayersSkipRequested", allPlayersSkipRequested)
                on("allPlayersScoreSubmitted", allPlayersScoreSubmitted)
            }

            // During reconnection, reuse the RoomScene. Creating a new one would:
            // - replace roomEventListener / playerEventListener with a scene that is never displayed (see
            //   onRoomConnect for more details; essentially, its show() method is never called during reconnection), and
            // - leave the on-screen scene as a dead UI shell that can never receive events.
            // Instead, update the existing scene's room reference in-place and re-register it as the active listener,
            // then forward the connect event to it.
            val existingScene = if (Multiplayer.isReconnecting) Multiplayer.roomScene else null

            val scene = if (existingScene != null) {
                existingScene.room = room

                // Re-register the existing scene as the event listener in case something
                // inadvertently replaced it while the reconnection was in flight.
                playerEventListener = existingScene
                roomEventListener = existingScene

                existingScene
            } else RoomScene(room)

            Multiplayer.room = room
            Multiplayer.player = localPlayer
            Multiplayer.roomScene = scene

            // onRoomConnect triggers UI mutations (updateInformation, updatePlayerList, show, etc.), so we move its
            // operations to the update thread.
            updateThread { scene.onRoomConnect(room) }
        } catch (e: Exception) {
            Multiplayer.log("ERROR: initialConnection handler threw an exception: ${e.javaClass.simpleName} — ${e.message}")
            Multiplayer.log(e)

            roomEventListener?.onRoomConnectFail("Unexpected error processing server room state: ${e.message}")

            socket = null

            expectedSocket.off()
            expectedSocket.disconnect()
        }
    }

    private val playerJoined = Listener {
        Multiplayer.log("RECEIVED: playerJoined -> ${it.contentToString()}")

        val json = it[0] as? JsonObject ?: run {
            Multiplayer.log("WARNING: playerJoined — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        val player = parsePlayer(json)

        playerEventListener?.onPlayerJoin(player)
    }

    private val playerLeft = Listener {

        Multiplayer.log("RECEIVED: playerLeft -> ${it.contentToString()}")
        val uid = (it[0] as? String)?.toLongOrNull() ?: run {
            Multiplayer.log("WARNING: playerLeft — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        playerEventListener?.onPlayerLeft(uid)
    }

    private val allPlayersBeatmapLoadComplete = Listener {

        Multiplayer.log("RECEIVED: allPlayersBeatmapLoadComplete")
        roomEventListener?.onRoomMatchStart()
    }

    private val allPlayersSkipRequested = Listener {

        Multiplayer.log("RECEIVED: allPlayersSkipRequested")
        roomEventListener?.onRoomMatchSkip()
    }

    private val allPlayersScoreSubmitted = Listener {

        val array = it[0] as? JsonArray ?: run {
            Multiplayer.log("WARNING: allPlayersScoreSubmitted — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }

        Multiplayer.log("RECEIVED: allPlayersScoreSubmitted\n${array}")
        roomEventListener?.onRoomFinalLeaderboard(array)
    }

    private fun createErrorListener(expectedSocket: RoomSocket) = Listener {
        if (socket !== expectedSocket) return@Listener

        Multiplayer.log("RECEIVED: error -> ${it.contentToString()}")

        val error = it[0] as? String ?: run {
            Multiplayer.log("WARNING: error — unexpected payload type: ${it.contentToString()}")
            return@Listener
        }
        roomEventListener?.onServerError(error)
    }

    // Default listeners

    private fun createConnectErrorListener(expectedSocket: RoomSocket) = Listener {
        if (socket !== expectedSocket) {
            return@Listener
        }

        Multiplayer.log("RECEIVED: connect_error -> ${it.contentToString()}")

        roomEventListener?.onRoomConnectFail(it[0].toString())

        socket = null

        expectedSocket.off()
        expectedSocket.disconnect()
    }

    private fun createDisconnectListener(expectedSocket: RoomSocket) = Listener {
        if (socket !== expectedSocket) return@Listener

        Multiplayer.log("RECEIVED: disconnect -> ${it.contentToString()}")

        val reason = it.getOrNull(0) as? String

        roomEventListener?.onRoomDisconnect(
            reason = reason,
            // Socket was manually disconnected by either server or client.
            byUser = reason == "io server disconnect" || reason == "io client disconnect"
        )

        socket = null

        expectedSocket.off()
        expectedSocket.disconnect()
    }


    // Emitters

    /**
     * Connect to the specified room, if success it'll call [IRoomEventListener.onRoomConnect] if not
     * [IRoomEventListener.onRoomConnectFail]
     */
    fun connectToRoom(roomId: Long, userId: Long, gameSessionId: String, roomPassword: String? = null,
                      multiplayerSessionID: String? = null) {
        lastRoomPassword = roomPassword

        val oldSocket = socket
        socket = null

        oldSocket?.off()
        oldSocket?.disconnect()

        val url = "${LobbyAPI.HOST}/$roomId"
        val auth = mutableMapOf<String, String>()
        val sign = SecurityUtils.signRequest("${userId}_$gameSessionId")

        auth["uid"] = userId.toString()
        auth["gameSessionID"] = gameSessionId
        auth["version"] = API_VERSION.toString()

        if (multiplayerSessionID != null) {
            auth["multiplayerSessionID"] = multiplayerSessionID
        }

        if (sign != null) {
            auth["authSign"] = sign
        }

        if (!roomPassword.isNullOrBlank()) {
            auth["password"] = roomPassword
        }

        Multiplayer.log("Starting connection -> $roomId, $userId")

        val newSocket = if (BuildSettings.MOCK_MULTIPLAYER) MockSocket(userId) else RoomSocket.factory.create(url, auth)

        socket = newSocket

        newSocket.apply {

            on("initialConnection", createInitialConnectionListener(this))
            on("error", createErrorListener(this))

            on(RoomSocket.EVENT_CONNECT_ERROR, createConnectErrorListener(this))
            on(RoomSocket.EVENT_DISCONNECT, createDisconnectListener(this))

        }.connect()
    }

    /**
     * Disconnect from socket.
     */
    fun disconnect() {
        socket?.apply {
            Multiplayer.log("Disconnected from socket.")
            off()
            disconnect()
        }

        socket = null
    }

    // Host only

    /**
     * Change room beatmap.
     */
    @JvmOverloads
    @JvmStatic
    fun changeBeatmap(md5: String? = null, title: String? = null, artist: String? = null, version: String? = null, creator: String? = null) {

        val json = buildJsonObject {

            putIfNotNull("md5", md5)
            putIfNotNull("title", title)
            putIfNotNull("artist", artist)
            putIfNotNull("version", version)
            putIfNotNull("creator", creator)

        }

        socket?.emit("beatmapChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'beatmapChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: beatmapChanged -> $md5, $title, $artist, $version, $creator")
    }

    /**
     * Kick player from room.
     */
    fun kickPlayer(uid: Long) {
        socket?.emit("playerKicked", uid.toString()) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playerKicked' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: playerKicked -> $uid")
    }

    /**
     * Notify all clients to start loading beatmap.
     */
    fun notifyMatchPlay() {
        socket?.emit("playBeatmap") ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playBeatmap' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: playBeatmap")
    }

    /**
     * Change room host.
     */
    fun setRoomHost(uid: Long) {
        socket?.emit("hostChanged", uid.toString()) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'hostChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: hostChanged -> $uid")
    }

    /**
     * Change room mods.
     */
    @JvmStatic
    fun setRoomMods(mods: String) {
        socket?.emit("roomModsChanged", (LenientJson.parseToJsonElement(mods) as JsonArray)) ?: run {
			Multiplayer.log("WARNING: Tried to emit event 'roomModsChanged' while socket is null.")
			return
		}
        Multiplayer.log("EMITTED: roomModsChanged -> $mods")
    }

    /**
     * Change the remove slider lock setting.
     */
    fun setRoomRemoveSliderLock(isEnabled: Boolean) {
        val json = buildJsonObject {
            put("isRemoveSliderLock", isEnabled)
        }

        socket?.emit("roomGameplaySettingsChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomGameplaySettingsChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomGameplaySettingsChanged -> $json")
    }

    /**
     * Change `free mods` condition.
     */
    fun setRoomFreeMods(value: Boolean) {
        val json = buildJsonObject {
            put("isFreeMod", value)
        }

        socket?.emit("roomGameplaySettingsChanged", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomGameplaySettingsChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomGameplaySettingsChanged -> $json")
    }

    /**
     * Change room team mode.
     */
    fun setRoomTeamMode(mode: TeamMode) {
        socket?.emit("teamModeChanged", mode.name) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'teamModeChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: teamModeChanged -> $mode")
    }

    /**
     * Change room win condition.
     */
    fun setRoomWinCondition(condition: WinCondition) {
        socket?.emit("winConditionChanged", condition.name) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'winConditionChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: winConditionChanged -> $condition")
    }

    /**
     * Change room name.
     */
    fun setRoomName(name: String) {
        socket?.emit("roomNameChanged", name) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomNameChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: roomNameChanged -> $name")
    }

    /**
     * Change room max players.
     */
    fun setRoomMaxPlayers(maxPlayers: Int) {
        socket?.emit("maxPlayersChanged", maxPlayers) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'maxPlayersChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: maxPlayersChanged -> $maxPlayers")
    }

    /**
     * Change room password.
     */
    fun setRoomPassword(password: String) {
        socket?.emit("roomPasswordChanged", password) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'roomPasswordChanged' while socket is null.")
            return
        }
        //Multiplayer.log("EMITTED: roomPasswordChanged -> $password")
    }

    // All players

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitFinalScore(json: JsonObject?) {
        socket?.emit("scoreSubmission", json) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'scoreSubmission' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: scoreSubmission\n$json")
    }

    /**
     * Submit the match score at the end of the game.
     */
    @JvmStatic
    fun submitLiveScore(json: JsonObject?) {
        socket?.emit("liveScoreData", json) ?: run {
            //Multiplayer.log("WARNING: Tried to emit event 'liveScoreData' while socket is null.")
            return
        }

        // We don't indent here to avoid spam
        Multiplayer.log("EMITTED: liveScoreData -> $json")
    }

    /**
     * Notify beatmap finish load.
     */
    fun notifyBeatmapLoaded() {
        socket?.emit("beatmapLoadComplete") ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'beatmapLoadComplete' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: beatmapLoadComplete")
    }

    /**
     * Request skip.
     */
    fun requestSkip() {
        socket?.emit("skipRequested") ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'skipRequested' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: skipRequested")
    }

    /**
     * Send chat message.
     */
    fun sendMessage(message: String) {
        socket?.emit("chatMessage", message) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'chatMessage' while socket is null.")
            return
        }
        //Multiplayer.log("EMITTED: chatMessage -> $message")
    }

    /**
     * Change player status.
     */
    @JvmStatic
    fun setPlayerStatus(status: PlayerStatus) {
        socket?.emit("playerStatusChanged", status.name) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'playerStatusChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: playerStatusChanged -> $status")
    }

    /**
     * Change player mods.
     */
    @JvmStatic
    fun setPlayerMods(mods: String) {
        socket?.emit("playerModsChanged", (LenientJson.parseToJsonElement(mods) as JsonArray)) ?: run {
			Multiplayer.log("WARNING: Tried to emit event 'playerModsChanged' while socket is null.")
			return
		}

        Multiplayer.log("EMITTED: playerModsChanged -> $mods")
    }

    /**
     * Change player team.
     */
    fun setPlayerTeam(team: RoomTeam) {
        socket?.emit("teamChanged", team.name) ?: run {
            Multiplayer.log("WARNING: Tried to emit event 'teamChanged' while socket is null.")
            return
        }
        Multiplayer.log("EMITTED: teamChanged -> $team")
    }

}