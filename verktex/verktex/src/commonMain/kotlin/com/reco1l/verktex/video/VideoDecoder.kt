package com.reco1l.verktex.video

import com.reco1l.verktex.graphics.GraphicsTexture

/**
 * Decodes a video file, without its sound, into a texture. Each platform has its own, created with
 * [com.reco1l.verktex.Platform.createVideoDecoder], and [com.reco1l.verktex.ui.UIVideo] shows it.
 *
 * Its methods may be called from any thread.
 */
interface VideoDecoder {

    /**
     * The region showing the current frame, null until the size of the video is known.
     */
    val region: GraphicsTexture.Region?

    /**
     * The width of the video in pixels, 0 until it's known.
     */
    val width: Int

    /**
     * The height of the video in pixels, 0 until it's known.
     */
    val height: Int

    /**
     * Calls [callback] once, on any thread, when the size of the video is known: right away if it
     * already is.
     */
    fun setOnSizeKnown(callback: () -> Unit)

    fun play()

    fun pause()

    fun seekTo(milliseconds: Long)

    /**
     * Plays the video at [speed] times its normal speed.
     */
    fun setSpeed(speed: Float)

    /**
     * Stops decoding and frees the decoder's resources. Only the first call has an effect.
     */
    fun release()
}
