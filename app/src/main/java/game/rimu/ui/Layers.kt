package game.rimu.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.attachTo
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.ui.layouts.Background
import game.rimu.ui.layouts.TopBarLayout
import game.rimu.ui.layouts.NotificationCenter
import game.rimu.ui.views.ConstraintLayout


abstract class BaseLayer(override val ctx: MainContext) : ConstraintLayout(ctx), IWithContext
{

    override val dimensions = super.dimensions.apply {

        width = MATCH_PARENT
        height = MATCH_PARENT
    }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        invalidateScale()
        invalidateSkin()
    }
}


class LayerBackground(ctx: MainContext) : BaseLayer(ctx)
{
    init
    {
        ctx.engine.renderView attachTo this

        ctx.initializationTree!!.add {

            layouts[Background::class]
        }
    }
}


class LayerOverlay(ctx: MainContext) : BaseLayer(ctx)
{
    init
    {
        ctx.initializationTree!!.add {

            layouts[TopBarLayout::class]
            layouts[NotificationCenter::class]
        }

    }
}

class LayerScene(ctx: MainContext) : BaseLayer(ctx)

