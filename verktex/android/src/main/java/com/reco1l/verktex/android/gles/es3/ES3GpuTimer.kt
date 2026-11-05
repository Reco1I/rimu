package com.reco1l.verktex.android.gles.es3

import android.opengl.GLES30
import android.opengl.GLES20

/**
 * Measures how long the GPU takes to execute each frame with `GL_EXT_disjoint_timer_query`.
 *
 * Results arrive a few frames late: each frame uses one query of a ring and reads the oldest one
 * when it's available, so the CPU never waits for the GPU.
 */
internal class ES3GpuTimer {

    /**
     * Whether the device supports timer queries. Checked on the first frame, with a context.
     */
    var isSupported = false
        private set

    /**
     * Milliseconds the GPU took to execute the last measured frame, or -1 if unknown.
     */
    var gpuTime = -1f
        private set

    private var checkedSupport = false

    private val queries = IntArray(QUERY_COUNT)
    private val pending = BooleanArray(QUERY_COUNT)
    private var current = 0
    private var running = false

    private val result = IntArray(1)


    fun begin() {
        if (!checkedSupport) {
            checkedSupport = true
            isSupported = GLES20.glGetString(GLES20.GL_EXTENSIONS)?.contains("GL_EXT_disjoint_timer_query") == true
        }

        if (!isSupported) {
            return
        }

        // The queries go away with the context, they're made again for the new one.
        if (queries[0] == 0 || !GLES30.glIsQuery(queries[0])) {
            GLES30.glGenQueries(QUERY_COUNT, queries, 0)
            pending.fill(false)
            current = 0
        }

        // A query still waiting for its result can't be reused, the frame isn't measured.
        if (pending[current]) {
            return
        }

        // Clears the disjoint flag, which is set when the timings may be wrong (e.g. the GPU
        // changed its frequency).
        GLES20.glGetIntegerv(GL_GPU_DISJOINT_EXT, result, 0)

        GLES30.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[current])
        running = true
    }

    fun end() {
        if (!running) {
            return
        }

        GLES30.glEndQuery(GL_TIME_ELAPSED_EXT)
        running = false
        pending[current] = true
        current = (current + 1) % QUERY_COUNT

        // The oldest query of the ring is the one most likely to be ready.
        val oldest = current
        if (!pending[oldest]) {
            return
        }

        GLES30.glGetQueryObjectuiv(queries[oldest], GLES30.GL_QUERY_RESULT_AVAILABLE, result, 0)
        if (result[0] == 0) {
            return
        }

        GLES30.glGetQueryObjectuiv(queries[oldest], GLES30.GL_QUERY_RESULT, result, 0)
        pending[oldest] = false

        val nanos = result[0].toLong() and 0xFFFFFFFFL

        GLES20.glGetIntegerv(GL_GPU_DISJOINT_EXT, result, 0)
        if (result[0] == 0) {
            gpuTime = nanos / 1_000_000f
        }
    }


    private companion object {
        const val QUERY_COUNT = 4

        const val GL_TIME_ELAPSED_EXT = 0x88BF
        const val GL_GPU_DISJOINT_EXT = 0x8FBB
    }
}
