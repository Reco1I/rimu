package com.osudroid.beatmaplisting

import com.osudroid.beatmaplisting.mirrors.CatboyDownloadRequestModel
import com.osudroid.beatmaplisting.mirrors.CatboyPreviewRequestModel
import com.osudroid.beatmaplisting.mirrors.CatboySearchRequestModel
import com.osudroid.beatmaplisting.mirrors.CatboySearchResponseModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectDownloadRequestModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectPreviewRequestModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectSearchRequestModel
import com.osudroid.beatmaplisting.mirrors.OsuDirectSearchResponseModel
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings

/**
 * Defines a beatmap mirror API and its actions.
 */
enum class BeatmapMirror(

    /**
     * The home URL of the beatmap mirror where the user will be redirected to when
     * clicking on the logo.
     */
    val homeUrl: String,

    /**
     * The description of the beatmap mirror.
     */
    val description: String,

    /**
     * Whether this beatmap mirror supports downloading beatmaps without video. If `false`, the download action will
     * always include the video if available.
     */
    val supportsNoVideoDownloads: Boolean,

    // Actions / Endpoints

    /**
     * The search query action.
     */
    val search: BeatmapMirrorActionWithResponse<BeatmapMirrorSearchRequestModel, BeatmapMirrorSearchResponseModel>,

    /**
     * The download action.
     */
    val download: BeatmapMirrorAction<BeatmapMirrorDownloadRequestModel>,

    /**
     * The music preview action.
     */
    val preview: BeatmapMirrorAction<BeatmapMirrorPreviewRequestModel>,

) {

    /**
     * osu.direct beatmap mirror.
     *
     * [See documentation](https://osu.direct/api/docs)
     */
    OSU_DIRECT(
        homeUrl = "https://osu.direct",
        description = "osu.direct",
        supportsNoVideoDownloads = true,

        search = BeatmapMirrorActionWithResponse(
            request = OsuDirectSearchRequestModel(),
            response = OsuDirectSearchResponseModel(),
        ),
        download = BeatmapMirrorAction(OsuDirectDownloadRequestModel()),
        preview = BeatmapMirrorAction(OsuDirectPreviewRequestModel()),
    ),

    /**
     * Catboy beatmap mirror.
     *
     * [See documentation](https://dev.catboy.best/docs)
     */
    CATBOY(
        homeUrl = "https://catboy.best",
        description = "Mino",
        supportsNoVideoDownloads = false,

        search = BeatmapMirrorActionWithResponse(
            request = CatboySearchRequestModel(),
            response = CatboySearchResponseModel(),
        ),
        download = BeatmapMirrorAction(CatboyDownloadRequestModel()),
        preview = BeatmapMirrorAction(CatboyPreviewRequestModel()),
    );

    companion object {
        /**
         * The selected beatmap mirror.
         */
        var selected
            get() = entries.getOrElse(Settings[OsuSettings.BeatmapMirror]) { entries[0] }
            set(value) {
                Settings[OsuSettings.BeatmapMirror] = value.ordinal
            }
    }
}

