package com.osudroid.game.session

import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.toBoardItem
import com.osudroid.scoring.Statistic
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.async
import com.osudroid.utils.runSafe
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.toast.Toasts
import kotlin.math.max
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem

/**
 * What an attempt tells the multiplayer room, and asks of it: its live score, skipping, leaving and its final score.
 * Nothing is sent outside multiplayer.
 */
class MultiplayerSync {

    // The real time when the player last pressed back, in milliseconds since the start, -1 before.
    private var lastBackPressTime = -1f

    private var isSkipRequested = false

    // The real time elapsed since the attempt started, in milliseconds.
    private var realTimeElapsed = 0L

    // The real time elapsed since the live score was last sent, in milliseconds.
    private var statisticDataTimeElapsed = 0L

    // The live score sent last, to send it again only once it changed.
    private var lastScoreSent: ScoreBoardItem? = null


    /**
     * Sends the live score every 3 seconds, when it changed.
     *
     * @param deltaTime The gameplay time of the frame, in seconds.
     * @param speedMultiplier The rate gameplay goes at, to tell the real time of the frame.
     */
    fun update(deltaTime: Float, speedMultiplier: Float, stat: Statistic) {
        if (!Multiplayer.isMultiplayer) {
            return
        }

        val mSecElapsed = (deltaTime / speedMultiplier * 1000).toLong()
        realTimeElapsed += mSecElapsed
        statisticDataTimeElapsed += mSecElapsed

        // Sending statistics data every 3000ms if data was changed
        if (statisticDataTimeElapsed > 3000) {
            statisticDataTimeElapsed %= 3000

            if (Multiplayer.isConnected) {
                val liveScore = stat.toBoardItem()

                if (liveScore != lastScoreSent) {
                    lastScoreSent = liveScore
                    async { runSafe { RoomAPI.submitLiveScore(liveScore.toJson()) } }
                }
            }
        }
    }

    /**
     * Called as the player presses back: the second press, once enough time passed, leaves the match.
     *
     * @return Whether the player leaves the match.
     */
    fun onBackPress(): Boolean {
        // Setting a delay of 300ms minimum for the player to tap back button again.
        if (lastBackPressTime > 0 && realTimeElapsed - lastBackPressTime > max(300f, Settings[OsuSettings.BackButtonPressTime] * 1.5f)) {
            return true
        }

        lastBackPressTime = realTimeElapsed.toFloat()
        Toasts.show("Tap twice to exit to room.")
        return false
    }

    /**
     * Asks the room to skip the intro, once: [onRequest] is called then, before it's asked.
     */
    fun requestSkip(onRequest: () -> Unit) {
        if (isSkipRequested) {
            return
        }

        isSkipRequested = true
        onRequest()

        async { RoomAPI.requestSkip() }
        Toasts.show("Skip requested")
    }

    /**
     * Sends the final score of the attempt, when connected.
     */
    fun submitFinalScore(stat: Statistic) {
        if (Multiplayer.isConnected) {
            async { runSafe { RoomAPI.submitFinalScore(stat.toJson()) } }
        }
    }

    /**
     * Shows the chat of the room, when connected.
     */
    fun showChat() {
        if (Multiplayer.isConnected) {
            Multiplayer.roomScene!!.chat.show()
        }
    }

    /**
     * Hides the chat of the room, when connected.
     */
    fun hideChat() {
        if (Multiplayer.isConnected) {
            Multiplayer.roomScene!!.chat.hide()
        }
    }
}
