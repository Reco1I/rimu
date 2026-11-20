package com.osudroid.game.loading

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.timings.BreakPeriod
import com.osudroid.beatmaps.timings.EffectControlPoint
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.data.BeatmapInfo
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes
import com.osudroid.difficulty.attributes.TimedDifficultyAttributes
import com.osudroid.game.SliderPath
import com.osudroid.utils.ModHashMap
import com.reco1l.verktex.math.Path
import kotlin.concurrent.Volatile

/**
 * A beatmap loaded to be played with some mods: what every attempt at it shares, kept while it's retried.
 */
class LoadedPlay(
    val beatmapInfo: BeatmapInfo,
    val parsedBeatmap: Beatmap,
    val playableBeatmap: DroidPlayableBeatmap,

    /**
     * The mods of the play, the relevant ones alone.
     */
    val mods: ModHashMap,

    /**
     * The file of the music.
     */
    val audioPath: String,

    /**
     * The objects of [playableBeatmap], in order.
     */
    val objects: Array<HitObject>,

    val breakPeriods: Array<BreakPeriod>,
    val timingControlPoints: Array<TimingControlPoint>,
    val effectControlPoints: Array<EffectControlPoint>,

    /**
     * The paths of the sliders, in the order of the objects.
     */
    val sliderPaths: Array<SliderPath>,

    /**
     * The paths the bodies of the sliders are drawn along, in the order of the objects.
     */
    val sliderRenderPaths: Array<Path>,

    /**
     * The difficulty of the beatmap at each object, for the pp shown while playing.
     */
    val timedDifficulty: TimedDifficulty
) {

    /**
     * The start time of the first object, in seconds.
     */
    val firstObjectStartTime = objects[0].startTime.toFloat() / 1000

    /**
     * The end time of the last object, in seconds.
     */
    val lastObjectEndTime = objects[objects.size - 1].endTime.toFloat() / 1000
}

/**
 * The difficulty of a beatmap at each of its objects, calculated in the background when the pp counter is shown.
 */
class TimedDifficulty {

    @Volatile
    var droid: Array<TimedDifficultyAttributes<DroidDifficultyAttributes>>? = null

    @Volatile
    var standard: Array<TimedDifficultyAttributes<StandardDifficultyAttributes>>? = null

    /**
     * Called on the update thread once it's calculated, by the attempt being played.
     */
    @Volatile
    var onCalculated: (() -> Unit)? = null
}
