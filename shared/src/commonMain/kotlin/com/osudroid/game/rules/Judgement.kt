package com.osudroid.game.rules

import ru.nsu.ccfit.zuev.osu.scoring.ResultType

/**
 * The result of judging a hit object as a whole: a circle, the end of a slider or a spinner.
 */
enum class JudgementResult(
    /**
     * The score the result is worth before combo and mods, as the statistic registers it.
     */
    val score: Int,

    /**
     * How replays store the result.
     */
    val resultType: ResultType
) {
    Great(300, ResultType.HIT300),
    Ok(100, ResultType.HIT100),
    Meh(50, ResultType.HIT50),
    Miss(0, ResultType.MISS);

    companion object {

        /**
         * The result worth [score] (300, 100, 50, anything else being a miss), as the objects report it.
         */
        fun fromScore(score: Int) = when (score) {
            300 -> Great
            100 -> Ok
            50 -> Meh
            else -> Miss
        }

        /**
         * The result a replay stored with the id of a [ResultType]. Objects a replay has no result for count as
         * [Great], as the results of autoplay.
         */
        fun fromResultType(id: Byte) = when (id) {
            ResultType.MISS.id -> Miss
            ResultType.HIT50.id -> Meh
            ResultType.HIT100.id -> Ok
            else -> Great
        }
    }
}

/**
 * A part of a slider judged apart from it, worth score and combo of its own.
 */
enum class SliderPart {
    Head,
    Repeat,
    Tick,

    /**
     * The end of the slider, which judges the whole slider.
     */
    Tail
}
