package com.osudroid.beatmaplisting.mirrors

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.URLBuilder

import com.osudroid.beatmaplisting.BeatmapMirrorDownloadRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorPreviewRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType.*
import com.osudroid.beatmaplisting.BeatmapMirrorSearchResponseModel
import com.osudroid.beatmaplisting.BeatmapSetModel
import ru.nsu.ccfit.zuev.osu.RankedStatus


class OsuDirectSearchRequestModel : BeatmapMirrorSearchRequestModel {

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

        return URLBuilder("https://osu.direct/api/v2/search").apply {
            parameters.append("sort", "${sort.parseToApi()}:${order.parseToApi()}")
            if (status != null) {
                parameters.append("status", status.value.toString())
            }
            parameters.append("mode", "0")
            parameters.append("query", query)
            parameters.append("offset", offset.toString())
            parameters.append("amount", limit.toString())
        }.buildString()
    }
}

class OsuDirectSearchResponseModel : BeatmapMirrorSearchResponseModel {
    override suspend fun invoke(response: HttpResponse): MutableList<BeatmapSetModel> =
        response.body<List<MirrorBeatmapSet>>().mapTo(mutableListOf()) { it.toModel(thumbnail = it.covers?.card) }
}

class OsuDirectDownloadRequestModel : BeatmapMirrorDownloadRequestModel {
    override fun invoke(beatmapSetId: Long, withVideo: Boolean): String {
        val url = "https://osu.direct/api/d/$beatmapSetId"

        return if (!withVideo) URLBuilder(url).apply { parameters.append("noVideo", "1") }.buildString() else url
    }
}

class OsuDirectPreviewRequestModel : BeatmapMirrorPreviewRequestModel {
    override fun invoke(beatmapId: Long): String {
        return "https://osu.direct/api/media/preview/$beatmapId"
    }
}