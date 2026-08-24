package com.reco1l.verktex

import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.time.TickingClock
import com.reco1l.verktex.time.SystemBasedTickingClock
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.RunOnDrawWorker
import com.reco1l.verktex.worker.RunOnUpdateWorker

/**
 * # Verktex Engine
 *
 * This is the main orchestrator, responsible for managing the game main loops and the components to
 * be drawn on screen, as well as dispatching events to the appropriate components.
 *
 * The engine is designed with modularity in mind, allowing for easy customization and extension of
 * its core functionalities:
 *
 * ### Modules of the engine
 *
 * * [graphics]: Responsible for all rendering operations.
 * * [audio]: Responsible for all audio operations.
 *
 * These clases must be initialized before starting the engine using the [start] method. Most
 * supported platforms already provide default implementations for these modules, but they can be
 * replaced with custom implementations if needed.
 *
 * ### Scene Management
 *
 * The engine manages a stack of scenes, where each scene represents a distinct state or screen in the
 * application. Scenes can be pushed onto or popped from the stack, allowing for easy navigation between
 * different parts of the application.
 *
 * ### Multiplatform Support
 *
 * The engine is designed to work across multiple platforms, ensuring a consistent experience regardless of the
 * underlying hardware or operating system.
 * It also provides access to platform-specific functionality, rendering operations, texture management,
 * input handling, and font management. These functionalities can be customized by providing custom
 * implementations for the respective modules.
 *
 * ### Workers and Threading
 *
 * The engine utilizes separate workers for drawing and updating, allowing for efficient rendering and
 * game logic processing. The [drawWorker] handles rendering operations, while the [updateWorker]
 * manages game logic updates. Both workers can be started, paused, resumed, and stopped as needed
 * by the game host.
 *
 *
 * ### Workflow
 *
 * The engine is initialized by the game host, which sets up the necessary modules and handles the
 * application lifecycle events. The engine's main loops for drawing and updating are started using the
 * [start] method.
 *
 * [GameHost] -> ([GraphicsRenderer], [AudioDriver]) -> [Verktex] -> [UIScene] -> [UIComponent]
 *
 * ### Requirements
 * This engine is built up with latest Kotlin language features, the minimum required compiler
 * version is 2.3.
 *
 * @author Reco1l
 */
object Verktex {

    //region Modules

    /**
     * The game host, responsible for managing the engine's lifecycle and dispatching native events.
     */
    lateinit var host: GameHost
        private set

    /**
     * The rendered used by the engine. This is responsible for all rendering operations.
     */
    lateinit var graphics: GraphicsRenderer
        private set

    /**
     * The audio backend used by the engine. This is responsible for all audio operations.
     */
    lateinit var audio: AudioDriver
        private set


    private var isInitialized = false


    /**
     * Initializes the engine with the specified modules.
     */
    fun initialize(host: GameHost, renderer: GraphicsRenderer, audio: AudioDriver) {
        if (isInitialized)
            throw IllegalStateException("Engine already initialized!")

        this.graphics = renderer
        this.audio = audio
        this.host = host

        isInitialized = true
    }

    //endregion

    private val sceneStack = ArrayDeque<UIScene>().apply { addLast(UIScene.EmptyScene) }
    private val overlayContainer = UIContainer()


    /**
     * The clock used across the engine.
     */
    @JvmStatic
    val clock: TickingClock
        field = SystemBasedTickingClock()

    /**
     * The currently active scene in the engine. This is the topmost scene in the scene stack.
     */
    @JvmStatic
    val scene
        get() = sceneStack.last()


    /**
     * The theme used for UI components. This defines the visual style and appearance of the UI elements.
     */
    var theme = Theme()
        set(value) {
            if (field != value) {
                field = value
                onThemeChange()
            }
        }

    /**
     * The current font scale factor. Changing this will scale all UI elements accordingly.
     */
    var fontScale = 1f
        set(value) {
            if (field != value) {
                field = value
                onThemeChange()
            }
        }


    private lateinit var drawWorker: LoopWorker
    private lateinit var updateWorker: LoopWorker

    private var started = false
    private var paused = false

    //region Lifecycle

    @RunOnDrawWorker
    private fun draw() {
        graphics.begin()
        scene.draw(graphics)
        overlayContainer.draw(graphics)
        graphics.end()
    }

    @RunOnUpdateWorker
    private fun update() {
        drawWorker.check()
        clock.tick()
        scene.update(clock)
    }


    /**
     * Starts the engine's main loops for drawing and updating. This method should be called after
     * all necessary components (e.g., platform, renderer, textures, input) have been initialized.
     *
     * Aswell this method should be called only once in the application lifecycle.
     */
    fun start() {
        if (started) {
            Logger.w("Verktex", "start() called while engine is already started")
            return
        }

        if (!isInitialized) {
            throw IllegalStateException("Cannot start() engine, initialize() must be called first!")
        }

        started = true

        drawWorker = Platform.createLoopWorker("draw_worker") { draw() }
        updateWorker = Platform.createLoopWorker("update_worker") { update() }

        drawWorker.start()
        updateWorker.start()

        clock.start()
    }

    /**
     * Resumes the engine's main loops for drawing and updating after being paused.
     */
    fun resume() {
        if (!started) {
            Logger.w("Verktex", "resume() called before start()")
            return
        }

        if (!paused) {
            Logger.w("Verktex", "resume() called while engine is not paused")
            return
        }

        Logger.i("Verktex", "Resuming draw and update workers...")

        drawWorker.resume()
        updateWorker.resume()

        clock.start()

        paused = false
    }

    /**
     * Pauses the engine's main loops for drawing and updating.
     */
    fun pause() {
        if (!started) {
            Logger.w("Verktex", "pause() called before start()")
            return
        }

        if (paused) {
            Logger.w("Verktex", "pause() called while engine is already paused")
            return
        }

        Logger.i("Verktex", "Pausing draw and update workers...")

        clock.pause()

        drawWorker.pause()
        updateWorker.pause()

        paused = true
    }

    /**
     * Disposes of the engine's resources and stops the main loops for drawing and updating.
     */
    fun dispose() {
        if (!started) {
            Logger.w("Verktex", "dispose() called before start()")
            return
        }

        Logger.i("Verktex", "Disposing engine resources and stopping workers...")

        clock.pause()

        drawWorker.stop()
        updateWorker.stop()

        started = false
    }

    //endregion

    //region UI management

    /**
     * Adds a new scene to the top of the scene stack. The newly added scene will become the active
     * scene and will be drawn and updated in subsequent frames.
     */
    @JvmStatic
    fun pushScene(scene: UIScene) {
        sceneStack.addLast(scene)
    }

    /**
     * Removes the topmost scene from the scene stack and returns it. If there are no scenes in the
     * stack, it returns null.
     */
    @JvmStatic
    fun popScene(): UIScene? {
        if (sceneStack.size <= 1) {
            Logger.w(
                "Verktex",
                "Attempted to pop the last scene from the stack. This operation is not allowed."
            )
            return null
        }

        val scene = sceneStack.lastOrNull()
        if (scene != null) {
            sceneStack.removeLast()
        }
        return scene
    }

    /**
     * Adds an overlay UI component to the engine. Overlays are drawn on top of the current scene
     * and can be used for UI elements like dialogs, notifications, or other temporary UI components.
     */
    @JvmStatic
    fun addOverlay(overlay: UIComponent) {
        overlayContainer += overlay
    }

    /**
     * Removes an overlay UI component from the engine. This will stop the overlay from being drawn
     * and receiving input events.
     */
    @JvmStatic
    fun removeOverlay(overlay: UIContainer) {
        overlayContainer -= overlay
    }

    //endregion

    //region UI focus

    var focusedComponent: IFocusable? = null
        private set


    fun requestFocus(component: IFocusable): Boolean {
        if (!component.canFocus) return false
        if (component == focusedComponent) return true

        focusedComponent?.onFocusLost()
        focusedComponent = component
        focusedComponent?.onFocusGained()
        return true
    }

    fun clearFocus() {
        focusedComponent?.onFocusLost()
        focusedComponent = null
    }

    //endregion

    //region Events

    /**
     * Schedules a task to be executed on the draw thread. The provided action will be executed during
     * the next draw cycle.
     *
     * @return A [LoopWorker.TaskState] representing the scheduled task, which can be used to manage its execution.
     */
    @JvmStatic
    fun scheduleOnDraw(action: () -> Unit): LoopWorker.TaskState {
        if (!started) {
            throw IllegalStateException("Engine has not been started. Call start() before scheduling tasks.")
        }
        return drawWorker.schedule(action)
    }

    /**
     * Schedules a task to be executed on the update thread. The provided action will be executed during
     * the next update cycle.
     *
     * @return A [LoopWorker.TaskState] representing the scheduled task, which can be used to manage its execution.
     */
    @JvmStatic
    fun scheduleOnUpdate(action: () -> Unit): LoopWorker.TaskState {
        if (!started) {
            throw IllegalStateException("Engine has not been started. Call start() before scheduling tasks.")
        }
        return updateWorker.schedule(action)
    }


    private fun onThemeChange() {
        sceneStack.forEach { scene ->
            scene.propagate { child ->
                child.onStyle(theme)
            }
        }
    }

    internal fun onInputEvent(event: InputEvent): Boolean {
        if (overlayContainer.propagateConsumable { it.onInputEvent(event) }) {
            return true
        }
        return scene.propagateConsumable { it.onInputEvent(event) }
    }

    fun onGameWindowFocusChange(hasFocus: Boolean) {
        sceneStack.forEach { scene ->
            scene.onGameWindowFocusChange(hasFocus)
        }
    }

    //endregion

}