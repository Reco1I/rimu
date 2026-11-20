package com.osudroid.game.rules

import com.osudroid.beatmaps.HitWindow
import kotlin.math.abs
import ru.nsu.ccfit.zuev.osu.scoring.ResultType

/**
 * Judges hits by their timing.
 */
object HitJudging {

    /**
     * Judges a hit [error] seconds away from its object, early or late.
     *
     * @param forcedResult The result a replay recorded for the hit, which is its result whatever the timing, null
     * for none. A miss is a miss even within the window.
     */
    fun judge(error: Float, hitWindow: HitWindow, forcedResult: ResultType? = null): JudgementResult {
        val accuracy = abs(error)

        // In seconds, as the game always compared them.
        if (accuracy > hitWindow.mehWindow / 1000 || forcedResult == ResultType.MISS) {
            return JudgementResult.Miss
        }

        return when {
            forcedResult == ResultType.HIT300 || forcedResult == null && accuracy <= hitWindow.greatWindow / 1000 -> JudgementResult.Great
            forcedResult == ResultType.HIT100 || forcedResult == null && accuracy <= hitWindow.okWindow / 1000 -> JudgementResult.Ok
            else -> JudgementResult.Meh
        }
    }
}
