package com.osudroid.game.rules

import com.osudroid.scoring.Statistic


/**
 * Applies judgements to the [statistic] of a play: its score, combo, counts and health.
 *
 * It's the one place the scoring rules live, for the play as it's played and as a seek reconstructs it. It has
 * no effects of its own: sounds, the HUD and failing are up to its caller.
 */
class ScoreProcessor(val statistic: Statistic) {

    /**
     * Whether an object of the current combo was missed, which gives the end of the combo no katu or geki.
     */
    var comboWasMissed = false
        private set

    /**
     * Whether an object of the current combo was judged worse than a 300, which gives its end no geki.
     */
    var comboWas100 = false
        private set

    /**
     * Applies the [result] of an object judged as a whole: a circle, the end of a slider or a spinner.
     *
     * @param endCombo Whether the object is the last of its combo.
     * @param incrementCombo Whether the object adds to the combo: the end of a slider doesn't when it wasn't tracked.
     * @return The texture of the judgement the object shows: `hit0`, `hit50`, `hit100`, `hit100k`, `hit300`,
     * `hit300k` or `hit300g`.
     */
    fun applyObject(result: JudgementResult, endCombo: Boolean, incrementCombo: Boolean = true): String {
        val name: String

        when (result) {
            JudgementResult.Miss -> {
                comboWasMissed = true
                statistic.registerHit(0, false, false, incrementCombo)

                // A miss keeps the flags of its combo even when it ends it, as it always has.
                return "hit0"
            }

            JudgementResult.Meh -> {
                statistic.registerHit(50, false, false, incrementCombo)
                comboWas100 = true
                name = "hit50"
            }

            JudgementResult.Ok -> {
                comboWas100 = true

                if (endCombo && !comboWasMissed) {
                    statistic.registerHit(100, true, false, incrementCombo)
                    name = "hit100k"
                } else {
                    statistic.registerHit(100, false, false, incrementCombo)
                    name = "hit100"
                }
            }

            JudgementResult.Great -> {
                if (endCombo && !comboWasMissed) {
                    if (!comboWas100) {
                        statistic.registerHit(300, true, true, incrementCombo)
                        name = "hit300g"
                    } else {
                        statistic.registerHit(300, true, false, incrementCombo)
                        name = "hit300k"
                    }
                } else {
                    statistic.registerHit(300, false, false, incrementCombo)
                    name = "hit300"
                }
            }
        }

        if (endCombo) {
            resetCombo()
        }

        return name
    }

    /**
     * Applies a hit head, repeat or tick of a slider. Its [SliderPart.Tail] judges the whole slider, through
     * [applySliderTail].
     *
     * @return The texture of the judgement the part shows: `sliderpoint30` or `sliderpoint10`.
     */
    fun applySliderPart(part: SliderPart): String {
        return when (part) {
            SliderPart.Head -> {
                statistic.registerHit(30, false, false)
                statistic.addSliderHeadHit()
                "sliderpoint30"
            }

            SliderPart.Repeat -> {
                statistic.registerHit(30, false, false)
                statistic.addSliderRepeatHit()
                "sliderpoint30"
            }

            SliderPart.Tick -> {
                statistic.registerHit(10, false, false)
                statistic.addSliderTickHit()
                "sliderpoint10"
            }

            SliderPart.Tail -> throw IllegalArgumentException("The tail of a slider judges the whole slider, see applySliderTail")
        }
    }

    /**
     * Applies the end of a slider, which judges the whole slider with [result].
     *
     * @param tracked Whether the end was tracked: it adds to the combo and counts as hit only then.
     */
    fun applySliderTail(result: JudgementResult, endCombo: Boolean, tracked: Boolean): String {
        if (tracked) {
            statistic.addSliderEndHit()
        }

        return applyObject(result, endCombo, tracked)
    }

    /**
     * Applies a missed head, repeat, tick or end of a slider, which only breaks the combo.
     */
    fun applySliderPartMiss() {
        statistic.registerHit(0, true, false)
    }

    /**
     * Applies a rotation of a spinner before it's cleared.
     */
    fun applySpinnerRotation() {
        statistic.registerSpinnerHit()
    }

    /**
     * Applies a bonus rotation of a cleared spinner.
     */
    fun applySpinnerBonus() {
        statistic.registerHit(1000, false, false)
    }

    /**
     * Starts a new combo, as after a seek.
     */
    fun resetCombo() {
        comboWasMissed = false
        comboWas100 = false
    }
}
