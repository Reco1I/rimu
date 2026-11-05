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
     * Records the [count] ints of [commands] into the acquired frame and submits it.
     */
    @JvmStatic external fun submit(context: Long, commands: ByteBuffer, count: Int)

    /**
     * Presents the submitted frame, which may wait for the display.
     */
    @JvmStatic external fun present(context: Long)

    @JvmStatic external fun createShaderModule(context: Long, spirv: ByteArray): Long
    @JvmStatic external fun destroyShaderModule(context: Long, module: Long)

    /**
     * Creates a pipeline for the render passes. [vertexInput] is the binding count, the stride and input
     * rate of each, the attribute count and the location, binding, VkFormat and offset of each.
     * [blend] holds the VkBlendFactors of source and destination RGB, then of source and destination
     * alpha. [depth] holds whether the test and writes are enabled, then the VkCompareOp. [ycbcrSampler]
     * is the sampler of the video frames the pipeline samples (see [textureYcbcrSampler]), 0 otherwise.
     */
    @JvmStatic external fun createPipeline(context: Long, vertexModule: Long, fragmentModule: Long, vertexInput: IntArray, blend: IntArray, depth: IntArray, ycbcrSampler: Long): Long

    /**
     * Destroys the pipeline once the frames using it are done.
     */
    @JvmStatic external fun destroyPipeline(context: Long, pipeline: Long)

    /**
     * Creates an RGBA8 texture, cleared to transparent before the next frame draws. Any thread.
     */
    @JvmStatic external fun createTexture(context: Long, width: Int, height: Int): Long

    /**
     * Copies [size] bytes of [pixels] (a direct buffer) from [offset] into a rectangle of the texture,
     * before the next frame draws. [pixels] is only read during the call.
     */
    @JvmStatic external fun uploadTexture(context: Long, texture: Long, x: Int, y: Int, width: Int, height: Int, pixels: java.nio.ByteBuffer, offset: Int, size: Int)

    /**
     * Destroys the texture once the frames using it are done. Any thread.
     */
    @JvmStatic external fun destroyTexture(context: Long, texture: Long)

    /**
     * Whether video frames can be imported with [importHardwareBuffer].
     */
    @JvmStatic external fun supportsExternalTextures(context: Long): Boolean

    /**
     * Imports a video frame as a texture, acquired from its producer before the next frame draws, 0
     * when it can't be. It keeps its own reference to the buffer. Destroy it with [destroyTexture].
     */
    @JvmStatic external fun importHardwareBuffer(context: Long, hardwareBuffer: android.hardware.HardwareBuffer): Long

    /**
     * The YCbCr sampler an imported frame is sampled with, 0 for none. Pipelines sampling it are made for it.
     */
    @JvmStatic external fun textureYcbcrSampler(context: Long, texture: Long): Long

    /**
     * The frames submitted so far, and how many of them the GPU completed.
     */
    @JvmStatic external fun submissionCount(context: Long): Long
    @JvmStatic external fun completedSubmissionCount(context: Long): Long

    /**
     * Milliseconds the GPU took for the last measured frame, -1 when unknown.
     */
    @JvmStatic external fun gpuTime(context: Long): Float
}
