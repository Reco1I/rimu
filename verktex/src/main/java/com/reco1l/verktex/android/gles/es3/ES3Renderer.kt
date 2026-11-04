package com.reco1l.verktex.android.gles.es3

import com.reco1l.verktex.PerformanceStats
import com.reco1l.verktex.android.gles.ESContext
import com.reco1l.verktex.android.gles.ESRenderer
import com.reco1l.verktex.android.gles.ESWindow

/**
 * The [ESRenderer] of OpenGL ES 3.0 and up, which also measures the GPU time of each frame.
 */
class ES3Renderer(
    context: ESContext,
    device: ES3Device,
    window: ESWindow,
) : ESRenderer(context, device, window, ES3CommandExecutor()) {

    private val gpuTimer = ES3GpuTimer()


    override fun onFrameBegin() {
        gpuTimer.begin()
    }

    override fun onFrameExecuted() {
        gpuTimer.end()
        PerformanceStats.gpuTime = gpuTimer.gpuTime
    }
}
