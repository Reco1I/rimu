package com.osudroid.ui.v2.results

import com.osudroid.GameMode
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.difficulty.BeatmapDifficultyCalculator
import com.osudroid.difficulty.attributes.DifficultyAttributes
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes
import com.osudroid.difficulty.attributes.PerformanceAttributes
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModOldNightCore
import com.osudroid.scoring.Statistic
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.Logger
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.scoring.Replay

/**
 * The difficulty and performance of a score, as the results show them.
 */
class ResultsPerformance(
    val starRating: Double,
    val pp: Double,
    val maxPp: Double,
    /**
     * Whether they're osu!droid's (dpp) rather than osu!standard's (pp).
     */
    val isDroid: Boolean,
)

/**
 * Calculates the difficulty and performance of [stat] on [beatmapInfo] with the algorithm of the
 * settings, from the replay at [replayPath] when it's a replay file opened from the results of
 * [beatmapToReplay], whose missing slider statistics it fills in [stat]. Null when the beatmap can't
 * be parsed.
 *
 * Slow: called away from the update thread.
 */
fun calculateResultsPerformance(
    beatmapInfo: BeatmapInfo,
    stat: Statistic,
    replayPath: String?,
    beatmapToReplay: BeatmapInfo?,
    mapMD5: String?,
): ResultsPerformance? {
    val algorithm = OsuSettings.difficultyAlgorithm
    val mods = stat.mod

    val beatmapData: Beatmap = try {
        BeatmapCache.getBeatmap(
            beatmapInfo,
            true,
            when (algorithm) {
                DifficultyAlgorithm.droid -> GameMode.Droid
                DifficultyAlgorithm.standard -> GameMode.Standard
            }
        )
    } catch (e: Exception) {
        Logger.e("ResultsPerformance", "Failed to parse beatmap for difficulty calculation", e)
        null
    } ?: return null

    var difficultyAttributes: DifficultyAttributes? = null
    var performanceAttributes: PerformanceAttributes? = null

    // Don't try to load online replay
    if (replayPath != null && beatmapToReplay != null && !replayPath.startsWith("https://")) {
        val replay = Replay()
        replay.setObjectCount(beatmapToReplay.totalHitObjectCount)
        replay.setBeatmap(beatmapToReplay.fullBeatmapsetName, beatmapToReplay.fullBeatmapName, mapMD5 ?: beatmapToReplay.md5)

        if (replay.load(replayPath, true)) {
            // If statistics information is not complete, we populate it with information from replay.
            if (
                stat.sliderHeadHits == -1 ||
                stat.sliderTickHits == -1 ||
                stat.sliderRepeatHits == -1 ||
                stat.sliderEndHits == -1
            ) {
                replay.populateStatistics(beatmapData, stat)
            }

            var copiedMods = mods

            if (ModNightCore::class in mods && replay.replayVersion <= 3) {
                copiedMods = mods.deepCopy()
                copiedMods.removeOfType(ModNightCore::class)
                copiedMods.put(ModOldNightCore::class)
            }

            when (algorithm) {
                DifficultyAlgorithm.droid -> {
                    val playableBeatmap = beatmapData.createDroidPlayableBeatmap(copiedMods.values)

                    val attributes = BeatmapDifficultyCalculator.calculateDroidDifficultyForReplay(beatmapData, playableBeatmap)
                    difficultyAttributes = attributes
                    performanceAttributes = BeatmapDifficultyCalculator.calculateDroidPerformance(
                        playableBeatmap, attributes, replay, stat
                    )
                }

                DifficultyAlgorithm.standard -> {
                    val playableBeatmap = beatmapData.createStandardPlayableBeatmap(copiedMods.values)

                    val attributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, playableBeatmap)
                    difficultyAttributes = attributes

                    @Suppress("UNCHECKED_CAST")
                    performanceAttributes = BeatmapDifficultyCalculator.calculateStandardPerformance(
                        playableBeatmap, attributes,
                        replay.objectData as Array<Replay.ReplayObjectData>, stat
                    )
                }
            }
        }
    }

    if (difficultyAttributes == null) {
        difficultyAttributes = when (algorithm) {
            DifficultyAlgorithm.droid -> BeatmapDifficultyCalculator.calculateDroidDifficulty(beatmapData, mods.values)
            DifficultyAlgorithm.standard -> BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, mods.values)
        }

        performanceAttributes = when (algorithm) {
            DifficultyAlgorithm.droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                beatmapData, difficultyAttributes as DroidDifficultyAttributes, stat
            )

            DifficultyAlgorithm.standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                beatmapData, difficultyAttributes as StandardDifficultyAttributes, stat
            )
        }
    }

    val maxPerformanceAttributes = when (algorithm) {
        DifficultyAlgorithm.droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
            difficultyAttributes as DroidDifficultyAttributes
        )

        DifficultyAlgorithm.standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
            difficultyAttributes as StandardDifficultyAttributes
        )
    }

    return ResultsPerformance(
        starRating = difficultyAttributes.starRating,
        pp = performanceAttributes!!.total,
        maxPp = maxPerformanceAttributes.total,
        isDroid = algorithm == DifficultyAlgorithm.droid,
    )
}
