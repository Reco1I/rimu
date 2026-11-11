package com.osudroid.online.model

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import com.osudroid.data.*

data class BeatmapScoreModel(
    val scoreId: Long,
    val rank: Int,
    val mark: String = "",
    val playerName: String,
    val mods: JsonArray,
    val score: Long,
    val pp: Float,
    val maxCombo: Int,
    val accuracy: Float,
    val avatarUrl: String? = null,
)

/**
 * A score of the global leaderboard of a beatmap, as the server sends it.
 */
@Serializable
class LeaderboardScore(
    @SerialName("Id") val id: Long,
    @SerialName("Mark") val mark: String = "",
    @SerialName("Username") val username: String,
    @SerialName("Mods") val mods: JsonArray,
    @SerialName("Score") val score: Long,
    @SerialName("Pp") val pp: Double,
    @SerialName("Combo") val combo: Int,
    @SerialName("Accuracy") val accuracy: Double,
    @SerialName("Avatar") val avatar: String? = null,
) {
    fun toModel() = BeatmapScoreModel(
        scoreId = id,
        rank = -1,
        mark = mark,
        playerName = username,
        mods = mods,
        score = score,
        pp = pp.toFloat(),
        maxCombo = combo,
        accuracy = accuracy.toFloat(),
        avatarUrl = avatar,
    )
}

/**
 * The global leaderboard of a beatmap, as the server sends it.
 */
@Serializable
class LeaderboardResponse(@SerialName("Leaderboard") val leaderboard: List<LeaderboardScore>)

fun BeatmapScore(info: ScoreInfo): BeatmapScoreModel {
    return BeatmapScoreModel(
        scoreId = info.id,
        rank = 0,
        mark = info.mark,
        playerName = info.playerName,
        mods = LenientJson.parseToJsonElement(info.mods) as JsonArray,
        score = info.score,
        pp = 0f,
        maxCombo = info.maxCombo,
        accuracy = info.accuracy
    )
}
