package com.reco1l.verktex.android.video

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.ExperimentalFfmpegVideoRenderer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.android.AndroidExternalTexture
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.video.VideoDecoder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A [VideoDecoder] over ExoPlayer, which decodes in hardware straight into an external texture, with
 * Media3's FFmpeg extension for the codecs the device lacks.
 */
@OptIn(UnstableApi::class)
class ExoPlayerVideoDecoder(context: Context, path: String) : VideoDecoder {

    @Volatile
    override var width = 0
        private set

    @Volatile
    override var height = 0
        private set

    // Its region always covers it whole and the decoder sizes the buffers, so its own size is only nominal.
    private val texture = Verktex.graphics.createExternalTexture(NOMINAL_SIZE, NOMINAL_SIZE)
    private val externalTexture = texture as AndroidExternalTexture

    override val region: GraphicsTexture.Region
        get() = externalTexture.region

    private val player: ExoPlayer
    private val onSizeKnown = AtomicReference<(() -> Unit)?>()
    private val isReleased = AtomicBoolean(false)


    init {
        // ExoPlayer enforces that its calls run on its looper, the main thread here.
        player = createPlayer(context.applicationContext)

        mainHandler.post {
            player.volume = 0f
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width == 0 || videoSize.height == 0) {
                        return
                    }

                    width = videoSize.width
                    height = videoSize.height
                    onSizeKnown.getAndSet(null)?.invoke()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Logger.e("ExoPlayerVideoDecoder", "Failed to play the video", error)
                }
            })
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            player.prepare()
        }

        // The surface is (re)created on the draw thread, on the first load and after a context loss.
        externalTexture.onSurfaceRecreated = {
            val surface = externalTexture.surface

            mainHandler.post {
                if (!isReleased.get()) {
                    player.setVideoSurface(surface)
                }
            }
        }

        // Loaded right away: a decoder may report no size until it has a surface to decode into.
        Verktex.scheduleOnDraw { texture.load() }
    }


    override fun setOnSizeKnown(callback: () -> Unit) {
        onSizeKnown.set(callback)

        // The size may have arrived before the callback was set.
        if (width > 0) {
            onSizeKnown.getAndSet(null)?.invoke()
        }
    }

    override fun play() {
        mainHandler.post { player.play() }
    }

    override fun pause() {
        mainHandler.post { player.pause() }
    }

    override fun seekTo(milliseconds: Long) {
        mainHandler.post { player.seekTo(milliseconds) }
    }

    override fun setSpeed(speed: Float) {
        mainHandler.post { player.playbackParameters = PlaybackParameters(speed) }
    }

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }

        externalTexture.onSurfaceRecreated = null
        onSizeKnown.set(null)

        mainHandler.post {
            // In this order: the codec thread must stop writing to the surface before the player is
            // released, and the surface must outlive the player.
            player.setVideoSurface(null)
            player.release()

            Verktex.scheduleOnDraw {
                texture.dispose()
                Verktex.graphics.unregisterResource(texture)
            }
        }
    }


    private companion object {

        const val NOMINAL_SIZE = 16f

        val mainHandler = Handler(Looper.getMainLooper())

        fun createPlayer(context: Context): ExoPlayer {
            // The extension mode finds the FFmpeg audio renderer by itself, but the video one has to be added
            // by hand as the default factory looks for it under its former name.
            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildVideoRenderers(
                    context: Context,
                    extensionRendererMode: Int,
                    mediaCodecSelector: MediaCodecSelector,
                    enableDecoderFallback: Boolean,
                    eventHandler: Handler,
                    eventListener: VideoRendererEventListener,
                    allowedVideoJoiningTimeMs: Long,
                    out: ArrayList<Renderer>
                ) {
                    super.buildVideoRenderers(
                        context,
                        extensionRendererMode,
                        mediaCodecSelector,
                        enableDecoderFallback,
                        eventHandler,
                        eventListener,
                        allowedVideoJoiningTimeMs,
                        out
                    )

                    out.add(ExperimentalFfmpegVideoRenderer(allowedVideoJoiningTimeMs, eventHandler, eventListener, 50))
                }
            }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

            return ExoPlayer.Builder(context, renderersFactory)
                .setLooper(Looper.getMainLooper())
                .build()
        }
    }
}
