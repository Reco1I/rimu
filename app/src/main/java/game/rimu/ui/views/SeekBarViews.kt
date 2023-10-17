package game.rimu.ui.views

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


class SeekBarDimensions<T : SeekBar> : ViewDimensions<T>(MATCH_PARENT, 20)
{
    var barCornerRadius = 8f

    var thumbWidth = 12f
}



fun <T> T.SeekBar(
    attach: Boolean = true,
    init: SeekBar.() -> Unit
) where T : IWithContext,
        T : ViewGroup = SeekBar(ctx) child@{

    if (attach)
        this@SeekBar.addView(this@child)

    init()
}

open class SeekBar(override val ctx: RimuContext, init: SeekBar.() -> Unit) :
    AppCompatSeekBar(ctx),
    IWithContext,
    IScalableWithDimensions<SeekBar>,
    ISkinnableWithRules<SeekBar>
{

    override val skinningRules by lazy { ViewSkinningRules<SeekBar>() }

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

        init()
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

            inactiveBarDrawable.setColor(factorInt(0.15f))
            activeBarDrawable.setColor(factorInt(0.25f))
            thumbDrawable.setColor(factorInt(0.5f))
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