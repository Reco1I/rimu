package com.osudroid.online.model

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

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

fun BeatmapScore(json: JsonObject): BeatmapScoreModel {
    return BeatmapScoreModel(
        scoreId = json.getLong("Id"),
        rank = -1,
        mark = json.optString("Mark", ""),
        playerName = json.getString("Username"),
        mods = json.getJsonArray("Mods"),
        score = json.getLong("Score"),
        pp = json.getDouble("Pp").toFloat(),
        maxCombo = json.getInt("Combo"),
        accuracy = json.getDouble("Accuracy").toFloat(),
        avatarUrl = json.optString("Avatar")
    )
}

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
