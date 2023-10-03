package game.rimu.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.attachTo
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.layouts.Background
import game.rimu.ui.layouts.TopBarLayout
import game.rimu.ui.views.ConstraintLayout


abstract class LayoutLayer(
    override val ctx: RimuContext,
    onInitialization: LayoutLayer.() -> Unit = {}
) :
    ConstraintLayout(ctx),
    IWithContext
{
    init
    {
        dimensions.width = MATCH_PARENT
        dimensions.height = MATCH_PARENT
        onInitialization()
    }

    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        onApplyScale()
        onApplySkin()
    }
}


class LayerBackground(ctx: RimuContext) : LayoutLayer(ctx, {

    ctx.engine.renderView attachTo this

    ctx.initializationTree!!.add {

        layouts.load(Background::class)
    }
})


class LayerOverlay(ctx: RimuContext) : LayoutLayer(ctx, {


    ctx.initializationTree!!.add {

        layouts.load(TopBarLayout::class)
    }

})

class LayerScene(ctx: RimuContext) : LayoutLayer(ctx)

