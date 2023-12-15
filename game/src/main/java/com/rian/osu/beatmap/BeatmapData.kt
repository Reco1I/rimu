package com.rian.osu.beatmap

import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapColor
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.beatmap.sections.BeatmapEvents
import com.rian.osu.beatmap.sections.BeatmapGeneral
import com.rian.osu.beatmap.sections.BeatmapHitObjects
import com.rian.osu.beatmap.sections.BeatmapMetadata

// MODIFIED: I've converted 'hitObjects' to a nullable type so we can determine if the HitObjects
// were initially parsed or not.

/**
 * Represents a beatmap.
 */
class BeatmapData : Cloneable {
    /**
     * The format version of this beatmap.
     */
    @JvmField
    var formatVersion = 14

    /**
     * The general section of this beatmap.
     */
    var general = BeatmapGeneral()
        private set

    /**
     * The metadata section of this beatmap.
     */
    var metadata = BeatmapMetadata()
        private set

    /**
     * The difficulty section of this beatmap.
     */
    var difficulty = BeatmapDifficulty()
        private set

    /**
     * The events section of this beatmap.
     */
    var events = BeatmapEvents()
        private set

    /**
     * The colors section of this beatmap.
     */
    var colors = BeatmapColor()
        private set

    /**
     * The control points of this beatmap.
     */
    var controlPoints = BeatmapControlPoints()
        private set

    /**
     * The hit objects of this beatmap.
     */
    var hitObjects: BeatmapHitObjects? = null
        internal set

    /**
     * Raw timing points data.
     */
    var rawTimingPoints = mutableListOf<String>()
        private set

    /**
     * Raw hit objects data.
     */
    var rawHitObjects = mutableListOf<String>()
        private set

    /**
     * The path of parent folder of this beatmap.
     */
    @JvmField
    var folder: String? = null

    /**
     * The name of the `.osu` file of this beatmap.
     */
    @JvmField
    var filename = ""

    /**
     * The MD5 hash of this beatmap.
     */
    @JvmField
    var md5 = ""

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    fun getOffsetTime(time: Double) = time + if (formatVersion < 5) 24 else 0

    /**
     * Returns a time combined with beatmap-wide time offset.
     *
     * Beatmap version 4 and lower had an incorrect offset. Stable has this set as 24ms off.
     *
     * @param time The time.
     */
    fun getOffsetTime(time: Int) = time + if (formatVersion < 5) 24 else 0

    /**
     * Gets the max combo of this beatmap.
     */
    val maxCombo by lazy {

        hitObjects?.getObjects()?.sumOf {
            if (it is Slider) it.nestedHitObjects.size else 1
        }
    }


    public override fun clone() =
        (super.clone() as BeatmapData).apply {
            general = this@BeatmapData.general.copy()
            metadata = this@BeatmapData.metadata.copy()
            difficulty = this@BeatmapData.difficulty.clone()
            events = this@BeatmapData.events.clone()
            colors = this@BeatmapData.colors.clone()
            controlPoints = this@BeatmapData.controlPoints.clone()
            hitObjects = this@BeatmapData.hitObjects?.clone()
        }
}