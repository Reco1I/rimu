package game.rimu.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.forEach
import com.reco1l.framework.android.views.attachTo
import com.reco1l.framework.lang.safeIn
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.layouts.Background
import game.rimu.ui.layouts.ModelLayout
import game.rimu.ui.layouts.TopBarLayout
import game.rimu.ui.layouts.NotificationCenter
import game.rimu.ui.scenes.BaseScene
import game.rimu.ui.views.ConstraintLayout


abstract class BaseLayer(override val ctx: RimuContext) : ConstraintLayout(ctx), IWithContext
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

    fun onSceneChange(scene: BaseScene) = forEach { layout ->

        if (layout is ModelLayout && scene::class safeIn layout.parents)
            layout.hide()
    }
}


class LayerBackground(ctx: RimuContext) : BaseLayer(ctx)
{
    init
    {
        ctx.engine.renderView attachTo this

        ctx.initializationTree!!.add {

            layouts[Background::class]
        }
    }
}


class LayerOverlay(ctx: RimuContext) : BaseLayer(ctx)
{
    init
    {
        ctx.initializationTree!!.add {

            layouts[TopBarLayout::class]
            layouts[NotificationCenter::class]
        }

    }
}

class LayerScene(ctx: RimuContext) : BaseLayer(ctx)

