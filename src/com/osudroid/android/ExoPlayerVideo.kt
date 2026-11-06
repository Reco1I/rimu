package com.osudroid.android

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.reco1l.verktex.Logger
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
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.android.AndroidExternalTexture
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.px
import com.osudroid.ui.v2.UIVideo
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * A [UIVideo] played with ExoPlayer. It lives in the app rather than in the engine because it depends on
 * the FFmpeg decoder, which is a local AAR that a library module can't include.
 */
@OptIn(UnstableApi::class)
class ExoPlayerVideo(source: String) : UIVideo() {

    /**
     * The width of the video once the player reported it, 0 until then.
     */
    @Volatile
    override var videoWidth = 0
        private set

    /**
     * The height of the video once the player reported it, 0 until then.
     */
    @Volatile
    override var videoHeight = 0
        private set


    private val player: ExoPlayer

    // Its region always covers it whole and the decoder sizes the buffers, so its own size is only nominal.
    private val texture = Verktex.graphics.createExternalTexture(NOMINAL_SIZE, NOMINAL_SIZE)
    private val externalTexture = texture as AndroidExternalTexture

    private val onReady = AtomicReference<(() -> Unit)?>()
    private val isReleased = AtomicBoolean(false)


    init {
        textureRegion = externalTexture.region
        scaleType = ScaleType.Stretch
        scaleOrigin = Anchor.Center

        // ExoPlayer enforces that its calls run on its looper, the main thread here.
        player = createPlayer()

        mainHandler.post {
            player.volume = 0f
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width == 0 || videoSize.height == 0) {
                        return
                    }

                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                    notifyReady()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Logger.e("UIVideo", "Failed to play the video", error)
                }
            })
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(source))))
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

        // Loaded right away: a decoder may report no size until it has a surface to decode into, and this
        // component only has a size to be drawn with once that is known.
        Verktex.scheduleOnDraw { texture.load() }
    }


    override fun setOnReady(callback: () -> Unit) {
        onReady.set(callback)

        // The size may have arrived before the callback was set.
        if (videoWidth > 0) {
            notifyReady()
        }
    }

    private fun notifyReady() {
        Verktex.scheduleOnUpdate {
            width = videoWidth.toFloat().px
            height = videoHeight.toFloat().px

            onReady.getAndSet(null)?.invoke()
        }
    }

    override fun play() {
        mainHandler.post { player.play() }
    }

    override fun pause() {
        mainHandler.post { player.pause() }
    }

    override fun seekTo(ms: Int) {
        mainHandler.post { player.seekTo(ms.toLong()) }
    }

    override fun setPlaybackSpeed(speed: Float) {
        mainHandler.post { player.playbackParameters = PlaybackParameters(speed) }
    }

    override fun release() {
        if (!isReleased.compareAndSet(false, true)) {
            return
        }

        externalTexture.onSurfaceRecreated = null

        Verktex.scheduleOnUpdate {
            removeSelf()

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
    }


    private companion object {

        const val NOMINAL_SIZE = 16f

        val mainHandler = Handler(Looper.getMainLooper())

        fun createPlayer(): ExoPlayer {
            // The game's activity, or the context of a process without it (e.g. the UI tests).
            val context = AndroidGame.context.applicationContext

            // The extension mode finds the FFmpeg audio renderer by itself, but the video one has to be added
            // by hand as the default factory looks for it under its former name.
            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildVideoRenderers(
                    context: android.content.Context,
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
