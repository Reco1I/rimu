package com.osudroid.beatmaplisting

import com.osudroid.resources.Res
import com.reco1l.verktex.localization.LocalizedString
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
    enum class SortType(val description: LocalizedString) {
        Title(Res.string.beatmap_listing_sort_title),
        Artist(Res.string.beatmap_listing_sort_artist),
        BPM(Res.string.beatmap_listing_sort_bpm),
        DifficultyRating(Res.string.beatmap_listing_sort_difficulty_rating),
        HitLength(Res.string.beatmap_listing_sort_hit_length),
        PassCount(Res.string.beatmap_listing_sort_pass_count),
        PlayCount(Res.string.beatmap_listing_sort_play_count),
        TotalLength(Res.string.beatmap_listing_sort_total_length),
        FavouriteCount(Res.string.beatmap_listing_sort_favourite_count),
        LastUpdated(Res.string.beatmap_listing_sort_last_updated),
        RankedDate(Res.string.beatmap_listing_sort_ranked_date),
        SubmittedDate(Res.string.beatmap_listing_sort_submitted_date)
    }

    enum class OrderType(val description: LocalizedString) {
        Ascending(Res.string.beatmap_listing_order_ascending),
        Descending(Res.string.beatmap_listing_order_descending),
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