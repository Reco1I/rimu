package com.reco1l.verktex.android

import kotlin.time.Duration
import com.reco1l.verktex.android.bass.BASSAudioDriver
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker
import com.reco1l.verktex.Platform

class AndroidPlatform(context: android.content.Context) : Platform.Implementation {

    override val provider = AndroidProvider(context)
    override val audioDriver by lazy { BASSAudioDriver() }
    override val currentDisplay: Platform.Display
    override val usesVirtualKeyboard = true

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

}