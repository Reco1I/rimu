package com.reco1l.verktex.android.vulkan

import com.reco1l.verktex.graphics.GraphicsBuffer

/**
 * A vertex buffer of the Vulkan backend. It has no GPU storage of its own: the renderer copies its
 * words into the frame's ring for each draw, see [VKFrameRing].
 */
class VKBuffer : GraphicsBuffer() {

    // The renderer writes the words into the ring itself, it knows the frame and the offset.
    override fun uploadDataToGPU() = Unit

    override fun onBind() = Unit
    override fun onLoad() = Unit
    override fun onUnload() = Unit
}
