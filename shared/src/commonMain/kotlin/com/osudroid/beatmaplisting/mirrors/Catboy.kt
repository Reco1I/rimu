package com.osudroid.beatmaplisting.mirrors

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder

import com.osudroid.beatmaplisting.BeatmapMirrorDownloadRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorPreviewRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.Ascending
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.Descending
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.Artist
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.BPM
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.DifficultyRating
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.FavouriteCount
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.HitLength
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.LastUpdated
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.PassCount
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.PlayCount
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.RankedDate
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.SubmittedDate
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.Title
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.TotalLength
import com.osudroid.beatmaplisting.BeatmapMirrorSearchResponseModel
import com.osudroid.beatmaplisting.BeatmapSetModel
import ru.nsu.ccfit.zuev.osu.RankedStatus

// API reference: https://dev.catboy.best/docs


class CatboySearchRequestModel : BeatmapMirrorSearchRequestModel {

    private fun SortType.parseToApi() = when (this) {
        Title -> "title"
        Artist -> "artist"
        BPM -> "beatmaps.bpm"
        DifficultyRating -> "beatmaps.difficulty_rating"
        HitLength -> "beatmaps.hit_length"
        PassCount -> "beatmaps.passcount"
        PlayCount -> "beatmaps.playcount"
        TotalLength -> "beatmaps.total_length"
        FavouriteCount -> "favourite_count"
        LastUpdated -> "last_updated"
        RankedDate -> "ranked_date"
        SubmittedDate -> "submitted_date"
    }

    private fun OrderType.parseToApi() = when (this) {
        Ascending -> "asc"
        Descending -> "desc"
    }

    override fun invoke(query: String, offset: Int, limit: Int, sort: SortType, order: OrderType, status: RankedStatus?): String {

        return URLBuilder("https://catboy.best/api/v2/search").apply {
            parameters.append("sort", "${sort.parseToApi()}:${order.parseToApi()}")
            if (status != null) {
                parameters.append("status", status.value.toString())
            }
            parameters.append("mode", "0")
            parameters.append("query", query)
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }.buildString()
    }
}


class CatboySearchResponseModel : BeatmapMirrorSearchResponseModel {
    override suspend fun invoke(response: HttpResponse): MutableList<BeatmapSetModel> =
        response.body<List<MirrorBeatmapSet>>().mapTo(mutableListOf()) { it.toModel(thumbnail = "https://assets.ppy.sh/beatmaps/${it.id}/covers/card.jpg") }
}


class CatboyDownloadRequestModel : BeatmapMirrorDownloadRequestModel {
    override fun invoke(beatmapSetId: Long, withVideo: Boolean): String {
        return "https://catboy.best/d/$beatmapSetId"
    }
}


class CatboyPreviewRequestModel : BeatmapMirrorPreviewRequestModel {
    override fun invoke(beatmapId: Long): String {
        return "https://catboy.best/preview/audio/$beatmapId"
    }
}
