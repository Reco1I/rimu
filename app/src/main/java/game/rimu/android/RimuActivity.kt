package game.rimu.android

import android.app.Activity
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import com.reco1l.framework.data.extensionLowercase
import com.reco1l.framework.data.toFile
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.forEachTrim
import game.rimu.ui.scenes.SceneIntro


class RimuActivity :
    Activity(),
    IWithContext
{

    override val ctx by lazy { application.baseContext as RimuContext }


    // Activity

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        ctx.activity = this
        ctx.engine.startUpdateThread()
        ctx.bassDevice.start()

        applyWindowFlags()

        async {

            ctx.initializationTree!!.forEachTrim { ctx.it() }
            ctx.initializationTree = null

            ctx.engine.scene = SceneIntro(ctx)
            onManageIntent(intent)
        }
    }


    // Data management

    fun onManageIntent(intent: Intent)
    {
        val data = intent.data ?: return

        if (intent.scheme != SCHEME_CONTENT || intent.scheme != SCHEME_FILE)
            return

        val file = data.toFile(cacheDir, contentResolver)

        when(file.extensionLowercase)
        {
            "osz" -> ctx.beatmaps.importer.import(file)
            "osk" -> ctx.skins.importer.import(file)
        }
    }


    // Activity lifecycle

    override fun onResume()
    {
        super.onResume()

        ctx.bassDevice.updatePeriod = 5
        ctx.beatmaps.current?.stream?.bufferLength = 0.1f

        ctx.engine.start()
        ctx.engine.renderView.onResume()
    }

    override fun onPause()
    {
        super.onPause()

        ctx.bassDevice.updatePeriod = 100
        ctx.beatmaps.current?.stream?.bufferLength = 0.5f

        ctx.engine.renderView.onPause()
    }

    override fun onNewIntent(intent: Intent?)
    {
        super.onNewIntent(intent)

        // If the initialization tree is null means the game already started and the activity was
        // started from being in background.
        if (ctx.initializationTree == null)
            onManageIntent(intent ?: return)
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

