package com.osudroid.ui.v2.songselect

import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm

/**
 * The order of the beatmaps in the song select.
 */
enum class BeatmapSortOrder(val label: String) {
    Title("Title"),
    Artist("Artist"),
    Creator("Creator"),
    DateImported("Date imported"),
    Bpm("BPM"),
    StarRating("Star rating"),
    Length("Length")
}

/**
 * How the beatmaps are grouped in the song select.
 */
enum class BeatmapGrouping(val label: String) {

    /**
     * Every beatmap set is a panel holding its difficulties.
     */
    BeatmapSet("Sets"),

    /**
     * Every difficulty is a panel of its own.
     */
    Difficulty("Difficulties")
}

/**
 * Which beatmaps the song select shows and how.
 */
data class BeatmapFilter(

    /**
     * The text every shown difficulty must contain, separated by spaces. Terms such as `ar>9`,
     * `star<=5.5`, `bpm>=180` or `length<120` (seconds) compare a statistic of the difficulty
     * instead. `=`, `<`, `>`, `<=` and `>=` are supported.
     */
    val query: String = "",
    val sortOrder: BeatmapSortOrder = BeatmapSortOrder.Title,
    val grouping: BeatmapGrouping = BeatmapGrouping.BeatmapSet,

    /**
     * Whether only the beatmap sets marked as favorite are shown.
     */
    val favoritesOnly: Boolean = false,

    /**
     * The collection the shown beatmap sets must belong to, or null for any.
     */
    val collection: String? = null
) {

    /**
     * Returns the beatmap sets to show, each with the difficulties matching the filter. When
     * grouped by [BeatmapGrouping.Difficulty], every set holds a single difficulty.
     *
     * @param favoriteDirectories The directories of the favorite beatmap sets.
     * @param collectionDirectories The directories of the beatmap sets in [collection].
     */
    fun apply(
        beatmapSets: List<BeatmapSetInfo>,
        algorithm: DifficultyAlgorithm,
        favoriteDirectories: Set<String> = emptySet(),
        collectionDirectories: Set<String>? = null
    ): List<BeatmapSetInfo> {

        val terms = query.lowercase().split(' ').filter { it.isNotEmpty() }.map(::parseTerm)

        val filtered = beatmapSets.mapNotNull { set ->

            if (favoritesOnly && set.directory !in favoriteDirectories) {
                return@mapNotNull null
            }

            if (collection != null && (collectionDirectories == null || set.directory !in collectionDirectories)) {
                return@mapNotNull null
            }

            val beatmaps = set.beatmaps.filter { beatmap -> terms.all { it.matches(beatmap, algorithm) } }

            when {
                beatmaps.isEmpty() -> null
                beatmaps.size == set.beatmaps.size -> set
                else -> set.copy(beatmaps = beatmaps)
            }
        }

        val grouped = when (grouping) {
            BeatmapGrouping.BeatmapSet -> filtered
            BeatmapGrouping.Difficulty -> filtered.flatMap { set ->
                if (set.beatmaps.size == 1) listOf(set) else set.beatmaps.map { set.copy(beatmaps = listOf(it)) }
            }
        }

        return grouped.sortedWith(comparator(algorithm))
    }


    private fun comparator(algorithm: DifficultyAlgorithm): Comparator<BeatmapSetInfo> {

        fun text(selector: (BeatmapInfo) -> String) =
            compareBy<BeatmapSetInfo, String>(String.CASE_INSENSITIVE_ORDER) { selector(it.beatmaps[0]) }

        // Numeric statistics put the highest first, as the old song menu did.
        fun <T : Comparable<T>> highestFirst(selector: (BeatmapInfo) -> T) =
            compareByDescending<BeatmapSetInfo> { set -> set.beatmaps.maxOf(selector) }

        val primary = when (sortOrder) {
            BeatmapSortOrder.Title -> text { it.title }
            BeatmapSortOrder.Artist -> text { it.artist }
            BeatmapSortOrder.Creator -> text { it.creator }
            BeatmapSortOrder.DateImported -> highestFirst { it.dateImported }
            BeatmapSortOrder.Bpm -> highestFirst { it.bpmMax }
            BeatmapSortOrder.StarRating -> highestFirst { it.getStarRating(algorithm) }
            BeatmapSortOrder.Length -> highestFirst { it.length }
        }

        return primary.then(text { it.title }).thenBy { it.directory }
    }


    private sealed interface Term {
        fun matches(beatmap: BeatmapInfo, algorithm: DifficultyAlgorithm): Boolean
    }

    private class TextTerm(val text: String) : Term {
        override fun matches(beatmap: BeatmapInfo, algorithm: DifficultyAlgorithm) =
            searchableFields(beatmap).any { it.contains(text, ignoreCase = true) }
    }

    private class StatisticTerm(val key: String, val operator: String, val value: Float) : Term {

        override fun matches(beatmap: BeatmapInfo, algorithm: DifficultyAlgorithm): Boolean {
            val statistic = when (key) {
                "ar" -> beatmap.approachRate
                "od" -> beatmap.overallDifficulty
                "cs" -> beatmap.circleSize
                "hp" -> beatmap.hpDrainRate
                "star", "stars" -> beatmap.getStarRating(algorithm)
                "droidstar", "droidstars" -> beatmap.getStarRating(DifficultyAlgorithm.droid)
                "standardstar", "standardstars" -> beatmap.getStarRating(DifficultyAlgorithm.standard)
                "bpm" -> beatmap.mostCommonBPM
                "length" -> beatmap.length / 1000f
                else -> return false
            }

            return when (operator) {
                "=" -> statistic == value
                "<" -> statistic < value
                ">" -> statistic > value
                "<=" -> statistic <= value
                ">=" -> statistic >= value
                else -> false
            }
        }
    }


    private companion object {

        val STATISTIC_TERM = Regex("^(ar|od|cs|hp|stars?|droidstars?|standardstars?|bpm|length)(<=|>=|=|<|>)(\\d+(?:\\.\\d+)?)$")

        fun parseTerm(term: String): Term {
            val match = STATISTIC_TERM.matchEntire(term) ?: return TextTerm(term)
            val (key, operator, value) = match.destructured
            return StatisticTerm(key, operator, value.toFloat())
        }

        fun searchableFields(beatmap: BeatmapInfo) = sequenceOf(
            beatmap.title,
            beatmap.titleUnicode,
            beatmap.artist,
            beatmap.artistUnicode,
            beatmap.creator,
            beatmap.version,
            beatmap.tags,
            beatmap.source,
            beatmap.id?.toString().orEmpty(),
            beatmap.setId?.toString().orEmpty()
        )
    }
}
