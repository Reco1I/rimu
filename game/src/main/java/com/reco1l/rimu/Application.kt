package com.reco1l.rimu

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.reco1l.basskt.BassDevice
import com.reco1l.rimu.management.DatabaseManager
import com.reco1l.rimu.management.LayoutManager
import com.reco1l.rimu.management.SettingManager
import com.reco1l.rimu.management.beatmap.BeatmapManager
import com.reco1l.rimu.management.resources.ResourceManager
import com.reco1l.rimu.management.skin.SkinManager
import com.reco1l.toolkt.kotlin.async
import com.reco1l.toolkt.kotlin.forEachTrim


class RimuApplication : Application()
{

    override fun attachBaseContext(base: Context) = super.attachBaseContext(MainContext(base))

    override fun getBaseContext() = super.getBaseContext() as MainContext

}

/**
 * This will grab the entire game instances.
 */
class MainContext(base: Context) : ContextWrapper(base)
{


    /**
     * The initialization queue.
     */
    private var initializationQueue: MutableList<MainContext.() -> Unit>? = mutableListOf()


    /**
     * The main handler.
     */
    val handler = Handler(mainLooper)

    /**
     * Whether the game was already initialized or not.
     */
    val isInitialized
        get() = initializationQueue == null


    // Engines

    /**
     * The BASS device.
     */
    val bass = BassDevice()

    /**
     * The game engine.
     */
    val engine = RimuEngine(this)


    // Managers

    /**
     * The settings manager.
     */
    val settings = SettingManager(this)

    /**
     * The layout manager.
     */
    val layouts = LayoutManager(this)

    /**
     * The database manager.
     */
    val database = DatabaseManager(this)

    /**
     * The resource manager.
     */
    val resources = ResourceManager(this)

    /**
     * The beatmap manager.
     */
    val beatmaps = BeatmapManager(this)

    /**
     * The skin manager.
     */
    val skins = SkinManager(this)


    init
    {
        // Fixes 'View class ... is an AppCompat widget that can only be used with a Theme.AppCompat
        // theme (or descendant).' warning (and sometimes it produces a crash).
        // That warning is thrown because views are intended to be instantiated with activity context
        // rather than application context but since rimu! doesn't use theme attributes we ignore it.
        setTheme(R.style.Theme_Rimu)

        bass.start()
        engine.start()
    }


    // Compatibility

    /**
     * Because we're supporting low APIs this method should replace [getDrawable].
     * Internally this uses [AppCompatResources].
     */
    fun getDrawableCompat(@DrawableRes resId: Int) = AppCompatResources.getDrawable(this, resId)


    // Functions

    /**
     * Notifies that a new activity was created.
     */
    fun onActivityCreate(newActivity: Activity)
    {
        // Notify layout manager and replace content view.
        layouts.onActivityCreate(newActivity)

        if (!isInitialized) async {

            // Iterating all over the task submitted to the initialization tree and executing them
            // in asynchronous from UI thread.
            initializationQueue!!.forEachTrim { it() }
            initializationQueue = null

            engine.startUpdateThread()
        }
    }

    /**
     * Execute a task after the engine initialization, if the engine is already initialized the task will
     * be executed immediately.
     */
    fun onPostInitialization(

        priorityIndex: Int = initializationQueue?.size ?: 0,

        block: MainContext.() -> Unit

    ) = initializationQueue?.add(priorityIndex, block) ?: block(this)
}


/**
 * Indicates that an object contains context, usually used as global reference.
 */
interface IWithContext
{

    /**
     * The main application context (also known as "GlobalManager" in terms of osu!droid).
     */
    val ctx: MainContext

}


/**
 * Run a block into the update thread.
 *
 * @param waitEngine If `true` and the engine is paused the task will wait for the engine to start.
 */
fun IWithContext.updateThread(waitEngine: Boolean = false, block: () -> Unit) = ctx.engine.runOnUpdateThread(block, waitEngine)

/**
 * Run a block into the main thread.
 */
fun IWithContext.mainThread(block: () -> Unit) = ctx.handler.post(block)