package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.timings.DifficultyControlPoint
import com.rian.osu.beatmap.timings.TimingControlPoint

/**
 * A parser for parsing a beatmap's timing points section.
 */
object BeatmapControlPointsParser : BeatmapSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = line
        .split(",".toRegex())
        .dropLastWhile { it.isEmpty() }
        .let {
            if (it.size < 2) {
                throw UnsupportedOperationException("Malformed timing point")
            }

            val time = beatmapData.getOffsetTime(parseDouble(it[0].trim { s -> s <= ' ' }))

            // msPerBeat is allowed to be NaN to handle an edge case in which some
            // beatmaps use NaN slider velocity to disable slider tick generation.
            val msPerBeat = parseDouble(it[1].trim { s -> s <= ' ' }, allowNaN = true)

            val timeSignature = it.getOrNull(2)?.let { s -> parseInt(s) } ?: 4
            if (timeSignature < 1) {
                throw UnsupportedOperationException("The numerator of a time signature must be positive")
            }

            val timingChange = it.getOrNull(6)?.let { s -> s == "1" } ?: true
            val manager = beatmapData.controlPoints

            if (timingChange) {
                if (msPerBeat.isNaN()) {
                    throw UnsupportedOperationException("Beat length cannot be NaN in a timing control point")
                }

                manager.timing.add(TimingControlPoint(time, msPerBeat, timeSignature))
            }

            manager.difficulty.add(
                DifficultyControlPoint(
                    time,  // If msPerBeat is NaN, speedMultiplier should still be 1 because all comparisons against NaN are false.
                    if (msPerBeat < 0) 100 / -msPerBeat else 1.0,
                    !msPerBeat.isNaN()
                )
            )

            beatmapData.rawTimingPoints.add(line)
            Unit
        }
}
