package com.osudroid.beatmaplisting.mirrors

import com.osudroid.beatmaplisting.BeatmapModel
import com.osudroid.beatmaplisting.BeatmapSetModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.nsu.ccfit.zuev.osu.RankedStatus

/**
 * A beatmap set of a mirror's search, in the shape of the osu! API v2 that the mirrors answer in.
 */
@Serializable
internal class MirrorBeatmapSet(
    val id: Long,
    val title: String,
    @SerialName("title_unicode") val titleUnicode: String,
    val artist: String,
    @SerialName("artist_unicode") val artistUnicode: String,
    val ranked: Int,
    val creator: String,
    val video: Boolean,
    val covers: Covers? = null,
    val beatmaps: List<Beatmap>,
) {

    @Serializable
    class Covers(val card: String? = null)

    @Serializable
    class Beatmap(
        val id: Long,
        val version: String,
        @SerialName("difficulty_rating") val difficultyRating: Double,
        val ar: Double,
        val cs: Double,
        val drain: Double,
        val accuracy: Double,
        val bpm: Double,
        @SerialName("hit_length") val hitLength: Long,
        @SerialName("count_circles") val countCircles: Int,
        @SerialName("count_sliders") val countSliders: Int,
        @SerialName("count_spinners") val countSpinners: Int,
    )

    fun toModel(thumbnail: String?) = BeatmapSetModel(
        id = id,
        title = title,
        titleUnicode = titleUnicode,
        artist = artist,
        artistUnicode = artistUnicode,
        status = RankedStatus.valueOf(ranked),
        creator = creator,
        hasVideo = video,
        thumbnail = thumbnail,
        beatmaps = beatmaps.map {
            BeatmapModel(
                id = it.id,
                version = it.version,
                starRating = it.difficultyRating,
                ar = it.ar,
                cs = it.cs,
                hp = it.drain,
                od = it.accuracy,
                bpm = it.bpm,
                lengthSec = it.hitLength,
                circleCount = it.countCircles,
                sliderCount = it.countSliders,
                spinnerCount = it.countSpinners,
            )
        }.sortedBy(BeatmapModel::starRating),
    )
}
