package com.reco1l.verktex.desktop

import kotlin.time.Duration
import com.reco1l.verktex.Platform
import com.reco1l.verktex.desktop.bass.BASSAudioDriver
import com.reco1l.verktex.worker.LoopWorker
import com.reco1l.verktex.worker.ThreadLoopWorker

/**
 * The desktop implementation of [Platform.Implementation].
 *
 * @param density The scale of the window's content set by the system (e.g. 1.5 at 150%), which
 * density independent sizes follow.
 */
class DesktopPlatform(density: Float, width: Int, height: Int) : Platform.Implementation {

    override val provider = DesktopProvider()
    override val audioDriver by lazy { BASSAudioDriver() }
    override val currentDisplay = Platform.Display(density = density, width = width, height = height)

    // A physical keyboard, text is typed into the focused input directly.
    override val usesVirtualKeyboard = false


    override fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker {
        return object : ThreadLoopWorker(name, minimumInterval) {
            override fun onLoop() = onLoop()
        }
    }
}
