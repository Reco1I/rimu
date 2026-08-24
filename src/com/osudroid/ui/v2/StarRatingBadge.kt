package com.osudroid.ui.v2

import com.edlplan.framework.easing.Easing
import com.osudroid.ui.OsuColors
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.data.Color4
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
        // Badge color is determined by rating and should not be styled.
        style = {
            applySizeStyle()
        }
        text = "0.00"
        leadingIcon = UIIcon(FAIcon.Star)
        registerUpdateHandler(counter)
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (counter.isRolling) {
            text = "%.2f".format(counter.currentValue)
            backgroundColor = OsuColors.getStarRatingColor(counter.currentValue)

            if (counter.currentValue >= 6.5) {
                color = OsuColors.getStarRatingTextColor(counter.currentValue)
                alpha = 1f
            } else {
                color = Color4.Black
                alpha = 0.75f
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}