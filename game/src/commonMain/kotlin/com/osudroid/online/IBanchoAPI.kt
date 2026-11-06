package com.osudroid.online

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

import com.osudroid.online.model.*
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

}