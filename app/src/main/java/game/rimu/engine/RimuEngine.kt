package game.rimu.engine

import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.engine.surface.EngineCamera
import game.rimu.engine.surface.EngineSurface
import game.rimu.ui.scenes.BaseScene
import game.rimu.ui.views.EngineRenderView
import org.andengine.engine.Engine
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.ScreenOrientation.LANDSCAPE_SENSOR
import org.andengine.entity.scene.Scene
import org.andengine.opengl.util.GLState
import org.andengine.opengl.view.IRendererListener

class RimuEngine(override val ctx: RimuContext) :

    Engine(
        EngineOptions(
            true,
            LANDSCAPE_SENSOR,
            EngineSurface(ctx),
            EngineCamera(ctx)
        )
    ),
    IRendererListener,
    IWithContext
{

    /**
     * The engine surface manager aka ratio resolution policy.
     */
    val surface
        get() = engineOptions.resolutionPolicy as EngineSurface

    /**
     * The engine render view.
     */
    val renderView = EngineRenderView(ctx, this)


    override fun setScene(scene: Scene?)
    {
        if (scene !is BaseScene)
            throw ClassCastException("This engine only supports ${BaseScene::class} types")

        if (getScene() != scene)
        {
            ctx.layouts.onSceneChange(scene)
            super.setScene(scene)
        }
    }


    override fun getScene() = super.getScene() as? BaseScene

    override fun getCamera() = super.getCamera() as EngineCamera


    override fun onSurfaceCreated(gl: GLState?) = Unit

    override fun onSurfaceChanged(gl: GLState?, width: Int, height: Int) = Unit
}