package ru.nsu.ccfit.zuev.osu.scoring

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

// What a replay file holds, as JSON. Replays were written with Java's object serialization until
// version 9; those files aren't read anymore.

/**
 * The format replays are written in, encoded compactly and read ignoring what it doesn't know.
 */
internal val ReplayJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * A replay file.
 */
@Serializable
internal class ReplayFile(
    /**
     * The version of the replay, see [Replay.ReplayFormatVersion].
     */
    val version: Int,
    val beatmapsetName: String,
    val beatmapName: String,
    val md5: String,
    /**
     * The result of the play, or `null` if the replay doesn't have it.
     */
    val stat: ReplayStatistics? = null,
    /**
     * The movements of each cursor, in the order the game tracks them.
     */
    val cursors: List<ReplayCursor> = emptyList(),
    /**
     * The result of each hit object of the beatmap, in its order.
     */
    val objects: List<ReplayObject> = emptyList(),
)

/**
 * The result of the play a replay recorded.
 */
@Serializable
internal class ReplayStatistics(
    /**
     * When the play ended, in milliseconds since the Unix epoch.
     */
    val time: Long,
    val hit300k: Int,
    val hit300: Int,
    val hit100k: Int,
    val hit100: Int,
    val hit50: Int,
    val misses: Int,
    val totalScore: Long,
    val maxCombo: Int,
    val playerName: String,
    /**
     * The mods of the play, as [com.osudroid.utils.ModHashMap.serializeMods] writes them.
     */
    val mods: JsonElement,
)

/**
 * The movements of a cursor, one per index of each array: its time, in milliseconds, its position,
 * in the playfield's coordinates as they were recorded, and its [TouchType.id]. Positions of
 * [TouchType.UP] movements are 0.
 */
@Serializable
internal class ReplayCursor(
    val time: IntArray,
    val x: FloatArray,
    val y: FloatArray,
    val type: ByteArray,
)

/**
 * The result of a hit object.
 */
@Serializable
internal class ReplayObject(
    /**
     * The offset of the hit, in milliseconds.
     */
    val accuracy: Short = 0,
    /**
     * The indices of the slider's nested objects that were hit, if any.
     */
    val ticks: List<Int>? = null,
    /**
     * The [ResultType.id] of the object.
     */
    val result: Byte = 0,
)
