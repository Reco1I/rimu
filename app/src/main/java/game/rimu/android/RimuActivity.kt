package game.rimu.android

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import com.reco1l.framework.android.logE
import com.reco1l.framework.data.extensionLowercase
import com.reco1l.framework.data.subDirectory
import com.reco1l.framework.data.toFile
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.forEachTrim
import com.reco1l.framework.lang.klass
import game.rimu.ui.layouts.Notification
import game.rimu.ui.layouts.NotificationCenter
import game.rimu.ui.scenes.SceneIntro


class RimuActivity :
    AppCompatActivity(),
    IWithContext
{

    override val ctx by lazy { application.baseContext as RimuContext }


    // Activity

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        ctx.activity = this

        // Application wasn't initialized yet.
        if (ctx.initializationTree != null)
        {
            onFirstCreate()
            return
        }

        ctx.layouts.onActivityCreate()
    }

    private fun onFirstCreate()
    {
        ctx.engine.startUpdateThread()
        ctx.bassDevice.start()

        applyWindowFlags()

        async {
            ctx.initializationTree!!.forEachTrim { ctx.it() }
            ctx.initializationTree = null

            ctx.engine.scene = SceneIntro(ctx)
            onManageIntent(intent)

            mainThread {
                ctx.layouts[NotificationCenter::class].add(Notification(
                    header = "Welcome to rimu!",
                    message = "This is a test build, bugs are expected.",
                    icon = "icon-notification"
                ))
            }
        }

    }


    // Data management

    fun onManageIntent(intent: Intent)
    {
        fun onManageUri(uri: Uri) = try
        {
            val file = uri.toFile(cacheDir.subDirectory("import"), contentResolver)

            when (file.extensionLowercase)
            {
                "osz" -> ctx.beatmaps.importer.import(file)
                "osk" -> ctx.skins.importer.import(file)
            }
            Unit
        }
        catch (e: Exception)
        {
            klass logE ("Failed to import file from URI." to e)
        }

        when (intent.action)
        {
            ACTION_VIEW -> onManageUri(intent.data ?: return)
            ACTION_SEND -> onManageUri(intent.getParcelableExtra(EXTRA_STREAM) ?: return)
            ACTION_SEND_MULTIPLE ->
            {
                intent.getParcelableArrayListExtra<Uri>(EXTRA_STREAM)?.forEach {

                    onManageUri(it)
                }
            }
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

