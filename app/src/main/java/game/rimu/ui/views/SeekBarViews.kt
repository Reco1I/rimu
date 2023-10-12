package game.rimu.ui.views

import android.graphics.drawable.ShapeDrawable
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatSeekBar
import com.reco1l.framework.graphics.LayerDrawable
import com.reco1l.framework.graphics.clip
import com.reco1l.framework.graphics.setRadius
import com.reco1l.framework.graphics.setSize
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.IScalableWithDimensions
import game.rimu.ui.ISkinnableWithRules
import game.rimu.ui.ViewDimensions
import game.rimu.ui.ViewSkinningRules


class SeekBarDimensions<T : SeekBar> : ViewDimensions<T>()

class SeekBarSkinningRules<T : SeekBar> : ViewSkinningRules<T>()

fun <T> T.SeekBar(
    attach: Boolean = true,
    block: SeekBar.() -> Unit
) where T : ViewGroup, T : IWithContext = SeekBar(ctx).also {

    if (attach)
        addView(it)

    it.block()
}

open class SeekBar(override val ctx: RimuContext) :
    AppCompatSeekBar(ctx),
    IWithContext,
    IScalableWithDimensions<SeekBar, SeekBarDimensions<SeekBar>>,
    ISkinnableWithRules<SeekBar, SeekBarSkinningRules<SeekBar>>
{

    override val skinningRules by lazy { SeekBarSkinningRules<SeekBar>() }

    override val dimensions by lazy { SeekBarDimensions<SeekBar>() }


    private val thumbDrawable = ShapeDrawable()

    private val activeBarDrawable = ShapeDrawable()

    private val inactiveBarDrawable = ShapeDrawable()


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
        splitTrack = false
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

        val accent = skin.data.colours.accentColor.toInt()

        inactiveBarDrawable.apply {
            paint.color = accent
            paint.alpha = 100
        }

        activeBarDrawable.apply {
            paint.color = accent
            paint.alpha = 200
        }

        thumbDrawable.paint.color = accent
    }

    override fun onApplyScale(scale: Float)
    {
        super.onApplyScale(scale)

        thumbDrawable.setRadius(8f)
        activeBarDrawable.setRadius(8f)
        inactiveBarDrawable.setRadius(8f)

        post {
            thumbDrawable.setSize(12, height)
        }
    }
}