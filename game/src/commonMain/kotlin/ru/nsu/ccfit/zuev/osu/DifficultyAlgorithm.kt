package ru.nsu.ccfit.zuev.osu

import com.osudroid.GameMode
import kotlin.jvm.JvmStatic

/**
 * Represents the algorithm used to calculate the difficulty of a beatmap.
 */
enum class DifficultyAlgorithm {
    /**
     * osu!droid algorithm.
     */
    droid,

    /**
     * osu!standard algorithm.
     */
    standard;

    /**
     * Converts this [DifficultyAlgorithm] to its [GameMode] equivalent.
     */
    fun toGameMode(): GameMode {
        return when (this) {
            droid -> GameMode.Droid
            standard -> GameMode.Standard
        }
    }

    companion object {
        /**
         * Parses an integer value to a [DifficultyAlgorithm].
         *
         * @param value The integer value to parse.
         * @return The parsed [DifficultyAlgorithm].
         */
        @JvmStatic
        fun parse(value: Int): DifficultyAlgorithm {
            return if (value == 1) standard else droid
        }
    }
}
