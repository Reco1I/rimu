package com.osudroid.ui.v2

import com.edlplan.framework.easing.Easing
import com.osudroid.ui.OsuColors
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.ui.ColorVariant
import com.reco1l.andengine.ui.Theme
import com.reco1l.andengine.ui.UIBadge
import com.reco1l.framework.Color4
import com.rian.framework.RollingDoubleCounter
import kotlin.math.abs
import com.reco1l.framework.Interpolation
import ru.nsu.ccfit.zuev.osu.ResourceManager

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
                counter.rollingDuration = 100 + 80 * abs(value - prev).toFloat()
            }
        }

    init {
        // Badge color is determined by rating and should not be styled.
        style = {
            applySizeStyle()
        }
        text = "0.00"
        leadingIcon = FontAwesomeIcon(Icon.Star)
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        if (counter.isRolling) {
            counter.update(deltaTimeSec * 1000)

            text = "%.2f".format(counter.currentValue)
            background?.color = OsuColors.getStarRatingColor(counter.currentValue)

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