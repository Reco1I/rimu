package com.osudroid.beatmaplisting

import com.reco1l.verktex.ui.toast.Toasts

/**
 * The listing to search beatmap sets on the selected mirror ([BeatmapMirror]) and download them
 * ([BeatmapDownloader]). It was an Android view and isn't written on Verktex yet.
 */
object BeatmapListing {

    /**
     * Shows the listing. Until it's written, it only tells that it's a work in progress.
     */
    fun show() {
        Toasts.show("The beatmap listing is a work in progress.")
    }
}
