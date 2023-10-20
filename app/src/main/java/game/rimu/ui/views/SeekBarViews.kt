package game.rimu.ui.views

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.widget.AppCompatSeekBar
import com.reco1l.framework.graphics.LayerDrawable
import com.reco1l.framework.graphics.clip
import com.reco1l.framework.lang.intOf
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.ViewDimensions
import game.rimu.ui.ViewSkinningRules
import android.widget.SeekBar as AndroidSeekBar


class SeekBarDimensions<T : SeekBar> : ViewDimensions<T>(MATCH_PARENT, 20)
{
    var barCornerRadius = 8f

    var thumbWidth = 12f
}


fun IWithContext.SeekBar(
    parent: ViewGroup? = this as? ViewGroup,
    init: SeekBar.() -> Unit
) = SeekBar(ctx).apply {
    parent?.addView(this)
    init()
}

open class SeekBar(override val ctx: RimuContext) :
    AndroidSeekBar(ctx),
    IWithContext,
    IScalableWithDimensions<SeekBar>,
    ISkinnableWithRules<SeekBar>
{

    override val rules by lazy { ViewSkinningRules<SeekBar>() }

    override val dimensions by lazy { SeekBarDimensions<SeekBar>() }


    private val thumbDrawable = GradientDrawable()

    private val activeBarDrawable = GradientDrawable()

    private val inactiveBarDrawable = GradientDrawable()


    init
    {
        // Bar drawable
        progressDrawable = LayerDrawable(inactiveBarDrawable, activeBarDrawable.clip()).apply {

            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }

        // Thumb
        thumb = thumbDrawable
        thumbOffset = 0
    }

    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        invalidateSkin()
        invalidateScale()
    }


    override fun onApplySkin(skin: WorkingSkin)
    {
        super.onApplySkin(skin)

        skin.data.colours.accentColor.apply {

            inactiveBarDrawable.setColor(factorInt(0.2f))
            activeBarDrawable.setColor(factorInt(0.6f))
            thumbDrawable.setColor(toInt())
        }
    }


    override fun onApplyScale(scale: Float)
    {
        super.onApplyScale(scale)

        dimensions.apply {

            inactiveBarDrawable.cornerRadius = barCornerRadius * scale
            activeBarDrawable.cornerRadius = barCornerRadius * scale
            thumbDrawable.cornerRadius = barCornerRadius * scale

            thumbDrawable.setSize(
                intOf(thumbWidth * scale),
                intOf(height * scale)
            )
        }
    }
}