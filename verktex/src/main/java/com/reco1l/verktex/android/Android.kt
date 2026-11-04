package com.reco1l.verktex.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.view.KeyEvent
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.android.bass.BASSAudioDriver
import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.fonts.Font
import com.reco1l.verktex.android.gles.ESContext
import com.reco1l.verktex.android.gles.ESVersion
import com.reco1l.verktex.android.gles.es2.ES2Device
import com.reco1l.verktex.android.gles.es2.ES2Renderer
import com.reco1l.verktex.android.gles.es3.ES3Device
import com.reco1l.verktex.android.gles.es3.ES3Renderer
import com.reco1l.verktex.android.vulkan.VKContext
import com.reco1l.verktex.android.vulkan.VKDevice
import com.reco1l.verktex.android.vulkan.VKRenderer
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings

fun Verktex.initializeAndroid(
    host: AndroidGameHost,
    audioDriver: AudioDriver = BASSAudioDriver()
): AndroidWindow {

    Platform.implementation = AndroidPlatform(host)
    Logger.implementation = AndroidLogger()

    val window = AndroidWindow(host)

    val backend = GraphicsBackend.fromName(Settings[VerktexSettings.GraphicsBackend, GraphicsBackend.Auto.name])

    val renderer = (if (backend == GraphicsBackend.Vulkan) createVulkanRenderer(host, window) else null)
        ?: createESRenderer(window, if (backend == GraphicsBackend.OpenGLES2) ESVersion.ES2 else ESVersion.ES3)

    initialize(
        host = host,
        renderer = renderer,
        audio = audioDriver
    )

    return window
}

/**
 * Whether the device reports Vulkan 1.1 hardware, the version the experimental Vulkan backend
 * targets. It may still fall back to OpenGL ES if it can't be initialized.
 */
fun isVulkanAvailable(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_1_1)
}

/**
 * Lets Vulkan be tried again after it crashed while starting (see [createVulkanRenderer]), e.g. once
 * the user picks it again.
 */
fun resetVulkanCrashGuard(context: Context) {
    crashGuardPreferences(context).edit().remove(KEY_VULKAN_STARTING).apply()
}

private const val VULKAN_1_1 = 0x401000
private const val KEY_VULKAN_STARTING = "vulkan_starting"

private fun crashGuardPreferences(context: Context) =
    context.getSharedPreferences("verktex_graphics", Context.MODE_PRIVATE)

// Null when Vulkan can't be used on the device, the caller falls back to OpenGL ES.
private fun createVulkanRenderer(host: AndroidGameHost, window: AndroidWindow): GraphicsRenderer? {
    // A driver crash in native code isn't an exception, the process dies. A flag is kept from before
    // Vulkan starts until its first frame is presented: found on the next launch, Vulkan crashed, and
    // OpenGL ES is used until it's picked again instead of crashing on every launch.
    val preferences = crashGuardPreferences(host)
    if (preferences.getBoolean(KEY_VULKAN_STARTING, false)) {
        Logger.w("Verktex", "Vulkan didn't start the last time, using OpenGL ES. Pick it again in the settings to retry.")
        return null
    }
    // Committed synchronously, it must be on disk if the process dies right after.
    preferences.edit().putBoolean(KEY_VULKAN_STARTING, true).commit()

    val isDebuggable = host.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    var context: VKContext? = null

    return try {
        context = VKContext(window, enableValidation = isDebuggable)
        VKRenderer(context, VKDevice(context), window).also { renderer ->
            Logger.i("Verktex", "Using the experimental Vulkan backend: ${context.version}.")
            renderer.onFirstFramePresented = { resetVulkanCrashGuard(host) }
        }
    } catch (e: Throwable) {
        // UnsatisfiedLinkError included, e.g. an ABI the native library wasn't built for. It failed
        // without crashing, there's no reason not to try again next time.
        Logger.e("Verktex", "Couldn't use Vulkan, falling back to OpenGL ES.", e)
        try { context?.destroy() } catch (_: Throwable) {}
        resetVulkanCrashGuard(host)
        null
    }
}

private fun createESRenderer(window: AndroidWindow, preferredVersion: ESVersion): GraphicsRenderer {
    val context = ESContext(window, preferredVersion)

    Logger.i("Verktex", "Using the OpenGL ${context.esVersion} backend.")

    return when (context.esVersion) {
        ESVersion.ES3 -> ES3Renderer(context, ES3Device(), window)
        ESVersion.ES2 -> ES2Renderer(context, ES2Device(), window)
    }
}

fun KeyEvent.toInputKey(): KeyboardEvent.Key {
    return when (keyCode) {
        KeyEvent.KEYCODE_A -> KeyboardEvent.Key.A
        KeyEvent.KEYCODE_B -> KeyboardEvent.Key.B
        KeyEvent.KEYCODE_C -> KeyboardEvent.Key.C
        KeyEvent.KEYCODE_D -> KeyboardEvent.Key.D
        KeyEvent.KEYCODE_E -> KeyboardEvent.Key.E
        KeyEvent.KEYCODE_F -> KeyboardEvent.Key.F
        KeyEvent.KEYCODE_G -> KeyboardEvent.Key.G
        KeyEvent.KEYCODE_H -> KeyboardEvent.Key.H
        KeyEvent.KEYCODE_I -> KeyboardEvent.Key.I
        KeyEvent.KEYCODE_J -> KeyboardEvent.Key.J
        KeyEvent.KEYCODE_K -> KeyboardEvent.Key.K
        KeyEvent.KEYCODE_L -> KeyboardEvent.Key.L
        KeyEvent.KEYCODE_M -> KeyboardEvent.Key.M
        KeyEvent.KEYCODE_N -> KeyboardEvent.Key.N
        KeyEvent.KEYCODE_O -> KeyboardEvent.Key.O
        KeyEvent.KEYCODE_P -> KeyboardEvent.Key.P
        KeyEvent.KEYCODE_Q -> KeyboardEvent.Key.Q
        KeyEvent.KEYCODE_R -> KeyboardEvent.Key.R
        KeyEvent.KEYCODE_S -> KeyboardEvent.Key.S
        KeyEvent.KEYCODE_T -> KeyboardEvent.Key.T
        KeyEvent.KEYCODE_U -> KeyboardEvent.Key.U
        KeyEvent.KEYCODE_V -> KeyboardEvent.Key.V
        KeyEvent.KEYCODE_W -> KeyboardEvent.Key.W
        KeyEvent.KEYCODE_X -> KeyboardEvent.Key.X
        KeyEvent.KEYCODE_Y -> KeyboardEvent.Key.Y
        KeyEvent.KEYCODE_Z -> KeyboardEvent.Key.Z
        KeyEvent.KEYCODE_0 -> KeyboardEvent.Key.Digit0
        KeyEvent.KEYCODE_1 -> KeyboardEvent.Key.Digit1
        KeyEvent.KEYCODE_2 -> KeyboardEvent.Key.Digit2
        KeyEvent.KEYCODE_3 -> KeyboardEvent.Key.Digit3
        KeyEvent.KEYCODE_4 -> KeyboardEvent.Key.Digit4
        KeyEvent.KEYCODE_5 -> KeyboardEvent.Key.Digit5
        KeyEvent.KEYCODE_6 -> KeyboardEvent.Key.Digit6
        KeyEvent.KEYCODE_7 -> KeyboardEvent.Key.Digit7
        KeyEvent.KEYCODE_8 -> KeyboardEvent.Key.Digit8
        KeyEvent.KEYCODE_9 -> KeyboardEvent.Key.Digit9
        KeyEvent.KEYCODE_ENTER -> KeyboardEvent.Key.Enter
        KeyEvent.KEYCODE_DEL -> KeyboardEvent.Key.Backspace
        KeyEvent.KEYCODE_TAB -> KeyboardEvent.Key.Tab
        KeyEvent.KEYCODE_SPACE -> KeyboardEvent.Key.Space

        KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyboardEvent.Key.Shift
        KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> KeyboardEvent.Key.Ctrl
        KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> KeyboardEvent.Key.Alt
        KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> KeyboardEvent.Key.Meta

        KeyEvent.KEYCODE_ESCAPE -> KeyboardEvent.Key.Escape
        KeyEvent.KEYCODE_BACK -> KeyboardEvent.Key.Back
        KeyEvent.KEYCODE_MENU -> KeyboardEvent.Key.Menu
        KeyEvent.KEYCODE_FORWARD_DEL -> KeyboardEvent.Key.Delete
        KeyEvent.KEYCODE_MOVE_HOME -> KeyboardEvent.Key.Home
        KeyEvent.KEYCODE_MOVE_END -> KeyboardEvent.Key.End
        KeyEvent.KEYCODE_PAGE_UP -> KeyboardEvent.Key.PageUp
        KeyEvent.KEYCODE_PAGE_DOWN -> KeyboardEvent.Key.PageDown
        KeyEvent.KEYCODE_DPAD_UP -> KeyboardEvent.Key.Up
        KeyEvent.KEYCODE_DPAD_DOWN -> KeyboardEvent.Key.Down
        KeyEvent.KEYCODE_DPAD_LEFT -> KeyboardEvent.Key.Left
        KeyEvent.KEYCODE_DPAD_RIGHT -> KeyboardEvent.Key.Right
        else -> {
            Logger.w("InputManager", "Unknown key code: $keyCode")
            KeyboardEvent.Key.Unknown
        }
    }
}

fun Font.Style.toAndroidTypefaceStyle(weight: Font.Weight): Int {

    val isBold = weight >= Font.Weight.Bold
    val isItalic = this == Font.Style.Italic

    // For oblique style, we will treat it as normal for the purpose of Typeface style, since Android
    // does not have a direct oblique style we'll use text skewing to simulate it later.
    return when {
        isBold && isItalic -> Typeface.BOLD_ITALIC
        isBold -> Typeface.BOLD
        isItalic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
}

fun Font.Weight.toAndroidTypefaceWeight(): Int {
    return when (this) {
        Font.Weight.Thin -> 100
        Font.Weight.ExtraLight -> 200
        Font.Weight.Light -> 300
        Font.Weight.Normal -> 400
        Font.Weight.Medium -> 500
        Font.Weight.SemiBold -> 600
        Font.Weight.Bold -> 700
        Font.Weight.ExtraBold -> 800
        Font.Weight.Black -> 900
    }
}