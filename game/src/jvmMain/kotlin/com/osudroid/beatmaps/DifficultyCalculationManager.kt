package com.osudroid.beatmaps

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
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.LibraryManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

object DifficultyCalculationManager {


    private var job: Job? = null

    private var badge: Toasts.Toast? = null


    @JvmStatic
    fun checkForOutdatedStarRatings(): CompletableFuture<Unit> = stopCalculation().thenApply {
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
            val threadCount = ceil(Runtime.getRuntime().availableProcessors() / 2f).toInt()
            val threadPool = Executors.newFixedThreadPool(threadCount)
            val droidCalculator = DroidDifficultyCalculator()
            val standardCalculator = StandardDifficultyCalculator()

            var calculated = totalBeatmaps - pendingBeatmaps.size

            pendingBeatmaps.chunked(max(pendingBeatmaps.size / threadCount, 1))
                .forEach { chunk ->
                    ensureActive()

                    threadPool.submit {

                        // .indices to avoid creating an iterator.
                        for (i in chunk.indices) {
                            ensureActive()

                            val beatmapInfo = chunk[i]

                            try {
                                // The beatmap may already be calculated when the player selects the beatmap in song select.
                                if (beatmapInfo.needsDifficultyCalculation) {
                                    val msStartTime = System.currentTimeMillis()

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
                                            "Calculated difficulty for ${beatmapInfo.path}, took ${System.currentTimeMillis() - msStartTime}ms."
                                        )
                                    }
                                }

                                calculated++
                                mainThread {
                                    badge?.apply {
                                        isIndeterminate = false
                                        progress = calculated.toFloat() / totalBeatmaps
                                        title = "Calculating beatmap difficulties... (${calculated * 100 / totalBeatmaps}%)"
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
                }

            threadPool.shutdown()
            try {
                threadPool.awaitTermination(1, TimeUnit.HOURS)

                mainThread {
                    badge?.dismiss()
                    badge = null
                }
                SongSelect.loadBeatmaps()

            } catch (e: InterruptedException) {
                Logger.e("DifficultyCalculation", "Failed while waiting for executor termination.", e)
            }

            job = null
        }
    }


    @JvmStatic
    fun stopCalculation(): CompletableFuture<Unit> = job.stopAsync().thenApply { job = null }
}