package com.osudroid.online

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.online.model.*
import com.osudroid.scoring.Statistic
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Interface for Bancho API requests.
 *
 * This interface is intended to replace the original iBancho API that is spread
 * accross [OnlineManager] and [OnlineScoring] classes.
 */
object IBanchoAPI {

    private val HOSTNAME = "https://test.osudroid.moe/api"


    suspend fun getBeatmapLeaderboard(hash: String, type: String = "pp"): List<BeatmapScoreModel> {
        val response = httpClient.get("${HOSTNAME}/getrank.php") {
            parameter("hash", hash)
            parameter("uid", OnlineManager.getInstance().userId)
            parameter("type", type)
        }.body<LeaderboardResponse>()

        return response.leaderboard.map(LeaderboardScore::toModel)
    }


    /**
     * The statistics of the score [scoreId] of a leaderboard ranked by [type], and whether it has a replay,
     * or null if the server doesn't answer them.
     *
     * @param difficulty The original difficulty of the score's beatmap, which its mods are migrated from.
     */
    suspend fun getScoreStatistic(scoreId: Long, type: String = "pp", difficulty: BeatmapDifficulty? = null): OnlineScore? {
        val response = OnlineManager.getInstance().post("${HOSTNAME}/gettop.php", listOf(
            "playID" to scoreId.toString(),
            "type" to type,
        ))

        if (response.firstOrNull() != "SUCCESS" || response.size < 2) {
            return null
        }

        val params = response[1].trim().split(Regex("\\s+")).toTypedArray()

        if (params.size < 11) {
            return null
        }

        return OnlineScore(Statistic(params, difficulty), hasReplay = params[10] == "1")
    }

    /**
     * The address of the replay of the score [scoreId] of a leaderboard ranked by [type].
     */
    fun getReplayUrl(scoreId: Long, type: String = "pp"): String {
        return "${HOSTNAME}/" + (if (type == "pp") "bestpp/" else "upload/") + "$scoreId.odr"
    }

}

/**
 * A score of a global leaderboard, with its [stat]istics.
 */
class OnlineScore(val stat: Statistic, val hasReplay: Boolean)
