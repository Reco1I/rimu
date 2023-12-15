package com.rian.osu.beatmap.parser.sections

import com.rian.osu.beatmap.BeatmapData

/**
 * A parser for parsing a beatmap's metadata section.
 */
object BeatmapMetadataParser : BeatmapKeyValueSectionParser() {
    override fun parse(beatmapData: BeatmapData, line: String) = splitProperty(line).let {
        when (it[0]) {
            "Title" -> beatmapData.metadata.title = it[1]
            "TitleUnicode" -> beatmapData.metadata.titleUnicode = it[1]
            "Artist" -> beatmapData.metadata.artist = it[1]
            "ArtistUnicode" -> beatmapData.metadata.artistUnicode = it[1]
            "Creator" -> beatmapData.metadata.creator = it[1]
            "Version" -> beatmapData.metadata.version = it[1]
            "Source" -> beatmapData.metadata.source = it[1]
            "Tags" -> beatmapData.metadata.tags = it[1]
            "BeatmapID" -> beatmapData.metadata.beatmapID = parseInt(it[1])
            "BeatmapSetID" -> beatmapData.metadata.beatmapSetID = parseInt(it[1])
        }
    }
}
