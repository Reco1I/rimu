package com.osudroid.game.rules

import com.osudroid.beatmaps.DroidHitWindow
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.scoring.ResultType

class HitJudgingTest {

    // Great 50 ms, Ok 100 ms, Meh 200 ms.
    private val hitWindow = DroidHitWindow(10f)

    @Test
    fun `Hits are judged by the window they're in, early or late`() {
        for (sign in listOf(1f, -1f)) {
            assertEquals(JudgementResult.Great, HitJudging.judge(sign * 0f, hitWindow))
            assertEquals(JudgementResult.Great, HitJudging.judge(sign * 0.049f, hitWindow))
            assertEquals(JudgementResult.Ok, HitJudging.judge(sign * 0.051f, hitWindow))
            assertEquals(JudgementResult.Ok, HitJudging.judge(sign * 0.099f, hitWindow))
            assertEquals(JudgementResult.Meh, HitJudging.judge(sign * 0.101f, hitWindow))
            assertEquals(JudgementResult.Meh, HitJudging.judge(sign * 0.199f, hitWindow))
            assertEquals(JudgementResult.Miss, HitJudging.judge(sign * 0.201f, hitWindow))
        }
    }

    @Test
    fun `A recorded result is the result whatever the timing, within the window`() {
        assertEquals(JudgementResult.Great, HitJudging.judge(0.15f, hitWindow, ResultType.HIT300.id))
        assertEquals(JudgementResult.Ok, HitJudging.judge(0f, hitWindow, ResultType.HIT100.id))
        assertEquals(JudgementResult.Meh, HitJudging.judge(0f, hitWindow, ResultType.HIT50.id))
        assertEquals(JudgementResult.Miss, HitJudging.judge(0f, hitWindow, ResultType.MISS.id))
    }

    @Test
    fun `Hits outside the window miss even when a replay recorded a hit`() {
        assertEquals(JudgementResult.Miss, HitJudging.judge(0.25f, hitWindow, ResultType.HIT300.id))
    }

    @Test
    fun `Results convert from scores and replay results`() {
        for (result in JudgementResult.entries) {
            assertEquals(result, JudgementResult.fromScore(result.score))
            assertEquals(result, JudgementResult.fromResultType(result.resultType.id))
        }

        // Objects a replay has no result for are autoplay's.
        assertEquals(JudgementResult.Great, JudgementResult.fromResultType(0))
    }
}
