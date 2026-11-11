package com.osudroid.beatmaps

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.Platform
import com.reco1l.verktex.settings.SettingKey
import com.reco1l.verktex.settings.Settings
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.ui.toast.Toasts
import com.osudroid.ui.v2.songselect.SongSelect

import com.reco1l.verktex.Logger
import com.osudroid.utils.async
import com.osudroid.data.DatabaseManager
import com.osudroid.utils.mainThread
import com.osudroid.utils.stopAsync
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.difficulty.calculator.DroidDifficultyCalculator
import com.osudroid.difficulty.calculator.StandardDifficultyCalculator
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.jvm.JvmStatic
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import ru.nsu.ccfit.zuev.osu.LibraryManager
import kotlin.math.ceil
import kotlin.math.max

object DifficultyCalculationManager {


    private var job: Job? = null

    private var badge: Toasts.Toast? = null


    /**
     * Resets the star ratings calculated by older versions of the calculators, once the running
     * calculation stops. If there's none, they're reset before this returns.
     */
    @JvmStatic
    fun checkForOutdatedStarRatings() {
        val stopping = stopCalculation()

        if (stopping.isCompleted) {
            resetOutdatedStarRatings()
        } else {
            async {
                stopping.await()
                resetOutdatedStarRatings()
            }
        }
    }

    private fun resetOutdatedStarRatings() {
        // Before per-star rating versioning, the version was stored in this key.
        // If both star ratings have been updated after this, this key is no longer needed and can be removed.
        val oldStarRatingVersion = Settings[OsuSettings.LegacyStarRatingVersion]

        fun storedVersion(key: SettingKey<Long>) = if (Settings.contains(key)) Settings[key] else oldStarRatingVersion

        if (storedVersion(OsuSettings.DroidStarRatingVersion) < DroidDifficultyCalculator.VERSION) {
            DatabaseManager.beatmapInfoTable.resetDroidStarRatings()
            Settings[OsuSettings.DroidStarRatingVersion] = DroidDifficultyCalculator.VERSION
        }

        if (storedVersion(OsuSettings.StandardStarRatingVersion) < StandardDifficultyCalculator.VERSION) {
            DatabaseManager.beatmapInfoTable.resetStandardStarRatings()
            Settings[OsuSettings.StandardStarRatingVersion] = StandardDifficultyCalculator.VERSION
        }
    }


    @JvmStatic
    fun calculateDifficulties() {
        if (job?.isActive == true) {
            return
        }

        val totalBeatmaps = LibraryManager.getLibrary().sumOf { it.beatmaps.size }
        val pendingBeatmaps = LibraryManager.getLibrary().flatMap { set -> set.beatmaps.filter { it.needsDifficultyCalculation } }

        if (pendingBeatmaps.isEmpty()) {
            return
        }

        badge = Toasts.showPersistent(
            title = "Calculating beatmap difficulties...",
            text = "During this process, the game may suffer performance degradation.",
            isIndeterminate = true
        )

        job = async {
            val threadCount = ceil(Platform.processorCount / 2f).toInt()
            val dispatcher = Dispatchers.Default.limitedParallelism(threadCount)
            val droidCalculator = DroidDifficultyCalculator()
            val standardCalculator = StandardDifficultyCalculator()

            val calculated = AtomicInt(totalBeatmaps - pendingBeatmaps.size)

            // Each chunk is a child of this job, so stopping the job stops them.
            pendingBeatmaps.chunked(max(pendingBeatmaps.size / threadCount, 1)).map { chunk ->
                ensureActive()

                launch(dispatcher) {

                    // .indices to avoid creating an iterator.
                    for (i in chunk.indices) {
                        ensureActive()

                        val beatmapInfo = chunk[i]

                        try {
                            // The beatmap may already be calculated when the player selects the beatmap in song select.
                            if (beatmapInfo.needsDifficultyCalculation) {
                                val startTime = TimeSource.Monotonic.markNow()

                                val beatmap = BeatmapParser(beatmapInfo.path, this, beatmapInfo.md5).parse(true)

                                beatmapInfo.apply(beatmap, this)

                                if (beatmapInfo.droidStarRating == null) {
                                    val attributes = droidCalculator.calculate(beatmap, scope = this)
                                    beatmapInfo.droidStarRating = attributes.starRating.toFloat()
                                }

                                if (beatmapInfo.standardStarRating == null) {
                                    val attributes = standardCalculator.calculate(beatmap, scope = this)
                                    beatmapInfo.standardStarRating = attributes.starRating.toFloat()
                                }

                                DatabaseManager.beatmapInfoTable.update(beatmapInfo)

                                if (Platform.application.isDebug) {
                                    Logger.i(
                                        "DifficultyCalculation",
                                        "Calculated difficulty for ${beatmapInfo.path}, took ${startTime.elapsedNow().inWholeMilliseconds}ms."
                                    )
                                }
                            }

                            val done = calculated.incrementAndFetch()
                            mainThread {
                                badge?.apply {
                                    isIndeterminate = false
                                    progress = done.toFloat() / totalBeatmaps
                                    title = "Calculating beatmap difficulties... (${done * 100 / totalBeatmaps}%)"
                                }
                            }

                        } catch (e: Exception) {
                            if (e is CancellationException) {
                                throw e
                            }

                            Logger.e(
                                "DifficultyCalculation",
                                "Error while calculating difficulty.",
                                e
                            )
                        }

                    }
                }
            }.joinAll()

            mainThread {
                badge?.dismiss()
                badge = null
            }
            // Song select shows the new star ratings, the next one loads them anyway.
            (Verktex.scene as? SongSelect)?.loadBeatmaps()

            job = null
        }
    }


    /**
     * Stops the running calculation. The returned [Deferred] completes once it has stopped, at once
     * if there's none.
     */
    @JvmStatic
    fun stopCalculation(): Deferred<Unit> = job.stopAsync().also { stopping ->
        stopping.invokeOnCompletion { job = null }
    }
}
