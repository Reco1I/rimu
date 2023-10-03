package game.rimu.engine.surface

import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import org.andengine.engine.camera.SmoothCamera

class EngineCamera(override val ctx: RimuContext) :
    SmoothCamera(0f, 0f, 1280f, 720f, 2000f, 2000f, 1f),
    IWithContext
{
    internal fun onMeasureSurface(newWidth: Float, newHeight: Float) = set(
        mXMin,
        mYMin,
        mXMin + newWidth,
        mYMin + newHeight
    )
}
