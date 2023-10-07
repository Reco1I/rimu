package game.rimu.ui.views

import android.graphics.drawable.LayerDrawable
import androidx.appcompat.widget.AppCompatSeekBar
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.drawables.RoundShape
import game.rimu.ui.IScalable
import game.rimu.ui.ISkinnable
import game.rimu.ui.dimensions


class SeekBar(override val ctx: RimuContext) :
    AppCompatSeekBar(ctx),
    IWithContext,
    IScalable,
    ISkinnable
{

    private val thumbDrawable = RoundShape {

        dimensions {
            width = 28
            height = 18
            radius = height / 2f
        }

    }.toDrawable()

    private val activeBarDrawable = RoundShape {

        dimensions {
            width = this@SeekBar.width
            height = 14
            radius = height / 2f
        }

    }.toDrawable()

    private val inactiveBarDrawable = RoundShape {

        dimensions {
            width = this@SeekBar.width
            height = 14
            radius = height / 2f
        }

    }.toDrawable()


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
            paint.color = skin.data.colours.accentColor.toInt()
        }

        inactiveBarDrawable.apply {
            paint.color = skin.data.colours.accentColor.lightenInt(0.1f)
        }

        thumbDrawable.apply {
            paint.color = skin.data.colours.accentColor.lightenInt(1.25f)
        }
    }

    override fun onApplyScale(scale: Float)
    {
        super.onApplyScale(scale)
        requestLayout()
    }
}