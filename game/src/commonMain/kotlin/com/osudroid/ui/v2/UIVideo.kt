package com.osudroid.ui.v2

import com.osudroid.OsuGameHost
import com.reco1l.verktex.ui.UISprite

/**
 * Plays a video file without sound. Each platform implements it with its own decoder, and creates it
 * with `OsuGameHost.current.createVideo`.
 */
abstract class UIVideo : UISprite() {

    /**
     * The width of the video once the decoder reported it, 0 until then.
     */
    abstract val videoWidth: Int

    /**
     * The height of the video once the decoder reported it, 0 until then.
     */
    abstract val videoHeight: Int

    /**
     * Sets the callback called once on the update thread when the size of the video is known.
     */
    abstract fun setOnReady(callback: () -> Unit)

    abstract fun play()

    abstract fun pause()

    abstract fun seekTo(ms: Int)

    abstract fun setPlaybackSpeed(speed: Float)

    /**
     * Stops the video and frees its resources. Only the first call has an effect.
     */
    abstract fun release()
}
