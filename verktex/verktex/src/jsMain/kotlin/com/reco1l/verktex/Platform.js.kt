package com.reco1l.verktex

import com.reco1l.verktex.audio.AudioDriver
import com.reco1l.verktex.graphics.GraphicsBackend
import com.reco1l.verktex.io.systemFileSystem
import com.reco1l.verktex.video.VideoDecoder
import com.reco1l.verktex.worker.LoopWorker
import kotlin.time.Duration
import okio.FileSystem
import okio.Path

// There's no JavaScript platform, the target only checks that the common code compiles without the
// JVM's API. Nothing but what needs no host works.
actual object Platform {

    private fun unsupported(): Nothing = throw IllegalStateException("There's no JavaScript platform")

    actual val target: PlatformTarget get() = unsupported()
    actual val provider: Provider get() = unsupported()
    actual val uptimeMillis: Long get() = unsupported()
    actual val fileSystem: FileSystem get() = systemFileSystem
    actual val storage: PlatformStorage get() = unsupported()
    actual val audioDriver: AudioDriver get() = unsupported()
    actual var resolutionScale = 1f
    actual val currentDisplay: PlatformDisplay get() = unsupported()
    actual val usesVirtualKeyboard: Boolean get() = unsupported()
    actual val systemLocale: String get() = "en"
    actual val application: PlatformApplication get() = PlatformApplication()
    actual val isInForeground: Boolean get() = unsupported()
    actual val isLowEndDevice: Boolean get() = unsupported()
    actual val availableGraphicsBackends: List<GraphicsBackend> get() = unsupported()
    actual val haptics: Haptics? get() = unsupported()
    actual val canForceMaxRefreshRate: Boolean get() = unsupported()
    actual var forceMaxRefreshRate = false
    actual val canPickFiles: Boolean get() = unsupported()
    actual var onUncaughtException: (Throwable) -> Unit = { throw it }
    actual val canPlayVideos: Boolean get() = unsupported()

    actual fun openWebPage(url: String, userAgent: String?): Unit = unsupported()
    actual fun openUrl(url: String): Unit = unsupported()
    actual fun createLoopWorker(name: String, minimumInterval: Duration, onLoop: () -> Unit): LoopWorker = unsupported()
    actual fun runOnMainThread(block: () -> Unit): Unit = unsupported()
    actual fun exit(): Unit = unsupported()
    actual fun copyToClipboard(text: String, label: String): Unit = unsupported()
    actual fun pickFiles(mimeType: String, onPicked: (List<Path>) -> Unit): Unit = unsupported()
    actual fun createVideoDecoder(path: String): VideoDecoder? = unsupported()
}
