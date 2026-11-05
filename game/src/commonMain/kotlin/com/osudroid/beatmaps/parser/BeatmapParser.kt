package com.osudroid.beatmaps.parser

import com.reco1l.verktex.Logger
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapProcessor
import com.osudroid.GameMode
import com.osudroid.beatmaps.constants.BeatmapSection
import com.osudroid.beatmaps.parser.sections.BeatmapColorParser
import com.osudroid.beatmaps.parser.sections.BeatmapControlPointsParser
import com.osudroid.beatmaps.parser.sections.BeatmapDifficultyParser
import com.osudroid.beatmaps.parser.sections.BeatmapEventsParser
import com.osudroid.beatmaps.parser.sections.BeatmapGeneralParser
import com.osudroid.beatmaps.parser.sections.BeatmapHitObjectsParser
import com.osudroid.beatmaps.parser.sections.BeatmapMetadataParser
import com.osudroid.utils.SystemFileSystem
import com.osudroid.utils.md5Checksum
import okio.use
import okio.BufferedSource
import okio.IOException
import okio.Path.Companion.toPath
import okio.buffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlin.jvm.JvmOverloads

/**
 * A parser for parsing `.osu` files.
 */
class BeatmapParser {
    /**
     * The path to the `.osu` file.
     */
    private val path: String

    /**
     * The [CoroutineScope] to use for coroutines.
     */
    private val scope: CoroutineScope?

    /**
     * The precomputed MD5 hash of the beatmap file, if available.
     *
     * This is used to avoid unnecessary MD5 calculations when parsing.
     */
    private val precomputedMD5: String?

    /**
     * @param path The path to the `.osu` file.
     * @param scope The [CoroutineScope] to use for coroutines.
     */
    @JvmOverloads
    constructor(path: String, scope: CoroutineScope? = null, precomputedMD5: String? = null) {
        this.path = path
        this.scope = scope
        this.precomputedMD5 = precomputedMD5
    }

    /**
     * Parses the `.osu` file.
     *
     * @param withHitObjects Whether to parse hit objects. Setting this to `false` will improve parsing time significantly.
     * @param mode The [GameMode] to parse for. Defaults to [GameMode.Standard].
     * @return A [Beatmap] containing relevant information of the beatmap file.
     * @throws IOException If an I/O error occurs while reading the file.
     * @throws NumberFormatException If the beatmap's file version cannot be determined.
     * @throws IllegalArgumentException If the beatmap is not an osu!standard beatmap.
     */
    @JvmOverloads
    @Throws(IOException::class, IllegalArgumentException::class, NumberFormatException::class)
    fun parse(withHitObjects: Boolean, mode: GameMode = GameMode.Standard) = openFile().use { source ->
        scope?.ensureActive()

        // Check for format version first to avoid unnecessary MD5 calculation.
        val formatVersion = getFormatVersion(source)

        var currentLine: String?
        var currentSection: BeatmapSection? = null

        val beatmap = Beatmap(mode).also {
            it.md5 = precomputedMD5 ?: md5Checksum(path)
            it.filePath = path
            it.formatVersion = formatVersion
        }

        while (source.readUtf8Line().also { currentLine = it } != null) {
            scope?.ensureActive()

            if (beatmap.general.mode != 0) {
                throw IllegalArgumentException("Not an osu!standard beatmap")
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

                // HitObjects are always in the last section (since it is dependent on other things such as
                // difficulty, control points, etc.)
                // We can stop here if we do not need to parse them.
                if (currentSection == BeatmapSection.HitObjects && !withHitObjects) {
                    break
                }

                continue
            }

            if (currentSection == null) {
                continue
            }

            scope?.ensureActive()

            try {
                when (currentSection) {
                    BeatmapSection.General ->
                        BeatmapGeneralParser.parse(beatmap, line, scope)

                    BeatmapSection.Metadata ->
                        BeatmapMetadataParser.parse(beatmap, line, scope)

                    BeatmapSection.Difficulty ->
                        BeatmapDifficultyParser.parse(beatmap, line, scope)

                    BeatmapSection.Events ->
                        BeatmapEventsParser.parse(beatmap, line, scope)

                    BeatmapSection.TimingPoints ->
                        BeatmapControlPointsParser.parse(beatmap, line, scope)

                    BeatmapSection.Colors ->
                        BeatmapColorParser.parse(beatmap, line, scope)

                    BeatmapSection.HitObjects ->
                        BeatmapHitObjectsParser.parse(beatmap, line, scope)

                    else -> continue
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                Logger.e("BeatmapParser.parse", "Unable to parse line", e)
            }
        }

        val processor = BeatmapProcessor(beatmap, scope)

        processor.preProcess()

        beatmap.hitObjects.objects.forEach {
            scope?.ensureActive()

            it.applyDefaults(beatmap.controlPoints, beatmap.difficulty, mode, scope)
            it.applySamples(beatmap.controlPoints, scope)
        }

        processor.postProcess()

        beatmap
    }

    private fun openFile() = SystemFileSystem.source(path.toPath()).buffer()

    @Throws(IOException::class, NumberFormatException::class)
    private fun getFormatVersion(source: BufferedSource): Int {
        val head = source.readUtf8Line() ?: throw IOException("Empty file")
        if (!Regex("osu file format v(\\d+)").containsMatchIn(head)) {
            throw NumberFormatException("Invalid format version")
        }

        return head.substring(head.indexOf("file format v") + 13).toIntOrNull()
            ?: throw NumberFormatException("Invalid format version")
    }
}
