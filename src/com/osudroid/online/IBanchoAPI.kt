package com.osudroid.online

import com.osudroid.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

import com.osudroid.online.model.*
import com.reco1l.framework.net.*
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

/**
 * Interface for Bancho API requests.
 *
 * This interface is intended to replace the original iBancho API that is spread
 * accross [OnlineManager] and [OnlineScoring] classes.
 */
object IBanchoAPI {

    private val HOSTNAME = "https://test.osudroid.moe/api"


    fun getBeatmapLeaderboard(hash: String, type: String = "pp"): List<BeatmapScoreModel> {

        JsonObjectRequest("${HOSTNAME}/getrank.php").use { request ->
            request.buildUrl {
                addQueryParameter("hash", hash)
                addQueryParameter("uid", OnlineManager.getInstance().userId.toString())
                addQueryParameter("type", type)
            }

            val list = mutableListOf<BeatmapScoreModel>()

            val leaderboard = request.execute().json.getJsonArray("Leaderboard")

            for (i in 0 until leaderboard.size) {
                list.add(BeatmapScore(leaderboard.getJsonObject(i)))
            }

            return list
        }
    }

}