package com.osudroid.game.replay

import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.Card

/**
 * Controls playback of gameplay when replaying.
 */
class ReplayPlaybackControl : Card() {
    /**
     * Controls for playback rate.
     */
    val rateControl = ReplayPlaybackRate()

    /**
     * Controls for seeking.
     */
    val seekControl = ReplayPlaybackSeek()

    init {
        width = Dimension.FillAvailable
        title = "Playback"

        content {
            +seekControl
            +rateControl
        }
    }
}
