package com.osudroid.ui.v2.test

import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.HitWindow
import com.osudroid.game.GameplayContext
import com.osudroid.game.replay.ReplaySettingsPanel
import com.osudroid.scoring.Statistic
import com.osudroid.ui.v2.hud.GameplayState
import com.reco1l.verktex.ui.Scene
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem

/**
 * A play that isn't playing, for HUD elements to be tried without one: [FakeGameplayDriver] makes it up.
 */
class FakeGameplayState : GameplayState {
    override var stat = Statistic()
    override val context = GameplayContext()
    override var elapsedTime = 0f
    override val initialStartTime = 0f
    override var firstObjectStartTime = 0f
    override var lastObjectEndTime = 0f
    override val playableBeatmap: DroidPlayableBeatmap? = null
    override var hitWindow: HitWindow? = null
    override val replaySettingsPanel: ReplaySettingsPanel? = null
    override val replayId = -1
    override val leaderboardItems: () -> List<ScoreBoardItem> = { emptyList() }
    override val isLeaderboardOnline = false
    override var isHUDEditorMode = false
    override val scene: Scene? = null

    override fun pause() = Unit
}
