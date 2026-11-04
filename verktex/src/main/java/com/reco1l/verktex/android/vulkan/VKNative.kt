package com.reco1l.verktex.android.vulkan

import android.view.Surface
import java.nio.ByteBuffer

/**
 * The JNI surface of the Vulkan backend, implemented in `src/main/cpp/vulkan`. Calls are coarse,
 * the per-draw work is encoded into one command stream per frame, see [VKCommandEncoder].
 *
 * Handles are opaque `Long`s, 0 being none. Every call but [createContext] takes the context handle.
 */
internal object VKNative {

    init {
        System.loadLibrary("verktex_vulkan")
    }

    /**
     * Creates the instance, device and per-frame resources, 0 on failure (see [lastError]).
     */
    @JvmStatic external fun createContext(enableValidation: Boolean): Long
    @JvmStatic external fun destroyContext(context: Long)

    /**
     * Why the last call that failed did.
     */
    @JvmStatic external fun lastError(): String

    @JvmStatic external fun deviceName(context: Long): String
    @JvmStatic external fun apiVersion(context: Long): Int
    @JvmStatic external fun maxImageDimension2D(context: Long): Int
    @JvmStatic external fun minUniformBufferOffsetAlignment(context: Long): Int

    /**
     * The size in bytes of each frame's ring, see [VKFrameRing].
     */
    @JvmStatic external fun ringSize(): Int

    /**
     * The size in bytes of the uniform block descriptors, which every block must fit in.
     */
    @JvmStatic external fun uniformRange(): Int

    /**
     * Replaces the window rendered to, null to release it. Returns 1 when the render pass was recreated
     * (pipelines must be recreated), 0 otherwise, or -1 on failure (see [lastError]).
     */
    @JvmStatic external fun setSurface(context: Long, surface: Surface?): Int

    /**
     * Waits for the next frame and acquires a swapchain image of the given size. Returns the frame's
     * index, which picks its ring, or -1 to skip the frame.
     */
    @JvmStatic external fun acquireFrame(context: Long, width: Int, height: Int): Int

    /**
     * The persistently mapped ring of a frame, see [VKFrameRing].
     */
    @JvmStatic external fun ringBuffer(context: Long, frameIndex: Int): ByteBuffer

    /**
     * Records the [count] ints of [commands] into the acquired frame, submits and presents it.
     */
    @JvmStatic external fun submit(context: Long, commands: ByteBuffer, count: Int)

    @JvmStatic external fun createShaderModule(context: Long, spirv: ByteArray): Long
    @JvmStatic external fun destroyShaderModule(context: Long, module: Long)

    /**
     * Creates a pipeline for the render pass. [vertexInput] is the binding count, the stride and input
     * rate of each, the attribute count and the location, binding, VkFormat and offset of each.
     * [blend] holds the VkBlendFactors of source and destination RGB, then of source and destination alpha.
     */
    @JvmStatic external fun createPipeline(context: Long, vertexModule: Long, fragmentModule: Long, vertexInput: IntArray, blend: IntArray): Long
    @JvmStatic external fun destroyPipeline(context: Long, pipeline: Long)
}
