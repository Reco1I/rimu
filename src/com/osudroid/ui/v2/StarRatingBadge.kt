package com.osudroid.ui.v2

import com.osudroid.ui.OsuColors
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.fonts.Fonts
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.text.faSolidIcon
import com.rian.framework.RollingDoubleCounter
import kotlin.math.abs


/**
 * A [UIBadge] for displaying star ratings. Automatically adjusts its styling according to the rating.
 */
class StarRatingBadge : UIBadge() {

    private val counter = RollingDoubleCounter(0.0).apply {
        rollingEasing = Easing.OutQuint
    }

    /**
     * The star rating value displayed by this [StarRatingBadge].
     *
     * Visuals may not reflect this value due to rolling animation.
     */
    var rating
        get() = counter.targetValue
        set(value) {
            val prev = counter.targetValue

            if (prev != value) {
                counter.targetValue = value
                counter.rollingDuration = 0.1f + 0.08f * abs(value - prev).toFloat()
            }
        }

    init {
        // Sized like any badge, with a bolder font. The colors are determined by the rating, so
        // they're applied again over the ones of the badge style.
        style += Style { theme ->
            fontFamily = Fonts[theme.typography.h3.family]
            fontWeight = theme.typography.h3.weight
            applyRating(counter.currentValue)
        }
        text = "0.00"
        leadingIcon = faSolidIcon(FAIcon.Star)
    }


    private fun applyRating(value: Double) {
        text = "%.2f".format(value)
        backgroundColor = OsuColors.getStarRatingColor(value)

        if (value >= 6.5) {
            color = OsuColors.getStarRatingTextColor(value)
            alpha = 1f
        } else {
            color = Color4.Black
            alpha = 0.75f
        }
    }

    /**
     * Skips the rolling animation, jumping to the colors and text of the current rating.
     */
    fun applyColorsInstantly() {
        counter.setValueWithoutRolling(counter.targetValue)
        applyRating(counter.currentValue)
    }

    override fun onUpdate(clock: FrameClock) {
        counter.onUpdate(clock.elapsedFrameTime)

        if (counter.isRolling) {
            applyRating(counter.currentValue)
        }

        super.onUpdate(clock)
    }

}
