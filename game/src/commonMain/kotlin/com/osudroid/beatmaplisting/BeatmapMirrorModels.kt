package com.osudroid.beatmaplisting

import io.ktor.client.statement.HttpResponse

import ru.nsu.ccfit.zuev.osu.RankedStatus


// Request

fun interface BeatmapMirrorSearchRequestModel {
    /**
     * @param query The search query.
     * @param offset The search result offset.
     */
    operator fun invoke(query: String, offset: Int, limit: Int, sort: SortType, order: OrderType, status: RankedStatus?): String

    /**
     * Sorting types availables by the osu!api.
     */
    enum class SortType(val description: String) {
        Title("Title"),
        Artist("Artist"),
        BPM("BPM"),
        DifficultyRating("Difficulty rating"),
        HitLength("Hit length"),
        PassCount("Pass count"),
        PlayCount("Play count"),
        TotalLength("Total length"),
        FavouriteCount("Favourite count"),
        LastUpdated("Last updated"),
        RankedDate("Ranked date"),
        SubmittedDate("Submitted date")
    }

    enum class OrderType(val description: String) {
        Ascending("Ascending"),
        Descending("Descending"),
    }

}

fun interface BeatmapMirrorDownloadRequestModel {
    /**
     * @param beatmapSetId The beatmap set ID.
     * @param withVideo Whether to include the video in the download if available.
     */
    operator fun invoke(beatmapSetId: Long, withVideo: Boolean): String
}

fun interface BeatmapMirrorPreviewRequestModel {
    /**
     * @param beatmapId The beatmap ID.
     */
    operator fun invoke(beatmapId: Long): String
}


// Response

interface BeatmapMirrorSearchResponseModel {
    /**
     * Reads the beatmap sets of the response to a search.
     */
    suspend operator fun invoke(response: HttpResponse): MutableList<BeatmapSetModel>
}

/**
 * The user agent the mirrors are asked with.
 */
const val MIRROR_USER_AGENT = "Chrome/Android"