package game.rimu.ui.views

import android.annotation.SuppressLint
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import androidx.appcompat.widget.AppCompatSeekBar
import com.reco1l.framework.graphics.RoundShape
import com.reco1l.framework.graphics.setRadius
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.views.addons.IScalable
import game.rimu.ui.views.addons.ISkinnable


class SeekBar(override val ctx: RimuContext) :
    AppCompatSeekBar(ctx),
    IWithContext,
    IScalable,
    ISkinnable
{

    private val thumbDrawable = ShapeDrawable(RoundShape())

    private val activeBarDrawable = ShapeDrawable(RoundShape())

    private val inactiveBarDrawable = ShapeDrawable(RoundShape())


    init
    {
        // Bar drawable
        LayerDrawable(arrayOf(inactiveBarDrawable, activeBarDrawable)).also {

            it.setId(0, android.R.id.background)
            it.setId(1, android.R.id.progress)
            progressDrawable = it

        }

        // Thumb
        thumb = thumbDrawable
        thumbOffset = 0
        splitTrack = false
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        activeBarDrawable.apply {
            paint.color = skin.data.colours.accentColor.hexInt
        }

        inactiveBarDrawable.apply {
            paint.color = skin.data.colours.accentColor.bright(0.1f).hexInt
        }

        thumbDrawable.apply {
            paint.color = skin.data.colours.accentColor.bright(1.25f).hexInt
        }
    }

    override fun onApplyScale(scale: Float)
    {
        val barApply = fun ShapeDrawable.()
        {
            intrinsicWidth = this@SeekBar.width
            intrinsicHeight = (14 * scale).toInt()

            setRadius(null, intrinsicHeight / 2f)
        }

        inactiveBarDrawable.apply(barApply)
        activeBarDrawable.apply(barApply)

        thumbDrawable.apply {

            intrinsicWidth = (28).toScale()
            intrinsicHeight = (18).toScale()

            setRadius(radius = intrinsicHeight / 2f)
        }

        requestLayout()
    }
}