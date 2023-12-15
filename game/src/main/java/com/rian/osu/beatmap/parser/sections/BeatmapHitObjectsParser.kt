package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.constants.HitObjectType
import com.rian.osu.beatmap.hitobject.*
import com.rian.osu.math.Precision.almostEqualsNumber
import com.rian.osu.math.Vector2
import kotlin.math.max

/**
 * A parser for parsing a beatmap's hit objects section.
 */
object BeatmapHitObjectsParser : BeatmapSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = line
        .split(",")
        .dropLastWhile { it.isEmpty() }
        .let {
            if (it.size < 4) {
                throw UnsupportedOperationException("Malformed hit object")
            }

            val time = beatmapData.getOffsetTime(parseDouble(it[2]))
            val type = HitObjectType.valueOf(parseInt(it[3]) % 16)

            val position = Vector2(
                parseInt(it[0]).toFloat(),
                parseInt(it[1]).toFloat()
            )

            val obj = when {
                type === HitObjectType.Normal || type === HitObjectType.NormalNewCombo ->
                    createCircle(time, position)

                type === HitObjectType.Slider || type === HitObjectType.SliderNewCombo ->
                    createSlider(beatmapData, time, position, it)

                type === HitObjectType.Spinner ->
                    createSpinner(beatmapData, time, it)

                else -> throw UnsupportedOperationException("Malformed hit object")
            }

            beatmapData.rawHitObjects.add(line)
            beatmapData.hitObjects!!.add(obj)
        }

    private fun createCircle(time: Double, position: Vector2) = HitCircle(time, position)

    @Throws(UnsupportedOperationException::class)
    private fun createSlider(beatmapData: BeatmapData, time: Double, startPosition: Vector2, pars: List<String>): Slider {
        if (pars.size < 8) {
            throw UnsupportedOperationException("Malformed slider")
        }

        val repeat = parseInt(pars[6])
        val rawLength = max(0.0, parseDouble(pars[7]))

        if (repeat > 9000) {
            throw UnsupportedOperationException("Repeat count is way too high")
        }

        val curvePointsData = pars[5].split("[|]".toRegex()).dropLastWhile { it.isEmpty() }
        var sliderType = SliderPathType.parse(curvePointsData[0][0])

        val curvePoints = mutableListOf<Vector2>().apply { add(Vector2(0f)) }

        curvePointsData.run {
            for (i in 1 until size) {
                this[i].split(":".toRegex()).dropLastWhile { it.isEmpty() }.let {
                    val curvePointPosition = Vector2(
                        parseInt(it[0]).toFloat(),
                        parseInt(it[1]).toFloat()
                    )

                    curvePoints.add(curvePointPosition - startPosition)
                }
            }
        }

        curvePoints.let {
            // A special case for old beatmaps where the first
            // control point is in the position of the slider.
            if (it.size >= 2 && it[0] == it[1]) {
                it.removeFirst()
            }

            // Edge-case rules (to match stable).
            if (sliderType === SliderPathType.PerfectCurve) {
                if (it.size != 3) {
                    sliderType = SliderPathType.Bezier
                } else if (almostEqualsNumber(
                        0f,
                        (it[1].y - it[0].y) * (it[2].x - it[0].x) -
                                (it[1].x - it[0].x) * (it[2].y - it[0].y)
                    )
                ) {
                    // osu-stable special-cased co-linear perfect curves to a linear path
                    sliderType = SliderPathType.Linear
                }
            }
        }

        val path = SliderPath(sliderType, curvePoints, rawLength)
        val timingControlPoint = beatmapData.controlPoints.timing.controlPointAt(time)
        val difficultyControlPoint = beatmapData.controlPoints.difficulty.controlPointAt(time)

        return Slider(
            time,
            startPosition,
            timingControlPoint,
            difficultyControlPoint,
            repeat,
            path,
            beatmapData.difficulty.sliderMultiplier,
            beatmapData.difficulty.sliderTickRate,
            // Prior to v8, speed multipliers don't adjust for how many ticks are generated over the same distance.
            // this results in more (or less) ticks being generated in <v8 maps for the same time duration.
            if (beatmapData.formatVersion < 8) 1 / difficultyControlPoint.speedMultiplier else 1.0
        )
    }

    private fun createSpinner(beatmapData: BeatmapData, time: Double, pars: List<String>) =
        Spinner(time, beatmapData.getOffsetTime(parseInt(pars[5])).toDouble())
}
