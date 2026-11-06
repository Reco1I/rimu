package com.osudroid.online

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

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

        val json = OsuHttp.getJsonObject("${HOSTNAME}/getrank.php") {
            url {
                parameters.append("hash", hash)
                parameters.append("uid", OnlineManager.getInstance().userId.toString())
                parameters.append("type", type)
            }
        }

        val leaderboard = json.getJsonArray("Leaderboard")

        return List(leaderboard.size) { i -> BeatmapScore(leaderboard.getJsonObject(i)) }
    }

}