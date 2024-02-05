package com.reco1l.rimu.ui.scenes

import android.view.KeyEvent
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.management.beatmap.IBeatmapObserver
import com.reco1l.rimu.ui.IScalable
import com.reco1l.rimu.ui.ISkinnable
import ktx.app.KtxScreen
import android.view.KeyEvent.Callback as KeyEventCallback

abstract class BaseScene(final override val ctx: MainContext) :
    KtxScreen,
    IScalable,
    ISkinnable,
    IWithContext,
    IBeatmapObserver,
    KeyEventCallback
{

    val stage = Stage(ScreenViewport())


    open fun onAttached()
    {
        Gdx.input.inputProcessor = stage

        ctx.beatmaps.bindObserver(observer = this)
        invalidateSkin()
    }

    open fun onDetached()
    {
        ctx.beatmaps.unbindObserver(this)
    }


    fun attachChild(actor: Actor) = stage.addActor(actor)

    fun detachChild(actor: Actor) = stage.root.removeActor(actor)


    override fun render(delta: Float)
    {
        super.render(delta)
        stage.draw()
    }

    open fun onManagedUpdate(delta: Float)
    {
        stage.act(delta)
    }

    // Key listening events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
}