package ru.nsu.ccfit.zuev.osu.scoring

import kotlin.jvm.JvmStatic

/**
 * Represents the scoring mode used in online beatmap leaderboard.
 */
enum class BeatmapLeaderboardScoringMode {
    SCORE,
    PP;

    companion object {
        /**
         * Parses an integer value to a [BeatmapLeaderboardScoringMode].
         *
         * @param value The integer value to parse.
         * @return The parsed [BeatmapLeaderboardScoringMode].
         */
        @JvmStatic
        fun parse(value: Int): BeatmapLeaderboardScoringMode {
            return if (value == 1) PP else SCORE
        }
    }
}
