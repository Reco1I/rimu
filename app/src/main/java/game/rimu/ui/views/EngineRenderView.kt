package game.rimu.ui.views

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.reco1l.framework.android.views.setSize
import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.RimuEngine
import org.andengine.opengl.view.RenderSurfaceView

class EngineRenderView(override val ctx: MainContext, engine: RimuEngine) :
    RenderSurfaceView(ctx),
    IWithContext
{
    init
    {
        setSize(MATCH_PARENT, MATCH_PARENT)
        setRenderer(engine, null)
    }
}