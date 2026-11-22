package com.osudroid.beatmaplisting

import com.osudroid.ui.v2.beatmaplisting.BeatmapListingScene
import com.reco1l.verktex.Verktex

/**
 * The listing to search beatmap sets on the selected mirror ([BeatmapMirror]) and download them
 * ([BeatmapDownloader]), see [BeatmapListingScene].
 */
object BeatmapListing {

    /**
     * Shows the listing above the current scene, unless it's the current one already.
     */
    fun show() {
        Verktex.withTreeLock {
            if (Verktex.scene !is BeatmapListingScene) {
                Verktex.pushScene(BeatmapListingScene())
            }
        }
    }
}
