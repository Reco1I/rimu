package game.rimu.android

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Vibrator
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.reco1l.basskt.BassDevice
import com.reco1l.framework.android.getSystemService
import game.rimu.engine.RimuEngine
import game.rimu.management.DatabaseManager
import game.rimu.management.LayoutManager
import game.rimu.management.MusicService
import game.rimu.management.SettingManager
import game.rimu.management.beatmap.BeatmapManager
import game.rimu.management.resources.ResourceManager
import game.rimu.management.skin.SkinManager

/**
 * This will grab the instance for rimu!
 */
class RimuContext(base: Context) : ContextWrapper(base)
{

    /**
     * The current activity instance.
     */
    lateinit var activity: RimuActivity


    /**
     * The initialization tree, all task that should be initialized asynchronously after the
     * activity creation should be placed here.
     */
    var initializationTree: MutableList<RimuContext.() -> Unit>? = mutableListOf()


    // Services

    /**
     * The music service.
     */
    lateinit var musicService: MusicService

    /**
     * The vibrator service.
     */
    val vibratorService = getSystemService<Vibrator>()


    // Engine

    /**
     * The main handler.
     */
    val handler = Handler(mainLooper)

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
     * The beatmap manager.
     */
    val beatmaps = BeatmapManager(this)

    /**
     * The resource manager.
     */
    val resources = ResourceManager(this)

    /**
     * The skin manager.
     */
    val skins = SkinManager(this)


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
    val ctx: RimuContext

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