@file:JvmName("ScoreJson")

package com.osudroid.data

import org.apache.commons.io.FilenameUtils
import org.json.JSONObject

/**
 * Reads a [ScoreInfo] from the JSON of an exported replay.
 */
fun ScoreInfo(json: JSONObject) =
    ScoreInfo(

        beatmapMD5 = json.getString("beatmapMD5"),
        replayFilename = FilenameUtils.getName(json.getString("replayfile")),

        // The keys don't correspond to the table columns in order to keep compatibility with the old replays.
        id = json.optLong("id", 0),
        playerName = json.getString("playername"),
        mods = json.getString("mods"),
        score = json.getLong("score"),
        maxCombo = json.getInt("combo"),
        mark = json.getString("mark"),
        hit300k = json.getInt("h300k"),
        hit300 = json.getInt("h300"),
        hit100k = json.getInt("h100k"),
        hit100 = json.getInt("h100"),
        hit50 = json.getInt("h50"),
        misses = json.getInt("misses"),
        time = json.getLong("time"),
        sliderHeadHits = json.optInt("sliderHeadHits", -1).takeIf { it >= 0 },
        sliderTickHits = json.optInt("sliderTickHits", -1).takeIf { it >= 0 },
        sliderRepeatHits = json.optInt("sliderRepeatHits", -1).takeIf { it >= 0 },
        sliderEndHits = json.optInt("sliderEndHits", -1).takeIf { it >= 0 }
    )
