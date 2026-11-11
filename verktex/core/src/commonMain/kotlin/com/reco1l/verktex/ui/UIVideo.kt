package com.reco1l.verktex.ui

import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.px
import com.reco1l.verktex.video.VideoDecoder
import kotlin.concurrent.Volatile

/**
 * Plays the video file at [path] without its sound, with the platform's [VideoDecoder]. It has the
 * size of the video once it's known, see [setOnReady]. Where videos can't be played it shows
 * nothing, see [isAvailable].
 */
class UIVideo(path: String) : UISprite() {

    private val decoder: VideoDecoder? = Platform.createVideoDecoder(path)

    @Volatile
    private var onReady: (() -> Unit)? = null

    @Volatile
    private var isReleased = false


    /**
     * Whether the platform could create a decoder for the video.
     */
    val isAvailable: Boolean
        get() = decoder != null

    /**
     * The width of the video once it's known, 0 until then.
     */
    val videoWidth: Int
        get() = decoder?.width ?: 0

    /**
     * The height of the video once it's known, 0 until then.
     */
    val videoHeight: Int
        get() = decoder?.height ?: 0


    init {
        scaleType = ScaleType.Stretch
        scaleOrigin = Anchor.Center

        decoder?.setOnSizeKnown {
            Verktex.scheduleOnUpdate {
                if (isReleased) {
                    return@scheduleOnUpdate
                }

                textureRegion = decoder.region
                width = decoder.width.toFloat().px
                height = decoder.height.toFloat().px

                onReady?.invoke()
                onReady = null
            }
        }
    }


    /**
     * Calls [callback] once on the update thread when the size of the video is known, after this
     * component took it.
     */
    fun setOnReady(callback: () -> Unit) {
        if (textureRegion != null && videoWidth > 0) {
            Verktex.scheduleOnUpdate(callback)
        } else {
            onReady = callback
        }
    }

    fun play() {
        decoder?.play()
    }

    fun pause() {
        decoder?.pause()
    }

    fun seekTo(milliseconds: Int) {
        decoder?.seekTo(milliseconds.toLong())
    }

    fun setPlaybackSpeed(speed: Float) {
        decoder?.setSpeed(speed)
    }

    /**
     * Stops the video, frees the decoder and removes this component. Only the first call has an
     * effect.
     */
    fun release() {
        if (isReleased) {
            return
        }
        isReleased = true
        onReady = null

        Verktex.scheduleOnUpdate {
            removeSelf()
            textureRegion = null
            decoder?.release()
        }
    }
}
