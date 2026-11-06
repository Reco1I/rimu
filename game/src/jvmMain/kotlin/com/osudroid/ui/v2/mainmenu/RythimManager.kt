package com.osudroid.ui.v2.mainmenu

import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.beatmaps.timings.EffectControlPoint
import com.osudroid.beatmaps.timings.EffectControlPointManager
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.beatmaps.timings.TimingControlPointManager
import com.osudroid.data.BeatmapInfo
import com.reco1l.verktex.Logger
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * The beat of the song playing in the menu, from the control points of its beatmap.
 */
object RythimManager {

    /**
     * The current timing control point.
     */
    var currentTimingControlPoint = TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
        private set

    /**
     * The current effect control point.
     */
    var currentEffectControlPoint = EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT
        private set

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

    // The beatmap the control points were loaded from.
    private var loadedBeatmap: BeatmapInfo? = null

    private var timingControlPointManager: TimingControlPointManager? = null
    private var effectControlPointManager: EffectControlPointManager? = null


    private fun loadControlPoints(beatmap: BeatmapInfo?) {
        loadedBeatmap = beatmap
        timingControlPointManager = null
        effectControlPointManager = null

        if (beatmap == null) {
            return
        }

        try {
            val data = BeatmapParser(beatmap.path).parse(false)

            timingControlPointManager = data.controlPoints.timing
            effectControlPointManager = data.controlPoints.effect
        } catch (e: Exception) {
            Logger.e("RythimManager", "Failed to load the control points of ${beatmap.path}", e)
        }
    }

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


    /**
     * Follows the music, and plays the next song when one ends. Called every frame by the menu.
     */
    fun onUpdate(secondsElapsed: Float) {
        val music = GlobalManager.getInstance().music
        music.update()

        if (music.currentBeatmap != loadedBeatmap) {
            loadControlPoints(music.currentBeatmap)
        }

        if (music.isPlaying) {
            val position = music.position.toDouble()

            currentTimingControlPoint = timingControlPointManager?.controlPointAt(position) ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.controlPointAt(position) ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT

            updateControlPointInformation(secondsElapsed, currentTimingControlPoint, currentEffectControlPoint)
        } else {
            currentTimingControlPoint = timingControlPointManager?.defaultControlPoint ?: TimingControlPointManager.DEFAULT_TIMING_CONTROL_POINT
            currentEffectControlPoint = effectControlPointManager?.defaultControlPoint ?: EffectControlPointManager.DEFAULT_EFFECT_CONTROL_POINT

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
