package com.osudroid.ui.v2.songselect

import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import org.junit.Assert
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm

class BeatmapFilterTest {

    private val sets = listOf(
        set("a", title = "Blue Zenith", artist = "xi", beatmaps = listOf(
            beatmap(version = "Easy", stars = 2f, ar = 5f),
            beatmap(version = "FOUR DIMENSIONS", stars = 7.5f, ar = 10f)
        )),
        set("b", title = "Adventure", artist = "Porter Robinson", beatmaps = listOf(
            beatmap(version = "Insane", stars = 5f, ar = 9f, dateImported = 3)
        )),
        set("c", title = "Crystallized", artist = "camellia", beatmaps = listOf(
            beatmap(version = "Hard", stars = 4f, ar = 8f, dateImported = 2),
            beatmap(version = "Insane", stars = 6f, ar = 9.3f, dateImported = 2)
        ))
    )

    private fun BeatmapFilter.directories() = apply(sets, DifficultyAlgorithm.droid).map { it.directory }


    @Test
    fun `Sorts by title by default`() {
        Assert.assertEquals(listOf("b", "a", "c"), BeatmapFilter().directories())
    }

    @Test
    fun `Matches text in any field ignoring case`() {
        Assert.assertEquals(listOf("b"), BeatmapFilter(query = "PORTER").directories())
        Assert.assertEquals(listOf("a", "c"), BeatmapFilter(query = "z").directories())
    }

    @Test
    fun `Keeps only the matching difficulties`() {
        val result = BeatmapFilter(query = "insane").apply(sets, DifficultyAlgorithm.droid)

        Assert.assertEquals(listOf("b", "c"), result.map { it.directory })
        Assert.assertEquals(listOf("Insane"), result[1].beatmaps.map { it.version })
    }

    @Test
    fun `Compares statistics`() {
        Assert.assertEquals(listOf("a", "c"), BeatmapFilter(query = "star>=6").directories())
        Assert.assertEquals(listOf("c"), BeatmapFilter(query = "ar>9 star<7").directories())
        Assert.assertEquals(listOf("b"), BeatmapFilter(query = "ar=9 insane").directories())
        Assert.assertEquals(listOf("c"), BeatmapFilter(query = "ar=9.3").directories())
    }

    @Test
    fun `Sorts numeric statistics highest first`() {
        Assert.assertEquals(listOf("a", "c", "b"), BeatmapFilter(sortOrder = BeatmapSortOrder.StarRating).directories())
        Assert.assertEquals(listOf("b", "c", "a"), BeatmapFilter(sortOrder = BeatmapSortOrder.DateImported).directories())
    }

    @Test
    fun `Groups by difficulty`() {
        val result = BeatmapFilter(grouping = BeatmapGrouping.Difficulty, sortOrder = BeatmapSortOrder.StarRating)
            .apply(sets, DifficultyAlgorithm.droid)

        Assert.assertEquals(listOf(7.5f, 6f, 5f, 4f, 2f), result.map { it.beatmaps.single().droidStarRating })
    }

    @Test
    fun `Filters favorites and collections`() {
        Assert.assertEquals(listOf("c"), BeatmapFilter(favoritesOnly = true).apply(sets, DifficultyAlgorithm.droid, setOf("c")).map { it.directory })
        Assert.assertEquals(listOf("b"), BeatmapFilter(collection = "x").apply(sets, DifficultyAlgorithm.droid, collectionDirectories = setOf("b")).map { it.directory })
        Assert.assertEquals(emptyList<String>(), BeatmapFilter(collection = "x").directories())
    }


    private fun set(directory: String, title: String, artist: String, beatmaps: List<BeatmapInfo>) =
        BeatmapSetInfo(null, directory, beatmaps.map { it.copy(setDirectory = directory, title = title, titleUnicode = title, artist = artist, artistUnicode = artist) })

    private fun beatmap(version: String, stars: Float, ar: Float, dateImported: Long = 1) = BeatmapInfo(
        filename = "$version.osu",
        md5 = "",
        id = null,
        audioFilename = "audio.mp3",
        backgroundFilename = null,
        status = null,
        setDirectory = "",
        setId = null,
        title = "",
        titleUnicode = "",
        artist = "",
        artistUnicode = "",
        creator = "mapper",
        version = version,
        tags = "",
        source = "",
        dateImported = dateImported,
        approachRate = ar,
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
        hitCircleCount = 0,
        spinnerCount = 0,
        sliderCount = 0,
        maxCombo = 0,
        epilepsyWarning = false
    )
}
