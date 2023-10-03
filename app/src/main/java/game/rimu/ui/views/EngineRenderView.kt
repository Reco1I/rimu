package game.rimu.ui.views

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.setSize
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.engine.RimuEngine
import org.andengine.opengl.view.RenderSurfaceView

class EngineRenderView(override val ctx: RimuContext, engine: RimuEngine) :
    RenderSurfaceView(ctx),
    IWithContext
{
    init
    {
        setSize(
            width = MATCH_PARENT,
            height = MATCH_PARENT
        )

        setRenderer(engine, engine)
    }
}