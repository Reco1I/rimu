package com.rian.osu.beatmap.parser.sections

import com.reco1l.toolkt.graphics.Color4
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.ComboColor

/**
 * A parser for parsing a beatmap's colors section.
 */
object BeatmapColorParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = splitProperty(line).let { p ->
        val s = p[1].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (s.size != 3 && s.size != 4) {
            throw UnsupportedOperationException("Color specified in incorrect format (should be R,G,B or R,G,B,A)")
        }

        val color = Color4(
            parseInt(s[0]),
            parseInt(s[1]),
            parseInt(s[2])
        )

        if (p[0].startsWith("Combo")) {
            val index = p[0].substring(5).toIntOrNull() ?: (beatmapData.colors.comboColors.size + 1)

            beatmapData.colors.comboColors.apply {
                add(ComboColor(index, color))
                
                sortBy { it.index }
            }
        }

        if (p[0].startsWith("SliderBorder")) {
            beatmapData.colors.sliderBorderColor = color
        }
    }
}
