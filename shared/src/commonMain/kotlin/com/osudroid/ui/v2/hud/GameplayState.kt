package com.osudroid.ui.v2.hud

import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.HitWindow
import com.osudroid.game.GameplayContext
import com.osudroid.game.replay.ReplaySettingsPanel
import com.osudroid.scoring.Statistic
import com.reco1l.verktex.ui.Scene
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem

/**
 * What the HUD shows of a play, and the little it does to it.
 */
interface GameplayState {

    /**
     * The statistics of the play so far.
     */
    val stat: Statistic

    /**
     * What the gameplay of the play depends on: its mods and its timing.
     */
    val context: GameplayContext

    /**
     * The current time of the play relative to the start of its beatmap, in seconds.
     */
    val elapsedTime: Float

    /**
     * The time the play started at relative to the start of its beatmap, in seconds.
     */
    val initialStartTime: Float

    /**
     * The start time of the first object, in seconds.
     */
    val firstObjectStartTime: Float

    /**
     * The end time of the last object, in seconds.
     */
    val lastObjectEndTime: Float

    /**
     * The beatmap being played, null before it's loaded.
     */
    val playableBeatmap: DroidPlayableBeatmap?

    /**
     * The hit window of the beatmap, null before it's loaded.
     */
    val hitWindow: HitWindow?

    /**
     * The panel controlling the playback of a replay or of autoplay, null when not watching one.
     */
    val replaySettingsPanel: ReplaySettingsPanel?

    /**
     * The id of the score whose replay is being watched, -1 when not watching one.
     */
    val replayId: Int

    /**
     * The scores of the leaderboard shown during gameplay.
     */
    val leaderboardItems: () -> List<ScoreBoardItem>

    /**
     * Whether [leaderboardItems] are online scores.
     */
    val isLeaderboardOnline: Boolean

    /**
     * Whether the HUD is being edited, as it's set by the HUD.
     */
    var isHUDEditorMode: Boolean

    /**
     * The scene the play is shown in, null when it has none.
     */
    val scene: Scene?

    /**
     * Pauses the play.
     */
    fun pause()
}
