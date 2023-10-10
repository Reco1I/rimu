package game.rimu.ui.scenes

import android.view.KeyEvent
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.IScalable
import org.andengine.entity.scene.Scene
import android.view.KeyEvent.Callback as KeyEventCallback

abstract class RimuScene(final override val ctx: RimuContext) :
    Scene(),
    IScalable,
    IWithContext,
    KeyEventCallback
{

    // Key listening events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
}