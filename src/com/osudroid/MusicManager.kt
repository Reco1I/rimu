package com.osudroid

import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.beatmaps.timings.EffectControlPoint
import com.osudroid.beatmaps.timings.EffectControlPointManager
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.beatmaps.timings.TimingControlPointManager
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.GlobalManager

object MusicManager {

    /**
     * The currently loaded beatmap.
     */
    var currentBeatmap: BeatmapInfo? = null
        set(value) {
            if (field != value) {
                field = value
                onBeatmapChangeListeners.values.forEach { it(value) }
            }
        }

    /**
     * Whether the music manager is currently loading a beatmap or not.
     */
    var isLoading = false
        private set

    /**
     * The current position of the song in milliseconds.
     */
    var position
        get() = songService.position
        set(value) = songService.seekTo(value)


    /**
     * Whether a song is currently playing or not.
     */
    val isPlaying
        get() = songService.status == Status.PLAYING

    /**
     * The length of the current song in milliseconds.
     */
    val length
        get() = songService.length


    /**
     * The current timing control point.
     */
    var currentTimingControlPoint = TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT

    /**
     * The current effect control point.
     */
    var currentEffectControlPoint = EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT


    private var onBeatmapChangeListeners = mapOf<Any, (BeatmapInfo?) -> Unit>()

    // Whether the song is meant to be playing, set by play() and cleared by pause() and stop().
    private var isPlaybackWanted = false

    private var timingControlPointManager: TimingControlPointManager? = null
    private var effectControlPointManager: EffectControlPointManager? = null


    private val songService = GlobalManager.getInstance().songService


    private fun clear() {
        timingControlPointManager = null
        effectControlPointManager = null
        currentTimingControlPoint = TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
        currentEffectControlPoint = EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
    }


    /**
     * Loads the control points of the current beatmap and, if [reloadAudio], its song, which is
     * otherwise expected to be already playing (e.g. started by song select).
     */
    fun load(reloadAudio: Boolean = true) {
        if (isLoading) {
            return
        }
        isLoading = true

        val beatmap = currentBeatmap

        if (beatmap == null) {
            clear()
            isLoading = false
            return
        }

        try {
            val parser = BeatmapParser(beatmap.path)
            val data = parser.parse(false)

            timingControlPointManager = data.controlPoints.timing
            effectControlPointManager = data.controlPoints.effect

            currentTimingControlPoint = timingControlPointManager?.controlPointAt(0.0) ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.controlPointAt(0.0) ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT

            if (reloadAudio) {
                songService.preLoad(beatmap.audioPath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Otherwise a failed load would prevent loading any other beatmap.
            isLoading = false
        }
    }

    fun play() {
        if (songService.status != Status.PLAYING) {
            songService.play()
        }
        // A song that can't be played is not waited for, see onUpdate.
        isPlaybackWanted = songService.status == Status.PLAYING
    }

    fun stop() {
        isPlaybackWanted = false
        if (songService.status == Status.PLAYING) {
            songService.stop()
        }
    }

    fun pause() {
        isPlaybackWanted = false
        if (songService.status == Status.PLAYING) {
            songService.pause()
        }
    }

    /**
     * Plays a random beatmap of the next beatmap set of the library.
     */
    fun next() = playBeatmapSet(LibraryManager.selectNextBeatmapSet())

    /**
     * Plays a random beatmap of the previous beatmap set of the library.
     */
    fun previous() = playBeatmapSet(LibraryManager.selectPreviousBeatmapSet())

    private fun playBeatmapSet(beatmapSet: BeatmapSetInfo?) {
        // Null when the library is empty.
        val beatmap = beatmapSet?.beatmaps?.randomOrNull() ?: return

        currentBeatmap = beatmap
        load()
        play()
    }

    fun addOnBeatmapChangeListener(key: Any, listener: (BeatmapInfo?) -> Unit) {
        onBeatmapChangeListeners = onBeatmapChangeListeners + (key to listener)
    }

    fun removeOnBeatmapChangeListener(key: Any) {
        onBeatmapChangeListeners = onBeatmapChangeListeners - key
    }


    fun onUpdate(secondsElapsed: Float) {
        // A song that stopped while it was meant to play reached its end, outside of gameplay the
        // next one plays, as it did in the old menu.
        if (isPlaybackWanted && !isLoading && songService.status == Status.STOPPED && !songService.isGaming) {
            isPlaybackWanted = false
            next()
        }

        if (songService.status == Status.PLAYING) {
            val positon = songService.position

            currentTimingControlPoint = timingControlPointManager?.controlPointAt(positon.toDouble()) ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.controlPointAt(positon.toDouble()) ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
        } else {
            currentTimingControlPoint = timingControlPointManager?.defaultControlPoint ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.defaultControlPoint ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
        }

        RythimManager.onUpdate(secondsElapsed)
    }

}

object RythimManager {

    /**
     * The current beat index.
     */
    var beatIndex = 0
        private set(value) {
            if (field != value) {
                field = value
                onBeatChangeListeners.values.forEach { it() }
            }
        }

    /**
     * The current beat signature.
     */
    var beatSignature = 4
        private set(value) {
            if (field != value) {
                field = value
                beatIndex = 0
            }
        }

    /**
     * The elapsed time since the last beat in milliseconds.
     */
    var beatElapsed = 0.0
        private set

    /**
     * The current beat length in milliseconds.
     */
    var beatLength = 0.0
        private set


    /**
     * Whether the current section is in kiai time or not.
     */
    var isKiai = false
        private set


    private var onBeatChangeListeners = mapOf<Any, () -> Unit>()


    private fun updateControlPointInformation(secondsElapsed: Float, timingPoint: TimingControlPoint, effectPoint: EffectControlPoint) {
        beatSignature = timingPoint.timeSignature
        beatLength = timingPoint.msPerBeat
        isKiai = effectPoint.isKiai

        beatElapsed += secondsElapsed * 1000f

        while (beatElapsed >= beatLength) {
            beatElapsed -= beatLength.toFloat()
            beatIndex = (beatIndex + 1) % beatSignature
        }
    }


    fun onUpdate(secondsElapsed: Float) {
        if (MusicManager.isPlaying) {
            updateControlPointInformation(
                secondsElapsed,
                MusicManager.currentTimingControlPoint,
                MusicManager.currentEffectControlPoint
            )
        } else {
            updateControlPointInformation(
                secondsElapsed,
                TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT,
                EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
            )
        }
    }


    fun addOnBeatChangeListener(key: Any, listener: () -> Unit) {
        onBeatChangeListeners = onBeatChangeListeners + (key to listener)
    }

    fun removeOnBeatChangeListener(key: Any) {
        onBeatChangeListeners = onBeatChangeListeners - key
    }
}