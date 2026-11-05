package com.osudroid.multiplayer.api.data

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

import com.reco1l.verktex.Logger


/**
 * Parse a [JsonObject] of player to [RoomPlayer].
 */
fun parsePlayer(o: JsonObject) = RoomPlayer(
    id = o.getString("id").toLong(),
    name = o.getString("username"),
    rank = o.getInt("rank"),
    status = PlayerStatus.fromWire(o.getString("status")) ?: PlayerStatus.NotReady,
    team = if (o.isNull("team")) null else RoomTeam.fromWire(o.getString("team")),
    mods = RoomMods(o.getJsonArray("mods").toString())
)

/**
 * Parse a [JsonArray] of players to an array of [RoomPlayer]s of exactly [size] elements.
 *
 * `null` elements represent empty room slots.
 */
fun parsePlayers(array: JsonArray?, size: Int): Array<RoomPlayer?> {
    if (array != null) {
        val actual = array.size

        when {
            actual > size -> Logger.w(
                "Parsing",
                "parsePlayers: array has $actual entries but maxPlayers=$size; " +
                "${actual - size} excess player(s) beyond slot boundary will be ignored (EH-4)"
            )
            actual < size -> Logger.i(
                "Parsing",
                "parsePlayers: array has $actual entries for a $size-slot room; " +
                "${size - actual} trailing slot(s) treated as empty (EH-4)"
            )
        }
    }

    return Array(size) { i ->
        parsePlayer(array?.optJsonObject(i) ?: return@Array null)
    }
}

/**
 * Parse a [JsonObject] of `beatmap` to [RoomBeatmap]
 */
fun parseBeatmap(o: JsonObject?): RoomBeatmap? {

    if (o == null || !o.containsKey("md5")) {
        return null
    }

    val beatmap = RoomBeatmap(
        md5 = o.getString("md5"),
        title = o.optString("title"),
        artist = o.optString("artist"),
        creator = o.optString("creator"),
        version = o.optString("version")
    )

    if (!o.isNull("beatmapSetId")) {
        beatmap.parentSetID = o.getString("beatmapSetId").toLong()
    }

    return beatmap
}

/**
 * Parse a [JsonObject] of gameplay settings to [RoomGameplaySettings].
 */
fun parseGameplaySettings(o: JsonObject) = RoomGameplaySettings(
    isRemoveSliderLock = o.getBoolean("isRemoveSliderLock"),
    isFreeMod = o.getBoolean("isFreeMod")
)