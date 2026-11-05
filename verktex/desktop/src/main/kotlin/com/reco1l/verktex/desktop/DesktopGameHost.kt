package com.reco1l.verktex.desktop

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.GameHost
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.desktop.gl.GLContext
import com.reco1l.verktex.desktop.gl.GLDevice
import com.reco1l.verktex.desktop.gl.GLRenderer
import com.reco1l.verktex.fonts.Fonts
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.ui.ITextInput
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback

/**
 * The base of desktop game hosts, the counterpart of the Android platform's AndroidGameHost: it
 * opens a window, initialises the engine and turns GLFW's events into the engine's input.
 *
 * [run] must be called from the main thread (the first one on macOS, see `-XstartOnFirstThread`)
 * and returns once the window is closed.
 */
abstract class DesktopGameHost(
    private val title: String = "Verktex",
    private val initialWidth: Int = 1280,
    private val initialHeight: Int = 720,
) : GameHost {

    /**
     * The OpenGL context and window, once [run] created them.
     */
    lateinit var context: GLContext
        private set

    private val handle: Long
        get() = context.window.handle

    // The window's size in screen coordinates, the cursor's are in those. The framebuffer can be
    // larger (e.g. on Retina displays), the engine works in framebuffer pixels.
    private var windowSizeWidth = 1
    private var windowSizeHeight = 1

    private var pressedButtons = 0

    @Volatile
    private var textInput: ITextInput? = null


    /**
     * Opens the window and runs the engine until the window is closed.
     */
    fun run() {
        Logger.implementation = DesktopLogger()
        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit()) {
            throw IllegalStateException("Couldn't initialise GLFW.")
        }

        try {
            context = GLContext.create(title, initialWidth, initialHeight)

            val scaleX = FloatArray(1)
            val scaleY = FloatArray(1)
            glfwGetWindowContentScale(handle, scaleX, scaleY)

            val mode = glfwGetVideoMode(glfwGetPrimaryMonitor())
            Platform.implementation = DesktopPlatform(
                density = scaleX[0].takeIf { it > 0f } ?: 1f,
                width = mode?.width() ?: initialWidth,
                height = mode?.height() ?: initialHeight
            )

            Verktex.initialize(
                host = this,
                renderer = GLRenderer(context, GLDevice()),
                audio = Platform.audioDriver
            )

            loadBuiltInFonts()
            onEngineInitialized()

            installCallbacks()
            glfwShowWindow(handle)

            Verktex.start()
            onEngineStarted()

            while (!glfwWindowShouldClose(handle)) {
                glfwWaitEventsTimeout(0.25)
            }

            // Stops the workers and destroys the context, which takes the window with it.
            Verktex.dispose()
        } finally {
            glfwTerminate()
            glfwSetErrorCallback(null)?.free()
        }
    }

    /**
     * Asks the window to close, from any thread. [run] returns once the engine stopped.
     */
    fun close() {
        glfwSetWindowShouldClose(handle, true)
        glfwPostEmptyEvent()
    }


    private fun loadBuiltInFonts() {
        fun load(key: String, file: String) =
            Fonts.load(key, "fonts/$file", isInternal = true)
                ?: throw IllegalStateException("Failed to load built-in font: $file")

        BuiltIn.Fonts.Default = load("nunito", "nunito-medium.ttf")
        BuiltIn.Fonts.Torus = load("torus", "torus-bold.otf")
        BuiltIn.Fonts.FontAwesomeIconRegular = load("fa-regular", "fa-regular.otf")
        BuiltIn.Fonts.FontAwesomeIconSolid = load("fa-solid", "fa-solid.otf")
        BuiltIn.Fonts.FontAwesomeIconBrands = load("fa-brands", "fa-brands.otf")
    }

    private fun installCallbacks() {
        val window = context.window

        val width = IntArray(1)
        val height = IntArray(1)
        glfwGetWindowSize(handle, width, height)
        windowSizeWidth = width[0].coerceAtLeast(1)
        windowSizeHeight = height[0].coerceAtLeast(1)

        glfwSetWindowSizeCallback(handle) { _, w, h ->
            windowSizeWidth = w.coerceAtLeast(1)
            windowSizeHeight = h.coerceAtLeast(1)
        }

        glfwSetFramebufferSizeCallback(handle) { _, w, h ->
            window.windowWidth = w
            window.windowHeight = h
        }

        glfwSetWindowPosCallback(handle) { _, _, _ -> window.updateRefreshRate() }

        glfwSetWindowFocusCallback(handle) { _, focused -> Verktex.onGameWindowFocusChange(focused) }

        // Minimised there's no framebuffer, the workers are paused instead of spinning.
        glfwSetWindowIconifyCallback(handle) { _, iconified ->
            if (iconified) Verktex.pause() else Verktex.resume()
        }

        glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
            val inputButton = when (button) {
                GLFW_MOUSE_BUTTON_LEFT -> PointerEvent.Button.Left
                GLFW_MOUSE_BUTTON_RIGHT -> PointerEvent.Button.Right
                else -> return@glfwSetMouseButtonCallback
            }

            when (action) {
                GLFW_PRESS -> {
                    pressedButtons++
                    dispatchPointer(PointerEvent.Action.Down, inputButton)
                }
                GLFW_RELEASE -> {
                    pressedButtons = (pressedButtons - 1).coerceAtLeast(0)
                    dispatchPointer(PointerEvent.Action.Up, inputButton)
                }
            }
        }

        // The mouse only moves the pointer while a button is held, like a touch on Android.
        glfwSetCursorPosCallback(handle) { _, _, _ ->
            if (pressedButtons > 0) {
                dispatchPointer(PointerEvent.Action.Move, PointerEvent.Button.Unknown)
            }
        }

        glfwSetScrollCallback(handle) { _, deltaX, deltaY ->
            // Positive is up and left, as Android's scroll axes.
            Input.dispatchScrollEvent(pointerId = 0, deltaX = deltaX.toFloat(), deltaY = deltaY.toFloat())
        }

        glfwSetCharCallback(handle) { _, codePoint ->
            val input = textInput ?: return@glfwSetCharCallback
            Verktex.withTreeLock { TextInputEditor.insert(input, String(Character.toChars(codePoint))) }
        }

        glfwSetKeyCallback(handle) { _, key, _, action, mods ->
            if (action != GLFW_RELEASE && handleTextEditingKey(key, mods)) {
                return@glfwSetKeyCallback
            }

            val consumed = Input.dispatchKeyboardEvent(
                // A held key repeats as more presses, as on Android.
                action = if (action == GLFW_RELEASE) KeyboardEvent.Action.Up else KeyboardEvent.Action.Down,
                key = glfwKeyToInputKey(key),
                shiftPressed = mods and GLFW_MOD_SHIFT != 0,
                ctrlPressed = mods and GLFW_MOD_CONTROL != 0,
                altPressed = mods and GLFW_MOD_ALT != 0,
                metaPressed = mods and GLFW_MOD_SUPER != 0
            )

            if (!consumed && action == GLFW_PRESS) {
                onUnhandledKeyDown(key, mods)
            }
        }
    }

    // The keys editing the attached text input, which go to it instead of the engine, as keys go
    // to the hidden EditText on Android while it's focused.
    private fun handleTextEditingKey(key: Int, mods: Int): Boolean {
        val input = textInput ?: return false
        val shortcut = mods and (GLFW_MOD_CONTROL or GLFW_MOD_SUPER) != 0

        when {
            key == GLFW_KEY_BACKSPACE -> Verktex.withTreeLock { TextInputEditor.deleteLast(input) }
            key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER -> Verktex.withTreeLock { input.onImeAction() }
            key == GLFW_KEY_V && shortcut -> {
                // Pasted on one line, as a single line input on Android.
                val text = glfwGetClipboardString(handle)?.replace(Regex("[\r\n]+"), " ") ?: return true
                Verktex.withTreeLock { TextInputEditor.insert(input, text) }
            }
            else -> return false
        }
        return true
    }

    private fun dispatchPointer(action: PointerEvent.Action, button: PointerEvent.Button) {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        glfwGetCursorPos(handle, x, y)

        // Screen coordinates to framebuffer pixels.
        val window = context.window
        val scaleX = window.windowWidth.toFloat() / windowSizeWidth
        val scaleY = window.windowHeight.toFloat() / windowSizeHeight

        Input.dispatchPointerEvent(
            action = action,
            button = button,
            pointerId = 0,
            x = x[0].toFloat() * scaleX,
            y = y[0].toFloat() * scaleY,
            time = (glfwGetTime() * 1000).toLong()
        )
    }

    /**
     * Called with a GLFW key code when a key press wasn't consumed by the current scene.
     */
    protected open fun onUnhandledKeyDown(key: Int, mods: Int) = Unit


    override fun attachTextInput(textInput: ITextInput) {
        this.textInput = textInput
    }

    override fun detachTextInput() {
        textInput = null
    }
}
