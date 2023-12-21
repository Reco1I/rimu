package com.reco1l.rimu

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
import com.reco1l.toolkt.data.resolveFilename
import com.reco1l.toolkt.kotlin.async
import com.reco1l.toolkt.kotlin.forEachTrim
import com.reco1l.toolkt.kotlin.ignoreException
import com.reco1l.rimu.ui.scenes.SceneIntro


class MainActivity :
    AppCompatActivity(),
    IWithContext
{

    override val ctx by lazy { application.baseContext as MainContext }


    // Activity

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        // Applying window fullscreen flags before setting the content view.
        onApplyWindowFlags()

        // In this case we reassign the content view to this new activity.
        ctx.onActivityCreate(this)

        ctx.onPostInitialization {

            // Setting first scene to the Intro scene which will play an storyboard.
            ctx.engine.scene = SceneIntro(ctx)

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
                else -> Unit
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
        ctx.bass.updatePeriod = 100
        ctx.beatmaps.current?.stream?.bufferLength = 0.5f

        ctx.engine.renderView.onPause()
    }

    override fun onResume()
    {
        super.onResume()

        // Once we're back we can restore the previous values.
        ctx.bass.updatePeriod = 5
        ctx.beatmaps.current?.stream?.bufferLength = 0.1f

        // Reloading resources and starting engine if wasn't started yet.
        ctx.engine.onReloadResources()
        ctx.engine.start()

        ctx.engine.renderView.onResume()
    }

    override fun onNewIntent(intent: Intent?)
    {
        super.onNewIntent(intent)

        // If the initialization tree is null means the game already started and the activity was
        // started from being in background.
        if (ctx.isInitialized)
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
        if (ctx.engine.scene?.onKeyDown(keyCode, event) == true)
            return true

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (ctx.engine.scene?.onKeyLongPress(keyCode, event) == true)
            return true

        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean
    {
        if (ctx.engine.scene?.onKeyUp(keyCode, event) == true)
            return true

        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean
    {
        if (ctx.engine.scene?.onKeyMultiple(keyCode, repeatCount, event) == true)
            return true

        return super.onKeyMultiple(keyCode, repeatCount, event)
    }
}

