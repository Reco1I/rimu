package com.osudroid.ui.v2.results

import com.osudroid.OsuGame
import com.osudroid.audio.MusicService
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.data.toScoreInfo
import com.osudroid.game.Gameplay
import com.osudroid.mods.ModAutoplay
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.isTeamStatistic
import com.osudroid.resources.Res
import com.osudroid.scoring.Statistic
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.multi.LobbyScene
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.toast.Toasts
import okio.Path.Companion.toPath
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring

/**
 * The results of a [gameplay]: what [ResultsScene] shows of a score, and saving and submitting it.
 */
class Results(private val gameplay: Gameplay) {

    /**
     * The scene of the results last loaded.
     */
    var scene: ResultsScene? = null
        private set

    /**
     * The statistic of the replay being watched, which the results show once it ends.
     */
    var replayStat: Statistic? = null

    var replayID = -1

    /**
     * The beatmap of the results.
     */
    var beatmapInfo: BeatmapInfo? = null

    /**
     * The statistic the results show, in multiplayer the one selected in the match's scores.
     */
    var currentStatistic: Statistic? = null

    private var music: MusicService? = null

    // What the watch replay button plays.
    private var replayPath: String? = null


    /**
     * Shows [stat] in a new scene, in place of the results' scene if it's shown.
     *
     * [beatmap] is the beatmap just played, null for the results of a replay played from [replayPath]
     * on [beatmapToReplay], and [mapMD5] the hash of the beatmap played: the score is saved and
     * submitted when it's [beatmap]'s.
     */
    fun load(
        stat: Statistic,
        beatmap: BeatmapInfo?,
        player: MusicService?,
        replayPath: String?,
        mapMD5: String?,
        beatmapToReplay: BeatmapInfo?,
    ) {
        val previousScene = scene
        val wasShown = previousScene != null && Verktex.scene === previousScene

        music = player
        currentStatistic = stat
        this.replayPath = replayPath

        if (replayPath != null && beatmap == null) {
            replayStat = stat
        }

        val beatmapInfo = beatmapToReplay ?: beatmap ?: this.beatmapInfo!!
        this.beatmapInfo = beatmapInfo

        val isFreshPlay = beatmap != null && beatmap.md5 == mapMD5

        val action = when {
            Multiplayer.isMultiplayer -> ResultsScene.Action.None
            beatmap != null -> ResultsScene.Action.Retry
            replayPath != null -> ResultsScene.Action.WatchReplay
            else -> ResultsScene.Action.None
        }

        val scene = ResultsScene(
            results = this,
            stat = stat,
            beatmapInfo = beatmapInfo,
            action = action,
            isFreshPlay = isFreshPlay,
            showStatistics = Settings[OsuSettings.DisplayScoreStatistics] && !stat.isTeamStatistic(),
            replayPath = replayPath,
            mapMD5 = mapMD5,
            beatmapToReplay = beatmapToReplay,
        )
        this.scene = scene

        // In case the scene was reloaded
        if (Multiplayer.isMultiplayer) {
            showLeaderboard(scene)
        }

        if (wasShown) {
            Verktex.withTreeLock {
                Verktex.popScene()
                Verktex.pushScene(scene)
            }
        }

        if (isFreshPlay) {
            SkinSounds["applause"].play()
            saveAndSubmit(stat, beatmap!!, replayPath, scene)
        }
    }

    /**
     * Saves the score just played to the database and submits it to the server, when it should be.
     */
    private fun saveAndSubmit(stat: Statistic, beatmap: BeatmapInfo, replayPath: String?, scene: ResultsScene) {
        val mods = stat.mod

        if (stat.totalScoreWithMultiplier <= 0 || ModAutoplay::class in mods ||
            (Multiplayer.isMultiplayer &&
                ((Multiplayer.room != null && Multiplayer.room!!.isTeamVersus) || gameplay.hasFailed))) {
            return
        }

        val totalNotes = stat.hit300 + stat.hit100 + stat.hit50 + stat.misses

        // Do not save and submit score if note count does not match, since it indicates a corrupted score
        // (potentially from bugging the gameplay by any unnecessary means).
        if (totalNotes != beatmap.totalHitObjectCount) {
            Toasts.showLong(Res.string.replay_corrupted)
            return
        }

        stat.replayFilename = replayPath?.toPath()?.name.orEmpty()
        stat.beatmapMD5 = beatmap.md5

        val scoreInfo = stat.toScoreInfo()

        async {
            try {
                DatabaseManager.scoreInfoTable.insertScore(scoreInfo)
            } catch (e: Exception) {
                Logger.e("Results", "Failed to save score to database", e)
            }
        }

        // Upload score
        if (!OnlineManager.getInstance().isStayOnline ||
            (Multiplayer.isMultiplayer && !Settings[OsuSettings.SubmitScoreOnMultiplayer])) {
            return
        }

        if (mods.values.any { !it.isRanked } || Multiplayer.isRemoveSliderLock()) {
            return
        }

        val online = OnlineManager.getInstance()
        val card = ScoreSubmissionCard(online.rank, online.score, online.accuracy, online.pp)
        scene.showSubmission(card)

        OnlineScoring.getInstance().sendRecord(beatmap, stat, card, replayPath)
    }

    /**
     * Shows the scores of the multiplayer match once they arrive, see [Multiplayer.finalData].
     */
    fun updateLeaderboard() {
        updateThread { scene?.let(::showLeaderboard) }
    }

    private fun showLeaderboard(scene: ResultsScene) {
        val stats = Multiplayer.finalData ?: return

        scene.showLeaderboard(ResultsLeaderboard(stats, currentStatistic) { stat ->
            load(stat, beatmapInfo, OsuGame.music, null, null, null)
        })
    }

    /**
     * Plays the beatmap again.
     */
    fun retry() {
        SkinSounds["applause"].stop()
        stopMusic()
        scene = null
        leaveAndRun { gameplay.retry() }
    }

    /**
     * Watches the replay of the results.
     */
    fun watchReplay() {
        val beatmap = beatmapInfo ?: return
        val path = replayPath ?: return
        val mods = currentStatistic?.mod ?: return

        SkinSounds["applause"].stop()
        stopMusic()
        scene = null
        leaveAndRun { gameplay.watchReplay(beatmap, path, mods) }
    }

    fun back() {
        SkinSounds["applause"].stop()
        Multiplayer.finalData = null
        currentStatistic = null

        if (Multiplayer.isMultiplayer) {
            // Preventing NPEs when player gets disconnected while playing
            if (!Multiplayer.isConnected) {
                leaveAndRun { Verktex.pushScene(LobbyScene()) }
            } else {
                leaveAndRun {
                    if (Verktex.scene !== Multiplayer.roomScene) {
                        Multiplayer.roomScene!!.show()
                    }
                }
            }
            return
        }

        replayMusic()
        leaveAndRun {}
        replayID = -1
    }

    fun stopMusic() {
        music?.stop()
    }

    private fun replayMusic() {
        val music = music ?: return
        music.stop()
        music.preLoad(beatmapInfo!!.audioPath)
        music.play()
    }

    private fun leaveAndRun(action: () -> Unit) {
        Verktex.withTreeLock {
            Verktex.popScene()
        }

        action()
    }
}
