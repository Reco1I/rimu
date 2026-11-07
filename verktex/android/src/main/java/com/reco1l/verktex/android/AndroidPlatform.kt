package com.reco1l.verktex.android

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Platform
import com.reco1l.verktex.android.bass.BASSAudioDriver
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker
import java.io.File
import kotlin.time.Duration
import okio.Path.Companion.toOkioPath

/**
 * The Android implementation of [Platform.Implementation], for the activity that hosts the engine.
 */
class AndroidPlatform(private val context: Context) : Platform.Implementation {

    override val target = Platform.Target.Android
    override val provider = AndroidProvider(context)
    override val audioDriver by lazy { BASSAudioDriver() }
    override val usesVirtualKeyboard = true

    override val systemLocale: String
        get() = java.util.Locale.getDefault().toLanguageTag()


    override val storage: Platform.Storage
        get() = Platform.Storage(
            dataDirectory = context.filesDir.toOkioPath(),
            cacheDirectory = context.cacheDir.toOkioPath(),
            externalDirectory = if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
                @Suppress("DEPRECATION")
                android.os.Environment.getExternalStorageDirectory().toOkioPath()
            } else null,
        )

    override val uptimeMillis: Long
        get() = android.os.SystemClock.uptimeMillis()

    // The whole display, system bars included: the engine draws edge to edge.
    override val currentDisplay: Platform.Display = run {
        val metrics = DisplayMetrics()
        val windowManager = context.getSystemService<WindowManager>()

        if (windowManager != null) {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        } else {
            metrics.setTo(context.resources.displayMetrics)
        }

        Platform.Display(density = context.resources.displayMetrics.density, width = metrics.widthPixels, height = metrics.heightPixels)
    }

    override val application = Platform.Application(
        versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        },
        isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    )

    override val isInForeground: Boolean
        get() = (context as? AndroidGameHost)?.isInForeground ?: true

    // Without OpenGL ES 3 or with less than 2 GB of memory.
    override val isLowEndDevice: Boolean by lazy {
        val activityManager = context.getSystemService<ActivityManager>() ?: return@lazy false

        if (activityManager.deviceConfigurationInfo.reqGlEsVersion < 0x30000) {
            return@lazy true
        }

        val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        memory.totalMem / (1024 * 1024) < 2000
    }

    override val availableGraphicsBackends: List<GraphicsBackend>
        get() = listOfNotNull(
            GraphicsBackend.Auto,
            GraphicsBackend.OpenGLES3,
            GraphicsBackend.OpenGLES2,
            GraphicsBackend.Vulkan.takeIf { isVulkanAvailable(context) },
        )

    override val haptics: Platform.Haptics? by lazy { AndroidHaptics.create(context) }

    // The alternative refresh rates of the display are only known from Android 12.
    override val canForceMaxRefreshRate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    override fun setForceMaxRefreshRate(enabled: Boolean) {
        val activity = context as? Activity ?: return

        activity.runOnUiThread {
            @Suppress("DEPRECATION")
            val display = activity.windowManager.defaultDisplay
            val fastestMode = display.supportedModes.maxByOrNull { it.refreshRate }

            val attributes = activity.window.attributes
            attributes.preferredDisplayModeId = if (enabled && fastestMode != null) fastestMode.modeId else 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                attributes.preferMinimalPostProcessing = enabled && display.isMinimalPostProcessingSupported
            }

            activity.window.attributes = attributes
        }
    }

    override val canPickFiles: Boolean
        get() = context is ComponentActivity

    override fun pickFiles(mimeType: String, onPicked: (List<okio.Path>) -> Unit) {
        val activity = context as? ComponentActivity ?: return

        runOnMainThread {
            // Registered when needed, since the engine's scenes aren't tied to the activity's lifecycle.
            val launcher = activity.activityResultRegistry.register("verktex.pickFiles", ActivityResultContracts.GetMultipleContents()) { uris ->
                if (uris.isEmpty()) return@register

                Thread({
                    val files = uris.mapNotNull { uri ->
                        try {
                            val file = File.createTempFile("picked", null, activity.cacheDir)
                            activity.contentResolver.openInputStream(uri)!!.use { input ->
                                file.outputStream().use { input.copyTo(it) }
                            }
                            file.toOkioPath()
                        } catch (e: Exception) {
                            Logger.e("AndroidPlatform", "Couldn't read the picked file $uri", e)
                            null
                        }
                    }
                    onPicked(files)
                }, "pickFiles").start()
            }
            launcher.launch(mimeType)
        }
    }


    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    // The thread's handler ends the application, reporting the crash.
    override fun handleUncaughtException(throwable: Throwable) {
        val thread = Thread.currentThread()
        thread.uncaughtExceptionHandler?.uncaughtException(thread, throwable) ?: throw throwable
    }

    override fun exit() {
        (context as? Activity)?.finish()
    }

    override fun copyToClipboard(text: String, label: String) {
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(label, text))
    }


    override fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker {
        return object : ThreadLoopWorker(name, minimumInterval) {
            override fun onLoop() = onLoop()
        }
    }

    override fun openWebPage(url: String, userAgent: String?) {
        WebPageActivity.open(context, url, userAgent)
    }

    override fun openUrl(url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
