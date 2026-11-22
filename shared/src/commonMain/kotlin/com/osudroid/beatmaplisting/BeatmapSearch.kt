package com.osudroid.beatmaplisting

import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.OrderType
import com.osudroid.beatmaplisting.BeatmapMirrorSearchRequestModel.SortType
import com.osudroid.online.httpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import ru.nsu.ccfit.zuev.osu.RankedStatus

/**
 * What the listing searches for.
 *
 * @property status The status of the sets, null for any.
 */
data class BeatmapSearch(
    val query: String = "",
    val sort: SortType = SortType.RankedDate,
    val order: OrderType = OrderType.Descending,
    val status: RankedStatus? = null
)

/**
 * Where the listing finds beatmap sets, a page at a time.
 */
fun interface BeatmapSearchSource {

    /**
     * Returns [limit] sets at most matching [search], skipping the first [offset] ones. It throws when
     * the search fails.
     */
    suspend fun search(search: BeatmapSearch, offset: Int, limit: Int): List<BeatmapSetModel>
}

/**
 * Searches the selected [BeatmapMirror].
 */
object MirrorSearchSource : BeatmapSearchSource {

    override suspend fun search(search: BeatmapSearch, offset: Int, limit: Int): List<BeatmapSetModel> {
        val mirror = BeatmapMirror.selected
        val url = mirror.search.request(search.query, offset, limit, search.sort, search.order, search.status)

        val response = httpClient.get(url) {
            header(HttpHeaders.UserAgent, MIRROR_USER_AGENT)
        }

        return mirror.search.response(response)
    }
}
