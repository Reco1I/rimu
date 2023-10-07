package com.rian.osu.beatmap.parser

import com.reco1l.framework.lang.ignoreException
import com.reco1l.framework.data.md5
import com.rian.osu.beatmap.BeatmapData
import com.rian.osu.beatmap.constants.BeatmapSection
import com.rian.osu.beatmap.parser.sections.BeatmapColorParser
import com.rian.osu.beatmap.parser.sections.BeatmapControlPointsParser
import com.rian.osu.beatmap.parser.sections.BeatmapDifficultyParser
import com.rian.osu.beatmap.parser.sections.BeatmapEventsParser
import com.rian.osu.beatmap.parser.sections.BeatmapGeneralParser
import com.rian.osu.beatmap.parser.sections.BeatmapHitObjectsParser
import com.rian.osu.beatmap.parser.sections.BeatmapMetadataParser
import com.rian.osu.beatmap.sections.BeatmapHitObjects
import com.rian.osu.utils.HitObjectStackEvaluator.applyStacking
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

// MODIFIED: I've moved the 'file' property to local in the function 'decode' to make any instance of
// BeatmapDecoder recyclable. I've also removed every try-catch statement because we want to delegate
// exception handling to whatever is using the decoder so we know from upper level what's going on with
// the beatmap.
// - Reco1l
/**
 * A parser for parsing `.osu` files.
 */
class BeatmapDecoder : Closeable {

    /**
     * The `BufferedSource` responsible for reading the beatmap file's contents.
     */
    private var source: BufferedSource? = null

    /**
     * Parses the `.osu` file.
     *
     * @param withHitObjects Whether to parse hit objects. This will improve parsing time significantly.
     * @return A `Beatmap` containing relevant information of the beatmap file,
     * `null` if the beatmap file cannot be opened or a line could not be parsed.
     */
    fun decode(file: File, withHitObjects: Boolean): BeatmapData {

        close()
        source = file.source().buffer()

        val head = source!!.readUtf8Line()!!

        val pattern = Pattern.compile("osu file format v(\\d+)")
        val matcher = pattern.matcher(head)

        if (!matcher.find()) {
            throw IOException("Invalid file format.")
        }

        val beatmapFormatVersion = matcher.group(1)?.toIntOrNull() ?: DEFAULT_FORMAT_VERSION

        var currentLine: String?
        var currentSection: BeatmapSection? = null
        val beatmapData = BeatmapData().apply {
            md5 = file.md5
            folder = file.parent
            filename = file.path
            formatVersion = beatmapFormatVersion
        }

        while (source?.readUtf8Line().also { currentLine = it } != null) {
            // Check if beatmap is not an osu!standard beatmap
            if (beatmapData.general.mode != 0) {
                // Silently ignore (do not log anything to the user)
                throw UnsupportedOperationException("Currently only std beatmaps are supported.")
            }

            var line = currentLine ?: continue

            // Handle space comments
            if (line.startsWith(" ") || line.startsWith("_")) {
                continue
            }

            // Now that we've handled space comments, we can trim space
            line = line.trim { it <= ' ' }

            // Handle C++ style comments and empty lines
            if (line.startsWith("//") || line.isEmpty()) {
                continue
            }

            // [SectionName]
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = BeatmapSection.parse(line.substring(1, line.length - 1))
                continue
            }

            if (currentSection == null) {
                continue
            }

            when (currentSection) {
                BeatmapSection.General ->
                    BeatmapGeneralParser.parse(beatmapData, line)

                BeatmapSection.Metadata ->
                    BeatmapMetadataParser.parse(beatmapData, line)

                BeatmapSection.Difficulty ->
                    BeatmapDifficultyParser.parse(beatmapData, line)

                BeatmapSection.Events ->
                    BeatmapEventsParser.parse(beatmapData, line)

                BeatmapSection.TimingPoints ->
                    BeatmapControlPointsParser.parse(beatmapData, line)

                BeatmapSection.Colors ->
                    BeatmapColorParser.parse(beatmapData, line)

                BeatmapSection.HitObjects ->
                    if (withHitObjects) {
                        beatmapData.hitObjects = BeatmapHitObjects()
                        BeatmapHitObjectsParser.parse(beatmapData, line)
                    }

                else -> continue
            }
        }
        close()
        populateObjectData(beatmapData)

        return beatmapData
    }

    override fun close() {
        ignoreException { source?.close() }

        source = null
    }

    companion object {

        /**
         * The default beatmap format version.
         */
        const val DEFAULT_FORMAT_VERSION = 14

        /**
         * Populates the object scales of a `Beatmap`.
         *
         * @param beatmapData The `Beatmap` whose object scales will be populated.
         */
        fun populateObjectData(beatmapData: BeatmapData) {
            val scale = (1 - 0.7f * (beatmapData.difficulty.cs - 5) / 5) / 2

            beatmapData.hitObjects?.apply {
                resetStacking()

                getObjects().apply {
                    forEach { it.scale = scale }

                    applyStacking(
                        beatmapData.formatVersion,
                        this,
                        beatmapData.difficulty.ar,
                        beatmapData.general.stackLeniency
                    )
                }
            }
        }
    }
}
