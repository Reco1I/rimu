package game.rimu.ui.scenes

import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import game.rimu.android.IWithContext
import game.rimu.android.RimuContext
import game.rimu.ui.views.addons.IScalable
import org.andengine.entity.scene.Scene

abstract class RimuScene(final override val ctx: RimuContext) :
    Scene(),
    KeyEvent.Callback,
    IScalable,
    IWithContext
{

    /**
     * The scene root layout where all layouts related to the scene will be attached.
     */
    val layout = ConstraintLayout(ctx)


    init
    {
        this.onCreate()
    }


    abstract fun onCreate()


    // View features

    fun addView(view: View) = layout.addView(view)

    fun addView(view: View, index: Int) = layout.addView(view, index)


    // Key listening events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
}