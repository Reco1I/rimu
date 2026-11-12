package com.osudroid.ui.v2.test

import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import com.osudroid.data.ModPreset
import com.osudroid.data.ScoreInfo
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModHidden
import com.osudroid.multiplayer.api.data.PlayerStatus
import com.osudroid.multiplayer.api.data.RoomBeatmap
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.api.data.RoomPlayer
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.Platform
import kotlin.random.Random
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

/**
 * Made up data for the UI tests, so components can be tried without a library, scores or a server.
 *
 * The beatmaps are copies of one real beatmap (`assets/test/beatmap.osu`) with made up metadata, so
 * they can be played, see [TestGameContext].
 */
object TestData {

    /**
     * Prefix of the directories of the sample beatmap sets, so they never match a real one.
     */
    private const val DIRECTORY_PREFIX = "uitest-"

    /**
     * The audio file of every beatmap, an empty one: songs are played by a [FakeMusicService].
     */
    const val AUDIO_FILENAME = "audio.mp3"

    // The beatmap every sample beatmap is a copy of.
    private val template by lazy {
        Platform.provider.read("test/beatmap.osu", isExternal = false)?.decodeToString()
            ?: error("The test beatmap (assets/test/beatmap.osu) is missing")
    }

    private val TITLES = listOf("Blue Zenith", "Adventure", "Crystallized", "Freedom Dive", "Galaxy Collapse",
        "Harumachi Clover", "Kimi no Bouken", "Lagtrain", "Night of Knights", "Odoru Mizu", "Padoru", "Quaoar",
        "Rainbow Tylenol", "Snow Halation", "Teo", "Uta", "Valsinas", "World's End Dancehall", "Xeroa", "Yomi Yori",
        "Zen Zen Zense")

    private val VERSIONS = listOf("Easy", "Normal", "Hard", "Insane", "Expert", "Extra")

    private val PLAYERS = listOf("Rei", "Asuka", "Shinji", "Misato", "Kaworu", "Toji", "Kensuke", "Mari")


    /**
     * Beatmap sets with made up metadata, always the same ones.
     */
    fun beatmapSets(): List<BeatmapSetInfo> {
        val random = Random(727)

        return TITLES.mapIndexed { index, title ->
            val count = random.nextInt(1, VERSIONS.size + 1)
            // Absolute, outside the beatmaps folder, see GameFolders.beatmapSetPath.
            val directory = (Platform.storage.cacheDirectory / "$DIRECTORY_PREFIX$index $title").toString()

            BeatmapSetInfo(index, directory, VERSIONS.take(count).mapIndexed { difficulty, version ->
                beatmap(directory, title, version, stars = difficulty * 1.3f + random.nextFloat() + 1f)
            })
        }
    }

    /**
     * Local scores on [beatmap], best first.
     */
    fun scores(beatmap: BeatmapInfo): List<ScoreInfo> {
        val random = Random(beatmap.md5.hashCode())

        return PLAYERS.shuffled(random).take(random.nextInt(3, PLAYERS.size)).mapIndexed { index, player ->
            val misses = index + random.nextInt(0, 3)
            val hit100 = index * 4 + random.nextInt(0, 10)

            ScoreInfo(
                beatmapMD5 = beatmap.md5,
                playerName = player,
                replayFilename = "",
                mods = if (index % 3 == 0) ModHashMap().apply { put(ModHidden::class) }.serializeMods() else "[]",
                score = 10_000_000L - index * 650_000L - random.nextLong(0, 100_000),
                maxCombo = 900 - index * 70,
                mark = listOf("XH", "SH", "A", "B", "C", "D")[index.coerceAtMost(5)],
                hit300k = 120,
                hit300 = 700 - hit100 - misses,
                hit100k = 10,
                hit100 = hit100,
                hit50 = index,
                misses = misses,
                time = currentTimeMillis() - index * 86_400_000L,
                sliderHeadHits = null,
                sliderTickHits = null,
                sliderRepeatHits = null,
                sliderEndHits = null
            )
        }
    }

    /**
     * Mod presets shown by the mod menu.
     */
    fun modPresets() = listOf(
        ModPreset(name = "HDHR", serializedMods = ModHashMap().apply {
            put(ModHidden::class)
            put(ModHardRock::class)
        }.serializeMods()),
        ModPreset(name = "Speed", serializedMods = ModHashMap().apply { put(ModDoubleTime::class) }.serializeMods()),
    )

    /**
     * A player of a multiplayer room.
     */
    fun roomPlayer(id: Long, status: PlayerStatus = PlayerStatus.NotReady) = RoomPlayer(
        id = id,
        // The id of the guest is negative.
        name = PLAYERS[id.mod(PLAYERS.size)],
        rank = (id * 137).mod(10_000) + 1,
        status = status,
        team = null,
        mods = RoomMods()
    )

    /**
     * The room representation of [beatmap].
     */
    fun roomBeatmap(beatmap: BeatmapInfo) = RoomBeatmap(
        md5 = beatmap.md5,
        title = beatmap.titleUnicode,
        artist = beatmap.artistUnicode,
        creator = beatmap.creator,
        version = beatmap.version
    )


    /**
     * The `.osu` file of [beatmap]: the test beatmap with the metadata of [beatmap].
     */
    fun beatmapFile(beatmap: BeatmapInfo) = beatmapFile(beatmap.title, beatmap.version)

    private fun beatmapFile(title: String, version: String): ByteString {
        val metadata = mapOf("Title" to title, "Artist" to ARTIST, "Creator" to CREATOR, "Version" to version)

        return template.split('\n').joinToString("\n") { line ->
            val key = line.substringBefore(':', "")
            val value = metadata[key] ?: return@joinToString line
            // The line ending is kept, the file's are CRLF.
            "$key:$value" + if (line.endsWith('\r')) "\r" else ""
        }.encodeUtf8()
    }

    private fun beatmap(directory: String, title: String, version: String, stars: Float) = BeatmapInfo(
        filename = "$version.osu",
        md5 = beatmapFile(title, version).md5().hex(),
        id = null,
        audioFilename = AUDIO_FILENAME,
        backgroundFilename = null,
        status = null,
        setDirectory = directory,
        setId = null,
        title = title,
        titleUnicode = title,
        artist = ARTIST,
        artistUnicode = ARTIST,
        creator = CREATOR,
        version = version,
        tags = "",
        source = "",
        dateImported = 0,
        approachRate = 9f,
        overallDifficulty = 8f,
        circleSize = 4f,
        hpDrainRate = 5f,
        droidStarRating = stars,
        standardStarRating = stars,
        bpmMax = 180f,
        bpmMin = 180f,
        mostCommonBPM = 180f,
        length = 120000,
        previewTime = -1,
        hitCircleCount = 300,
        spinnerCount = 2,
        sliderCount = 200,
        maxCombo = 900,
        epilepsyWarning = false
    )


    private const val ARTIST = "Artist"
    private const val CREATOR = "Mapper"
}
