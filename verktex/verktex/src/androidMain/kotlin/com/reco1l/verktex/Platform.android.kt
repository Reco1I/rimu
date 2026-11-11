package com.reco1l.verktex

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import com.reco1l.verktex.android.AndroidGameHost
import com.reco1l.verktex.android.AndroidHaptics
import com.reco1l.verktex.android.AndroidProvider
import com.reco1l.verktex.android.WebPageActivity
import com.reco1l.verktex.android.bass.BASSAudioDriver
import com.reco1l.verktex.android.isVulkanAvailable
import com.reco1l.verktex.android.video.ExoPlayerVideoDecoder
import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.io.systemFileSystem
import com.reco1l.verktex.video.VideoDecoder
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker
import java.io.File
import java.util.Locale
import kotlin.time.Duration
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

/**
 * The Android platform, over the activity that hosts the engine, see [attach].
 */
actual object Platform {

    @Volatile
    private var host: Host? = null

    private val attached: Host
        get() = host ?: throw IllegalStateException("The platform isn't attached to a host yet")

    private val context: Context
        get() = attached.context

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }


    /**
     * Attaches the platform to the [context] that hosts the engine, an [AndroidGameHost] for
     * everything to work.
     */
    internal fun attach(context: Context) {
        host = Host(context)
    }


    actual val target = PlatformTarget.Android

    actual val provider: Provider
        get() = attached.provider

    actual val uptimeMillis: Long
        get() = SystemClock.uptimeMillis()

    actual val fileSystem: FileSystem
        get() = systemFileSystem

    actual val storage: PlatformStorage
        get() = PlatformStorage(
            dataDirectory = context.filesDir.toOkioPath(),
            cacheDirectory = context.cacheDir.toOkioPath(),
            externalDirectory = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                @Suppress("DEPRECATION")
                Environment.getExternalStorageDirectory().toOkioPath()
            } else null,
        )

    actual val audioDriver: AudioDriver by lazy { BASSAudioDriver() }

    @Volatile
    actual var resolutionScale = 1f

    // The whole display, system bars included: the engine draws edge to edge.
    actual val currentDisplay: PlatformDisplay
        get() = attached.display

    actual val usesVirtualKeyboard = true

    actual val systemLocale: String
        get() = if (host != null) Locale.getDefault().toLanguageTag() else "en"

    actual val application: PlatformApplication
        get() = host?.application ?: PlatformApplication()

    actual val isInForeground: Boolean
        get() = (context as? AndroidGameHost)?.isInForeground ?: true

    // Without OpenGL ES 3 or with less than 2 GB of memory.
    actual val isLowEndDevice: Boolean
        get() = attached.isLowEndDevice

    actual val availableGraphicsBackends: List<GraphicsBackend>
        get() = listOfNotNull(
            GraphicsBackend.Auto,
            GraphicsBackend.OpenGLES3,
            GraphicsBackend.OpenGLES2,
            GraphicsBackend.Vulkan.takeIf { isVulkanAvailable(context) },
        )

    actual val haptics: Haptics?
        get() = attached.haptics

    // The alternative refresh rates of the display are only known from Android 12.
    actual val canForceMaxRefreshRate = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    actual var forceMaxRefreshRate = false
        set(value) {
            field = value
            applyForceMaxRefreshRate(value)
        }

    actual val canPickFiles: Boolean
        get() = context is ComponentActivity

    // The thread's handler ends the application, reporting the crash.
    actual var onUncaughtException: (Throwable) -> Unit = { throwable ->
        val thread = Thread.currentThread()
        val handler = if (host != null) thread.uncaughtExceptionHandler else null

        handler?.uncaughtException(thread, throwable) ?: throw throwable
    }

    // External textures are what the decoder draws into, some backends can't show them (e.g. Vulkan before Android 10).
    actual val canPlayVideos: Boolean
        get() {
            context
            return Verktex.graphics.device.capabilities.supportsExternalTextures
        }


    actual fun openWebPage(url: String, userAgent: String?) {
        WebPageActivity.open(context, url, userAgent)
    }

    actual fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    actual fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker {
        context
        return object : ThreadLoopWorker(name, minimumInterval) {
            override fun onLoop() = onLoop()
        }
    }

    actual fun runOnMainThread(block: () -> Unit) {
        context
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    actual fun exit() {
        (context as? Activity)?.finish()
    }

    actual fun copyToClipboard(text: String, label: String) {
        context.getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    actual fun pickFiles(mimeType: String, onPicked: (List<Path>) -> Unit) {
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
                            Logger.e("Platform", "Couldn't read the picked file $uri", e)
                            null
                        }
                    }
                    onPicked(files)
                }, "pickFiles").start()
            }
            launcher.launch(mimeType)
        }
    }

    actual fun createVideoDecoder(path: String): VideoDecoder? {
        return if (canPlayVideos) ExoPlayerVideoDecoder(context, path) else null
    }


    private fun applyForceMaxRefreshRate(enabled: Boolean) {
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


    /**
     * What's read from the context hosting the engine, once per host.
     */
    private class Host(val context: Context) {

        val provider = AndroidProvider(context)

        val haptics: Haptics? by lazy { AndroidHaptics.create(context) }

        val display: PlatformDisplay = run {
            val metrics = DisplayMetrics()
            val windowManager = context.getSystemService<WindowManager>()

            if (windowManager != null) {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
            } else {
                metrics.setTo(context.resources.displayMetrics)
            }

            PlatformDisplay(density = context.resources.displayMetrics.density, width = metrics.widthPixels, height = metrics.heightPixels)
        }

        val application = PlatformApplication(
            versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (_: Exception) {
                ""
            },
            isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        )

        val isLowEndDevice: Boolean by lazy {
            val activityManager = context.getSystemService<ActivityManager>() ?: return@lazy false

            if (activityManager.deviceConfigurationInfo.reqGlEsVersion < 0x30000) {
                return@lazy true
            }

            val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
            memory.totalMem / (1024 * 1024) < 2000
        }
    }
}
