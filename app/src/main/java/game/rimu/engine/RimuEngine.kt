package game.rimu.engine

import game.rimu.IWithContext
import game.rimu.MainContext
import game.rimu.engine.surface.EngineCamera
import game.rimu.engine.surface.EngineSurface
import game.rimu.management.skin.WorkingSkin
import game.rimu.ui.ISkinnable
import game.rimu.ui.scenes.BaseScene
import game.rimu.ui.views.EngineRenderView
import org.andengine.engine.Engine
import org.andengine.engine.options.EngineOptions
import org.andengine.entity.scene.Scene
import org.andengine.opengl.util.GLState
import org.andengine.opengl.view.IRendererListener


private fun EngineOptions(ctx: MainContext): EngineOptions
{
    val options = EngineOptions(EngineSurface(ctx), EngineCamera(ctx))

    options.renderOptions.apply {

        isDithering = true

        // Setting color space to support 32 bit per pixel images.
        configChooserOptions.apply {
            requestedRedSize = 8
            requestedBlueSize = 8
            requestedGreenSize = 8
            requestedAlphaSize = 8
            requestedDepthSize = 24
        }
    }

    return options
}


class RimuEngine(override val ctx: MainContext) :

    Engine(EngineOptions(ctx)),
    IRendererListener,
    IWithContext,
    ISkinnable
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


    init
    {
        ctx.initializationTree!!.add {

            ctx.skins.bindObserver(observer = this@RimuEngine)
        }
    }


    override fun setScene(scene: Scene?)
    {
        if (scene !is BaseScene)
            throw ClassCastException("This engine only supports ${BaseScene::class} types")

        if (getScene() != scene)
        {
            ctx.layouts.onSceneChange(scene)

            getScene()?.onDetached()
            super.setScene(scene)
            scene.onAttached()
        }
    }

    override fun onApplySkin(skin: WorkingSkin)
    {
        scene?.onApplySkin(skin)
    }


    override fun getScene() = super.getScene() as? BaseScene

    override fun getCamera() = super.getCamera() as EngineCamera


    override fun onSurfaceCreated(gl: GLState?) = Unit

    override fun onSurfaceChanged(gl: GLState?, width: Int, height: Int) = Unit
}