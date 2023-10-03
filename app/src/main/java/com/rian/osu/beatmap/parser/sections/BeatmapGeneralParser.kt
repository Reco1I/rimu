package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.constants.BeatmapCountdown
import com.rian.osu.beatmap.constants.SampleBank

/**
 * A parser for parsing a beatmap's general section.
 */
object BeatmapGeneralParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = splitProperty(line).let {
        when (it[0]) {
            "AudioFilename" -> beatmapData.general.audioFilename = it[1]
            "AudioLeadIn" -> beatmapData.general.audioLeadIn = parseInt(it[1])
            "PreviewTime" -> beatmapData.general.previewTime = beatmapData.getOffsetTime(parseInt(it[1]))
            "Countdown" -> beatmapData.general.countdown = BeatmapCountdown.parse(it[1])
            "SampleSet" -> beatmapData.general.sampleBank = SampleBank.parse(it[1])
            "SampleVolume" -> beatmapData.general.sampleVolume = parseInt(it[1])
            "StackLeniency" -> beatmapData.general.stackLeniency = parseFloat(it[1])
            "LetterboxInBreaks" -> beatmapData.general.letterboxInBreaks = it[1] == "1"
            "Mode" -> beatmapData.general.mode = parseInt(it[1])
        }
    }
}
