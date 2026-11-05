@file:JvmName("MultiplayerConverter")

package com.osudroid.multiplayer

import com.osudroid.multiplayer.api.data.RoomTeam
import com.osudroid.multiplayer.api.data.WinCondition
import com.osudroid.utils.deserializeMods
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


// Statistics

/**
 * Specifically made to handle `liveScoreData` event.
 */
fun jsonToScoreboardItem(json: JSONObject) = ScoreBoardItem().apply {

    userName = json.getString("username")
    playScore = json.getLong("score")
    maxCombo = json.getInt("combo")
    accuracy = json.getDouble("accuracy").toFloat()
    isAlive = json.getBoolean("isAlive")
}

/**
 * Specifically made to handle `scoreSubmission` event.
 */
fun jsonToStatistic(json: JSONObject) = StatisticV2().apply {

    uid = json.optLong("uid", -1L)
    playerName = json.getString("username")
    setForcedScore(json.getLong("score"))
    time = System.currentTimeMillis()
    mod = deserializeMods(json.optJSONArray("mods")?.toString() ?: "")
    scoreMaxCombo = json.optInt("maxCombo")
    hit300k = json.optInt("geki")
    hit300 = json.optInt("perfect")
    hit100k = json.optInt("katu")
    hit100 = json.optInt("good")
    hit50 = json.optInt("bad")
    misses = json.optInt("miss")
    isAlive = json.getBoolean("isAlive")
}

/**
 * Converts the statistic to a [ScoreBoardItem], used specifically for Multiplayer.
 */
fun StatisticV2.toBoardItem(): ScoreBoardItem {
    //noinspection DataFlowIssue
    val combo = if (!Multiplayer.isConnected || Multiplayer.room!!.winCondition != WinCondition.MaxCombo) combo else lastScoreMaxCombo
    return ScoreBoardItem(playerName, totalScoreWithMultiplier, combo, accuracy, isAlive)
}

/**
 * Whether the statistic corresponds to a team.
 */
fun StatisticV2.isTeamStatistic() =
    Multiplayer.isConnected && (playerName == RoomTeam.Red.toString() || playerName == RoomTeam.Blue.toString())
