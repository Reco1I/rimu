package game.rimu.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.forEach
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.lang.safeIn
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.layouts.Background
import game.rimu.ui.layouts.RimuLayout
import game.rimu.ui.layouts.TopBarLayout
import game.rimu.ui.scenes.RimuScene
import game.rimu.ui.views.ConstraintLayout


abstract class LayoutLayer(
    override val ctx: RimuContext,
    init: LayoutLayer.() -> Unit = {}
) :
    ConstraintLayout(ctx, {}),
    IWithContext
{

    override val dimensions = super.dimensions.apply {

        width = MATCH_PARENT
        height = MATCH_PARENT
    }


    init { init() }


    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        invalidateScale()
        invalidateSkin()
    }

    fun onSceneChange(scene: RimuScene) = forEach { layout ->

        if (layout is RimuLayout && scene::class safeIn layout.parents)
            layout.hide()
    }
}


class LayerBackground(ctx: RimuContext) : LayoutLayer(ctx, {

    ctx.engine.renderView attachTo this

    ctx.initializationTree!!.add {

        layouts[Background::class]
    }
})


class LayerOverlay(ctx: RimuContext) : LayoutLayer(ctx, {


    ctx.initializationTree!!.add {

        layouts[TopBarLayout::class]
    }

})

class LayerScene(ctx: RimuContext) : LayoutLayer(ctx)

