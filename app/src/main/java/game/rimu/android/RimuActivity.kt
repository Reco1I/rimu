package game.rimu.android

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.forEachTrim
import game.rimu.ui.scenes.SceneIntro


class RimuActivity :
    Activity(),
    IWithContext
{

    override lateinit var ctx: RimuContext


    // Activity

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        ctx = application.baseContext as RimuContext
        ctx.activity = this

        applyWindowFlags()

        // Initializing engine
        ctx.engine.startUpdateThread()

        async {

            ctx.initializationTree!!.forEachTrim { ctx.it() }
            ctx.initializationTree = null

            // Setting intro scene
            ctx.engine.scene = SceneIntro(ctx)
        }
    }


    override fun onResume()
    {
        super.onResume()

        ctx.engine.start()
        ctx.engine.renderView.onResume()
    }

    override fun onPause()
    {
        super.onPause()

        ctx.engine.renderView.onPause()
    }


    // Window events

    private fun applyWindowFlags()
    {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_FULLSCREEN
    }

    override fun onWindowFocusChanged(pHasWindowFocus: Boolean)
    {
        super.onWindowFocusChanged(pHasWindowFocus)

        applyWindowFlags()
    }


    // Key events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (super.onKeyDown(keyCode, event))
            return true

        return ctx.engine.scene?.onKeyDown(keyCode, event) ?: false
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (super.onKeyLongPress(keyCode, event))
            return true

        return ctx.engine.scene?.onKeyLongPress(keyCode, event) ?: false
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (super.onKeyUp(keyCode, event))
            return true

        return ctx.engine.scene?.onKeyUp(keyCode, event) ?: false
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean
    {
        if (super.onKeyMultiple(keyCode, repeatCount, event))
            return true

        return ctx.engine.scene?.onKeyMultiple(keyCode, repeatCount, event) ?: false
    }
}

