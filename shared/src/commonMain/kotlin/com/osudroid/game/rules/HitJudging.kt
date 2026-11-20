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
     * @param forcedResult The id of the [ResultType] a replay recorded for the hit, which is its result whatever
     * the timing (0 for none). A miss is a miss even within the window.
     */
    fun judge(error: Float, hitWindow: HitWindow, forcedResult: Byte = 0): JudgementResult {
        val accuracy = abs(error)

        // In seconds, as the game always compared them.
        if (accuracy > hitWindow.mehWindow / 1000 || forcedResult == ResultType.MISS.id) {
            return JudgementResult.Miss
        }

        return when {
            forcedResult == ResultType.HIT300.id || forcedResult.toInt() == 0 && accuracy <= hitWindow.greatWindow / 1000 -> JudgementResult.Great
            forcedResult == ResultType.HIT100.id || forcedResult.toInt() == 0 && accuracy <= hitWindow.okWindow / 1000 -> JudgementResult.Ok
            else -> JudgementResult.Meh
        }
    }
}
