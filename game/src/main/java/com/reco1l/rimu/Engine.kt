package com.reco1l.rimu

import android.view.WindowManager
import androidx.core.content.getSystemService
import com.badlogic.gdx.Gdx
import com.reco1l.rimu.management.skin.WorkingSkin
import com.reco1l.rimu.ui.ISkinnable
import com.reco1l.rimu.ui.scenes.BaseScene
import ktx.app.KtxGame
import kotlin.reflect.KClass


fun emptyScene(ctx: MainContext) = object : BaseScene(ctx) {}

class RimuEngine(override val ctx: MainContext) :
    KtxGame<BaseScene>(),
    IWithContext,
    ISkinnable
{

    val isRunning = true

    val updateThread by lazy { UpdateThread(this) }


    /**
     * The expected frame time, by default the 60 FPS equivalent which is 16ms.
     */
    var expectedFrameTime = 1f / 60f
        private set

    var surfaceWidth: Int = 1280
        private set

    var surfaceHeight: Int = 720
        private set

    var scene: BaseScene?
        get() = currentScreen as? BaseScene
        set(value) { setScene(value ?: emptyScene(ctx)) }


    init
    {
        ctx.onPostInitialization {

            ctx.skins.bindObserver(observer = this@RimuEngine)
        }

        // Setting the expected frame time to the display refresh rate.
        expectedFrameTime = 1f / ctx.getSystemService<WindowManager>()!!.defaultDisplay.refreshRate
    }


    inline fun <reified T : BaseScene> setScene(scene: T, clazz: KClass<T> = T::class)
    {
        if (this.scene != scene)
        {
            val lastScene = this.scene

            if (!containsScreen<T>())
                addScreen<T>(scene)

            setScreen<T>()
            ctx.layouts.onSceneChange(scene)

            lastScene?.onDetached()
            scene.onAttached()
        }
    }

    override fun render()
    {
        super.render()
        currentScreen?.render(Gdx.graphics.deltaTime)
        updateThread.tick()
    }

    fun runOnUpdateThread(task: () -> Unit) = updateThread.postTask(task)


    fun registerUpdateHandler(task: UpdateHandler) = updateThread.addHandler(task)

    fun unregisterUpdateHandler(task: UpdateHandler) = updateThread.removeHandler(task)


    fun onUpdateThreadTick()
    {
        val screen = currentScreen

        if (screen is BaseScene)
            screen.onManagedUpdate(Gdx.graphics.deltaTime)
    }


    override fun onApplySkin(skin: WorkingSkin) = updateThread { scene?.onApplySkin(skin) ?: Unit }

    override fun resize(width: Int, height: Int)
    {
        super.resize(width, height)
        surfaceWidth = width
        surfaceHeight = height

        ctx.layouts.onSurfaceChange(width, height)
    }
}


fun interface UpdateHandler
{
    fun onUpdate(sDelta: Float)
}

class UpdateThread(private val engine: RimuEngine) : Thread()
{

    private val taskQueue = mutableListOf<() -> Unit>()

    private val handlers = mutableListOf<UpdateHandler>()

    private var shouldTick = false


    override fun run()
    {
        while (true)
        {
            if (!shouldTick)
                continue

            shouldTick = false

            while (taskQueue.isNotEmpty())
                taskQueue.removeFirst().invoke()

            engine.onUpdateThreadTick()
        }
    }

    /**
     * Add a task to run in the next tick.
     */
    fun postTask(task: () -> Unit) = taskQueue.add(task)

    /**
     * Add a handler to run on every tick.
     */
    fun addHandler(handler: UpdateHandler) = handlers.add(handler)

    /**
     * Remove a handler.
     */
    fun removeHandler(handler: UpdateHandler) = handlers.remove(handler)

    /**
     * Notify that the update thread should tick, this should be called by the rendering thread.
     */
    fun tick()
    {
        shouldTick = true
    }
}