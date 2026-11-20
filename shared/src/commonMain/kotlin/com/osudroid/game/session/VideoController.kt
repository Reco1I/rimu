package com.osudroid.game.session

import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.data.BeatmapInfo
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.Video
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okio.Path.Companion.toPath

/**
 * The video of a beatmap, shown as the background of its play: loaded when the settings show it, started at its
 * [offset] and following the rate and the pauses of the play.
 *
 * @param onReady Called on the update thread once a video loaded knows its size, to lay it out.
 */
class VideoController(private val onReady: () -> Unit) {

    private var loadingJob: Job? = null

    // Whether the settings show the video.
    private var isEnabled = false

    // Whether the video started playing.
    private var isStarted = false

    /**
     * The video loaded, null before it's loaded or when it isn't shown.
     */
    var video: Video? = null
        private set

    /**
     * The time the video starts at, in seconds of the play.
     */
    var offset = 0f
        private set

    /**
     * The video shown as the background: loaded and enabled.
     */
    val shown: Video?
        get() = if (isEnabled) video else null


    /**
     * Loads the video of [playableBeatmap], or releases it when the settings don't show it.
     */
    fun load(beatmapInfo: BeatmapInfo, playableBeatmap: DroidPlayableBeatmap) {
        // This is used instead of getBackgroundBrightness to directly obtain the
        // updated value from the brightness slider.
        val brightness = OsuSettings.backgroundBrightness
        val videoFilename = playableBeatmap.events.videoFilename
        // Some platforms can't play videos, e.g. Vulkan before Android 10 can't show their frames.
        val videosSupported = Platform.canPlayVideos
        isEnabled = brightness > 0.02f && Settings[OsuSettings.EnableVideo] && videoFilename != null && videosSupported

        if (!isEnabled) {
            cancelLoading()
            release()
            return
        }

        if (video != null) {
            return
        }

        if (loadingJob != null && !loadingJob!!.isCompleted) {
            return
        }

        // Check if video file exists before attempting to load.
        val videoFile = (beatmapInfo.absoluteSetDirectory + "/" + videoFilename).toPath()

        if (!Platform.fileSystem.exists(videoFile)) {
            return
        }

        loadingJob = async {
            try {
                val video = Video(beatmapInfo.absoluteSetDirectory + "/" + videoFilename)
                if (!video.isAvailable) {
                    return@async
                }
                video.alpha = 0f

                ensureActive()

                this@VideoController.video = video

                // The video is laid out from the onReady callback once the decoder reports its dimensions, rather
                // than blocking here. If dimensions are already known (fast local file), the callback fires
                // immediately.
                video.setOnReady { updateThread { onReady() } }
            } catch (e: Exception) {
                video = null
                Logger.e(TAG, "Error while loading video background.", e)
            }

            loadingJob = null
        }
    }

    /**
     * Stops loading the video.
     */
    fun cancelLoading() {
        if (loadingJob != null) {
            loadingJob!!.cancel(CancellationException("Video loading job cancelled"))
            loadingJob = null
        }
    }

    /**
     * Takes the job loading the video, to stop it with the loading of the play.
     */
    fun takeLoadingJob(): Job? {
        val job = loadingJob
        loadingJob = null
        return job
    }

    /**
     * Releases the video loaded.
     */
    fun release() {
        if (video != null) {
            video!!.release()
            video = null
            isStarted = false
        }
    }

    /**
     * Starts again as the attempt starts, from the [offset] of the beatmap, in seconds.
     */
    fun reset(offset: Float) {
        isStarted = false
        this.offset = offset
    }

    /**
     * Starts the video once [time], in seconds, reached its offset, and fades it in.
     *
     * @param speedMultiplier The rate of the play, which the video starts at.
     */
    fun update(time: Float, speedMultiplier: Float) {
        val video = shown

        if (video == null || time < offset) {
            return
        }

        if (!isStarted) {
            video.play()
            video.setPlaybackSpeed(speedMultiplier)
            isStarted = true
        }

        if (video.alpha < 1.0f) {
            video.alpha = min(video.alpha + 0.03f, 1.0f)
        }
    }

    /**
     * Plays the video at the rate of the play.
     */
    fun setPlaybackSpeed(speedMultiplier: Float) {
        shown?.setPlaybackSpeed(speedMultiplier)
    }

    /**
     * Moves the video to [time] of the play, in seconds.
     */
    fun seekTo(time: Float) {
        shown?.seekTo(max(0, ((time - offset) * 1000).toInt()))
    }

    /**
     * Pauses the video, once it started.
     */
    fun pause() {
        if (isStarted) {
            video?.pause()
        }
    }

    /**
     * Plays the video again, once it started.
     */
    fun resume() {
        if (isStarted) {
            video?.play()
        }
    }

    private companion object {
        const val TAG = "VideoController"
    }
}
