package com.osudroid.ui.v2.test

import android.util.Log
import com.osudroid.beatmaps.DroidHitWindow
import com.osudroid.data.BeatmapInfo
import com.osudroid.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * A game scene that never plays, so whatever reads the game scene (the HUD, the room, the chat)
 * finds one in the UI test browser.
 *
 * Its time and statistics are set by the tests, see [FakeGameplayDriver]. Starting a game only
 * reports it through [onStartGame].
 */
class FakeGameScene : GameScene() {

    /**
     * The elapsed time of the "gameplay", in seconds.
     */
    var time = 0f

    /**
     * Called instead of starting gameplay, e.g. when a beatmap is confirmed in song select.
     */
    var onStartGame: (BeatmapInfo?) -> Unit = { Log.i("FakeGameScene", "Would play ${it?.title}") }


    init {
        resetPlay()
    }


    /**
     * Restores the statistics and the time of the scene.
     */
    fun resetPlay() {
        time = 0f
        stat = StatisticV2()
        hitWindow = DroidHitWindow(8f)
        firstObjectStartTime = 0f
        lastObjectEndTime = 60f
    }


    override fun getElapsedTime() = time

    override fun getInitialStartTime() = 0f

    override fun startGame(beatmapInfo: BeatmapInfo?, replayFile: String?, mods: ModHashMap?, isHUDEditor: Boolean) {
        onStartGame(beatmapInfo)
    }
}
