package com.reco1l.rimu.ui.scenes

import android.view.KeyEvent
import com.reco1l.rimu.IWithContext
import com.reco1l.rimu.MainContext
import com.reco1l.rimu.ui.IScalable
import com.reco1l.rimu.ui.ISkinnable
import org.andengine.entity.scene.Scene
import android.view.KeyEvent.Callback as KeyEventCallback

abstract class BaseScene(final override val ctx: MainContext) :
    Scene(),
    IScalable,
    ISkinnable,
    IWithContext,
    KeyEventCallback
{


    override fun onAttached() = invalidateSkin()
    

    // Key listening events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = true

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
}