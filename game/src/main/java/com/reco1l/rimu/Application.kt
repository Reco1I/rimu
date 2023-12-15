package com.reco1l.rimu

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
     * The current activity instance.
     */
    lateinit var activity: MainActivity


    /**
     * The initialization tree, all task that should be initialized asynchronously after the
     * activity creation should be placed here.
     */
    var initializationTree: MutableList<MainContext.() -> Unit>? = mutableListOf()


    // Engine

    /**
     * The main handler.
     */
    val handler = Handler(mainLooper)

    /**
     * The BASS device.
     */
    val bass by lazy { BassDevice() }

    /**
     * The game engine.
     */
    val engine by lazy { RimuEngine(this) }


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
        // theme (or descendant).' warning.
        // That warning is thrown because views are intended to be instantiated with activity context
        // rather than application context but since rimu! doesn't use theme attributes we ignore it.
        setTheme(R.style.Theme_Rimu)
    }


    // Compatibility

    /**
     * Because we're supporting low APIs this method should replace [getDrawable].
     * Internally this uses [AppCompatResources].
     */
    fun getDrawableCompat(@DrawableRes resId: Int) = AppCompatResources.getDrawable(this, resId)
}


/**
 * Indicates that an object contains context, usually used as global reference.
 */
interface IWithContext
{
    /**The app instance context*/
    val ctx: MainContext

    /**
     * Run a block into the main thread.
     */
    fun mainThread(block: () -> Unit) = ctx.handler.post(block)

    /**
     * Run a block into the update thread.
     *
     * @param waitEngine If `true` and the engine is paused the task will wait for the engine to start.
     */
    fun updateThread(waitEngine: Boolean = false, block: () -> Unit) =
        ctx.engine.runOnUpdateThread(block, waitEngine)
}