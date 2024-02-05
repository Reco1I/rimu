package com.reco1l.rimu.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.layouts.Background
import com.reco1l.rimu.ui.layouts.DebugOverlay
import com.reco1l.rimu.ui.layouts.TopBarLayout
import com.reco1l.rimu.ui.layouts.NotificationCenter
import com.reco1l.rimu.ui.views.ConstraintLayout


abstract class BaseLayer(override val ctx: MainContext) : ConstraintLayout(ctx), IWithContext
{

    override val dimensions = super.dimensions.apply {

        width = MATCH_PARENT
        height = MATCH_PARENT
    }
}


class LayerBackground(ctx: MainContext) : BaseLayer(ctx)
{
    init
    {
        ctx.onPostInitialization {

            layouts[Background::class]
        }
    }
}


class LayerOverlay(ctx: MainContext) : BaseLayer(ctx)
{
    init
    {
        ctx.onPostInitialization {

            layouts[TopBarLayout::class]
            layouts[NotificationCenter::class]
            layouts[DebugOverlay::class]
        }

    }
}

class LayerScene(ctx: MainContext) : BaseLayer(ctx)

