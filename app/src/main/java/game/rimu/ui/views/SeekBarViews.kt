package game.rimu.ui.views

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules


class SeekBarDimensions<T : SeekBar> : ViewDimensions<T>(MATCH_PARENT, WRAP_CONTENT)
{

    var barHeight = 14


    override fun onApplyScale(target: T, scale: Float)
    {
        super.onApplyScale(target, scale)

        target.trackHeight = (barHeight * scale).toInt()
    }
}


// Base

fun IWithContext.SeekBar(
    parent: ViewGroup? = this as? ViewGroup,
    init: SeekBar.() -> Unit
) = SeekBar(ctx).apply { parent?.addView(this); init() }

open class SeekBar(override val ctx: RimuContext) :
    Slider(ctx.activity),
    IWithContext,
    IScalableWithDimensions<SeekBar>,
    ISkinnableWithRules<SeekBar>
{

    override val rules by lazy { ViewSkinningRules<SeekBar>() }

    override val dimensions by lazy { SeekBarDimensions<SeekBar>() }


    init
    {
        labelBehavior = LabelFormatter.LABEL_FLOATING
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
            trackActiveTintList = toColorStateList(factor = 0.7f)
            trackInactiveTintList = toColorStateList(factor = 0.2f)
            haloTintList = toColorStateList(alpha = 0.1f)
            thumbTintList = toColorStateList()
        }
    }
}