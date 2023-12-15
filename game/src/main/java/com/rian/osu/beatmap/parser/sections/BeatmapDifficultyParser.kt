package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.BeatmapData

/**
 * A parser for parsing a beatmap's difficulty section.
 */
object BeatmapDifficultyParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = splitProperty(line).let {
        when (it[0]) {
            "CircleSize" -> beatmapData.difficulty.cs = parseFloat(it[1])
            "OverallDifficulty" -> beatmapData.difficulty.od = parseFloat(it[1])
            "ApproachRate" -> beatmapData.difficulty.ar = parseFloat(it[1])
            "HPDrainRate" -> beatmapData.difficulty.hp = parseFloat(it[1])
            "SliderMultiplier" -> beatmapData.difficulty.sliderMultiplier = parseDouble(it[1]).coerceIn(0.4, 3.6)
            "SliderTickRate" -> beatmapData.difficulty.sliderTickRate = parseDouble(it[1]).coerceIn(0.5, 8.0)
        }
    }
}
