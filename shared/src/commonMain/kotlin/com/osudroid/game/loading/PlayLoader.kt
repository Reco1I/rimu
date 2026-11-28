package com.osudroid.game.loading

import com.osudroid.GameMode
import com.osudroid.OsuGameHost
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.difficulty.BeatmapDifficultyCalculator
import com.osudroid.game.SliderPath
import com.osudroid.game.convertSliderPath
import com.osudroid.game.createSliderRenderPath
import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.math.Path
import com.reco1l.verktex.ui.shape.PathShape
import com.reco1l.verktex.ui.toast.Toasts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okio.FileNotFoundException
import okio.IOException
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import com.osudroid.ui.skinning.OsuSkin

/**
 * Loads the beatmap of a [PlayRequest], reusing what it can of the play loaded before it.
 */
object PlayLoader {

    /**
     * Loads [request], from [previous] when it's the same beatmap (and the same playable beatmap when its mods are the
     * same). A failure is shown to the player.
     *
     * @param onTimedDifficultyJob Given the job calculating the difficulty of the beatmap at each object, when it
     * starts one, to cancel it with the loading.
     * @return The loaded play, null when it can't be played.
     */
    fun load(
        request: PlayRequest,
        previous: LoadedPlay?,
        scope: CoroutineScope?,
        onTimedDifficultyJob: (Job) -> Unit
    ): LoadedPlay? {
        if (!OsuGameHost.current.verifyFileIntegrity()) {
            Toasts.showLong(Res.string.file_integrity_tampered)
            return null
        }

        scope?.ensureActive()

        val beatmapInfo = request.beatmap
        val mods = request.mods
        // The play loaded before, when it's of the same beatmap.
        val sameBeatmap = previous?.takeIf { it.parsedBeatmap.md5 == beatmapInfo.md5 }

        val parsedBeatmap: Beatmap = sameBeatmap?.parsedBeatmap ?: try {
            BeatmapCache.getBeatmap(beatmapInfo, true, GameMode.Droid, scope)
        } catch (e: IOException) {
            Logger.e(TAG, "Failed to load the beatmap: " + e.message)
            Toasts.showLong(e.message.toString())
            return null
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG, "Failed to load the beatmap: " + e.message)
            Toasts.showLong(e.message.toString())
            return null
        }

        if (parsedBeatmap.md5 != beatmapInfo.md5) {
            Toasts.showLong(Res.string.file_integrity_tampered)
            return null
        }

        if (parsedBeatmap.hitObjects.objects.isEmpty()) {
            Toasts.showLong("Empty Beatmap")
            return null
        }

        // Ensure that only relevant mods are applied.
        mods.values.removeAll { m -> !m.isRelevant }

        val audioPath = try {
            val musicFile = beatmapInfo.audioPath.toPath()

            if (!Platform.fileSystem.exists(musicFile)) {
                throw FileNotFoundException(musicFile.toString())
            }

            musicFile.toString()
        } catch (e: Exception) {
            Logger.e(TAG, "Load Music: " + e.message)
            Toasts.showLong(e.message.toString())
            return null
        }

        val play = if (sameBeatmap != null && sameBeatmap.mods == mods) {
            sameBeatmap
        } else {
            val playableBeatmap = parsedBeatmap.createDroidPlayableBeatmap(mods.values)
            val (sliderPaths, sliderRenderPaths) = calculateSliderPaths(playableBeatmap, scope)

            LoadedPlay(
                beatmapInfo = beatmapInfo,
                parsedBeatmap = parsedBeatmap,
                playableBeatmap = playableBeatmap,
                mods = mods,
                audioPath = audioPath,
                objects = playableBeatmap.hitObjects.objects.toTypedArray(),
                breakPeriods = playableBeatmap.events.breaks.toTypedArray(),
                timingControlPoints = playableBeatmap.controlPoints.timing.controlPoints.toTypedArray(),
                effectControlPoints = playableBeatmap.controlPoints.effect.controlPoints.toTypedArray(),
                sliderPaths = sliderPaths,
                sliderRenderPaths = sliderRenderPaths,
                timedDifficulty = TimedDifficulty()
            )
        }

        if (request.isHUDEditor || OsuSkin.current.hud.hasElement(HUDElements.pp_counter.name)) {
            calculateTimedDifficulty(play)?.let(onTimedDifficultyJob)
        }

        return play
    }

    // Calculates the difficulty of the play at each object for the algorithm the pp counter shows, unless it was.
    private fun calculateTimedDifficulty(play: LoadedPlay): Job? {
        val timedDifficulty = play.timedDifficulty

        return when (OsuSettings.difficultyAlgorithm) {
            DifficultyAlgorithm.droid -> if (timedDifficulty.droid != null) null else async {
                timedDifficulty.droid = BeatmapDifficultyCalculator.calculateDroidTimedDifficulty(
                    play.parsedBeatmap, play.playableBeatmap, this
                )

                updateThread { timedDifficulty.onCalculated?.invoke() }
            }

            DifficultyAlgorithm.standard -> if (timedDifficulty.standard != null) null else async {
                timedDifficulty.standard = BeatmapDifficultyCalculator.calculateStandardTimedDifficulty(
                    play.parsedBeatmap, play.mods.values, this
                )

                updateThread { timedDifficulty.onCalculated?.invoke() }
            }
        }
    }

    private fun calculateSliderPaths(playableBeatmap: DroidPlayableBeatmap, scope: CoroutineScope?): Pair<Array<SliderPath>, Array<Path>> {
        val sliders = playableBeatmap.hitObjects.objects.filterIsInstance<Slider>()

        val sliderPaths = Array(sliders.size) {
            scope?.ensureActive()
            convertSliderPath(sliders[it], scope)
        }

        val sliderRenderPaths = Array(sliders.size) {
            scope?.ensureActive()
            // Simplified for its mesh now, not on the frame the slider spawns.
            createSliderRenderPath(sliders[it], scope).also(PathShape::prepare)
        }

        return sliderPaths to sliderRenderPaths
    }

    private const val TAG = "PlayLoader"
}
