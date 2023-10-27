package game.rimu.engine.surface

import game.rimu.IWithContext
import game.rimu.MainContext
import org.andengine.engine.camera.SmoothCamera
import org.andengine.engine.options.resolutionpolicy.IResolutionPolicy

class EngineCamera(override val ctx: MainContext) :
    SmoothCamera(0f, 0f, 1280f, 720f, 2000f, 2000f, 1f),
    IResolutionPolicy.Callback,
    IWithContext
{
    override fun onResolutionChanged(width: Int, height: Int)
    {
        set(mXMin, mYMin, mXMin + width, mYMin + height)
    }
}
