package com.reco1l.verktex.android

import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.Platform

class AndroidPlatform(context: android.content.Context) : Platform.Implementation {

    override val provider = AndroidProvider(context)
    override val currentDisplay: Platform.Display
    override val usesVirtualKeyboard = true


    init {
        val metrics = context.resources.displayMetrics

        currentDisplay = Platform.Display(
            density = metrics.density,
            width = metrics.widthPixels,
            height = metrics.heightPixels
        )
    }


    override fun createLoopWorker(name: String, onLoop: () -> Unit): LoopWorker {
        return object : AndroidLoopWorker(name) {
            override fun onLoop() = onLoop()
        }
    }

}