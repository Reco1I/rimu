package com.rian.osu.beatmap.parser.sections

import com.reco1l.toolkt.graphics.Color4
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.timings.BreakPeriod
import kotlin.math.max

/**
 * A parser for parsing a beatmap's events section.
 */
object BeatmapEventsParser : BeatmapSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = line
        .split("\\s*,\\s*".toRegex())
        .dropLastWhile { it.isEmpty() }
        .let {
            if (it.size >= 3) {
                if (line.startsWith("0,0")) {
                    beatmapData.events.backgroundFilename = it[2].substring(1, it[2].length - 1)
                }

                if (line.startsWith("2") || line.startsWith("Break")) {
                    val start = beatmapData.getOffsetTime(parseInt(it[1]))
                    val end = max(start, beatmapData.getOffsetTime(parseInt(it[2])))

                    beatmapData.events.breaks.add(BreakPeriod(start.toFloat(), end.toFloat()))
                }

                if (line.startsWith("1") || line.startsWith("Video")) {
                    beatmapData.events.videoStartTime = parseInt(it[1])
                    beatmapData.events.videoFilename = it[2].substring(1, it[2].length - 1)
                }
            }
    
            if (it.size >= 5 && line.startsWith("3")) {
                beatmapData.events.backgroundColor = Color4(
                    parseInt(it[2]),
                    parseInt(it[3]),
                    parseInt(it[4])
                )
            }
        }
}
