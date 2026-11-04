package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.Logger
import com.reco1l.verktex.android.AndroidWindow
import com.reco1l.verktex.graphics.GraphicsContext

/**
 * The Vulkan instance, device and swapchain, see `vk_context.cpp`. The device lives as long as the
 * context, a new Android surface only recreates the swapchain, so resources survive it.
 *
 * @throws IllegalStateException if Vulkan can't be used, so the caller can fall back to OpenGL ES.
 */
class VKContext(
    private val window: AndroidWindow,
    enableValidation: Boolean,
) : GraphicsContext {

    internal val handle: Long = VKNative.createContext(enableValidation).also {
        if (it == 0L) throw IllegalStateException("Couldn't initialize Vulkan: ${VKNative.lastError()}")
    }

    override val version: String = VKNative.apiVersion(handle).let { version ->
        "Vulkan ${version ushr 22}.${(version ushr 12) and 0x3FF}.${version and 0xFFF} (${VKNative.deviceName(handle)})"
    }

    /**
     * The alignment of uniform block offsets in the ring.
     */
    val uniformAlignment = VKNative.minUniformBufferOffsetAlignment(handle).coerceAtLeast(4)

    /**
     * The bytes each uniform block slice takes in the ring, the size of its descriptor.
     */
    val uniformRange = VKNative.uniformRange()

    /**
     * The commands of the frame, submitted by [present].
     */
    val encoder = VKCommandEncoder()

    /**
     * Called when the render pass was recreated, the pipelines made for the previous one are invalid.
     */
    var onRenderPassRecreated: (() -> Unit)? = null

    /**
     * The ring of the frame being recorded, valid from [acquireFrame] to [present].
     */
    lateinit var ring: VKFrameRing
        private set

    private val rings = arrayOfNulls<VKFrameRing>(2)
    private var surfaceGeneration = -1


    override fun acquireFrame(): Boolean {
        if (!window.hasSurface) {
            if (surfaceGeneration != -1) {
                VKNative.setSurface(handle, null)
                surfaceGeneration = -1
            }
            return false
        }

        if (surfaceGeneration != window.surfaceGeneration) {
            val result = VKNative.setSurface(handle, window.holder.surface)
            if (result < 0) {
                Logger.e("VKContext", "Couldn't use the window: ${VKNative.lastError()}")
                return false
            }
            surfaceGeneration = window.surfaceGeneration

            if (result == 1) {
                onRenderPassRecreated?.invoke()
            }
        }

        val frameIndex = VKNative.acquireFrame(handle, window.windowWidth, window.windowHeight)
        if (frameIndex < 0) {
            return false
        }

        ring = rings[frameIndex] ?: VKFrameRing(VKNative.ringBuffer(handle, frameIndex)).also { rings[frameIndex] = it }
        ring.reset()
        encoder.reset()
        return true
    }

    override fun present() {
        VKNative.submit(handle, encoder.buffer, encoder.size)
    }

    override fun destroy() {
        VKNative.destroyContext(handle)
    }
}
