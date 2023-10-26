package game.rimu

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
import com.reco1l.framework.data.resolveFilename
import com.reco1l.framework.kotlin.async
import com.reco1l.framework.kotlin.forEachTrim
import com.reco1l.framework.kotlin.ignoreException
import game.rimu.ui.scenes.SceneIntro


class MainActivity :
    AppCompatActivity(),
    IWithContext
{

    override val ctx by lazy { application.baseContext as MainContext }


    // Activity

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // Setting the activity global reference to this one.
        ctx.activity = this

        // Application wasn't initialized yet.
        if (ctx.initializationTree != null)
        {
            onInitializeGame()
            return
        }

        // Applying window fullscreen flags before setting the content view.
        onApplyWindowFlags()
        ctx.layouts.onActivityCreate()
    }

    private fun onInitializeGame()
    {
        ctx.engine.startUpdateThread()
        ctx.bassDevice.start()

        async {
            // Iterating all over the task submitted to the initialization tree and executing them in
            // asynchronous from UI thread.
            ctx.initializationTree!!.forEachTrim { ctx.it() }
            ctx.initializationTree = null

            // Setting first scene to the Intro scene which will play an storyboard.
            ctx.engine.scene = SceneIntro(ctx)

            // If the activity was started with an intent we managed it after the initialization.
            onManageIntent(intent)
        }
    }


    // Data management

    fun onManageIntent(intent: Intent)
    {
        /**
         * Manages the given content/file scheme URI and copies the content to cache directory.
         */
        fun onManageUri(uri: Uri) = ignoreException {

            when (uri.resolveFilename(contentResolver).substringAfterLast('.'))
            {
                "osz" -> ctx.beatmaps.importer.import(uri)
                "osk" -> ctx.skins.importer.import(uri)
            }
        }

        async {
            // 'getParcelableExtra' and 'getParcelableArrayListExtra' are deprecated but current
            // replacement is exclusive to newer APIs so there's no replacement.
            @Suppress("DEPRECATION")
            when (intent.action)
            {
                // The intent was sent through the 'Open with...' option.
                ACTION_VIEW -> onManageUri(intent.data ?: return@async)

                // The intent was sent through the 'Share' option.
                ACTION_SEND -> onManageUri(intent.getParcelableExtra(EXTRA_STREAM) ?: return@async)
                ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<Uri>(EXTRA_STREAM)
                    ?.forEach { onManageUri(it) }
            }
        }
    }


    // Activity lifecycle

    override fun onPause()
    {
        super.onPause()

        // When the game is on background there's no need to keep a low delay properties as it may
        // consume unnecessary resources leading to the game being closed by Android.
        ctx.bassDevice.updatePeriod = 100
        ctx.beatmaps.current?.stream?.bufferLength = 0.5f

        ctx.engine.renderView.onPause()
    }

    override fun onResume()
    {
        super.onResume()

        // Once we're back we can restore the previous values.
        ctx.bassDevice.updatePeriod = 5
        ctx.beatmaps.current?.stream?.bufferLength = 0.1f

        // Starting engine if it wasn't yet, this will only take effect the first time opening the game.
        ctx.engine.start()

        ctx.engine.renderView.onResume()
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

    private fun onApplyWindowFlags()
    {
        // Deprecated flags but current replacement requires higher API.
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_HIDE_NAVIGATION or SYSTEM_UI_FLAG_FULLSCREEN
    }

    override fun onWindowFocusChanged(hasFocus: Boolean)
    {
        super.onWindowFocusChanged(hasFocus)

        onApplyWindowFlags()
    }


    // Key events

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (super.onKeyDown(keyCode, event))
            return true

        return ctx.engine.scene?.onKeyDown(keyCode, event)
            // Consuming back press action.
            ?: true
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

