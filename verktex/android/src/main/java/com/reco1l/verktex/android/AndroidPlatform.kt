package com.reco1l.verktex.android

import kotlin.time.Duration
import com.reco1l.verktex.android.bass.BASSAudioDriver
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker
import com.reco1l.verktex.Platform
import okio.Path.Companion.toOkioPath

class AndroidPlatform(private val context: android.content.Context) : Platform.Implementation {

    override val provider = AndroidProvider(context)
    override val audioDriver by lazy { BASSAudioDriver() }
    override val currentDisplay: Platform.Display
    override val usesVirtualKeyboard = true

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


    init {
        val metrics = context.resources.displayMetrics

        currentDisplay = Platform.Display(
            density = metrics.density,
            width = metrics.widthPixels,
            height = metrics.heightPixels
        )
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
