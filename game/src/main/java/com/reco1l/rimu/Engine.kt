package com.reco1l.rimu

import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.rimu.ui.scenes.BaseScene
import com.reco1l.rimu.ui.views.EngineRenderView
import org.andengine.engine.Engine
import org.andengine.engine.camera.SmoothCamera
import org.andengine.engine.options.EngineOptions
import org.andengine.engine.options.resolutionpolicy.FillResolutionPolicy
import org.andengine.entity.scene.Scene


fun EngineOptions(): EngineOptions
{
    // 1x1 are just initial values, they'll be changed along with surface.
    val options = EngineOptions(FillResolutionPolicy(), SmoothCamera(1f, 1f))

    options.camera.isResizeOnSurfaceSizeChanged = true
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

    Engine(EngineOptions()),
    IWithContext,
    ISkinnable
{

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

    override fun getCamera() = super.getCamera() as SmoothCamera


    override fun onUpdateCameraSurface()
    {
        super.onUpdateCameraSurface()

        ctx.layouts.onSurfaceChange(surfaceWidth, surfaceHeight)
    }
}