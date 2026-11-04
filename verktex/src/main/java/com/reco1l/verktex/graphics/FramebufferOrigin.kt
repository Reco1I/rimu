package com.reco1l.verktex.graphics

/**
 * Where the first row of a backend's framebuffer is, which decides how a framebuffer capture is
 * stored. The engine's own coordinates always start at the top left.
 */
enum class FramebufferOrigin {

    /**
     * Rows start at the top, as with Vulkan. A capture has the same orientation as other textures.
     */
    TopLeft,

    /**
     * Rows start at the bottom, as with OpenGL. A capture is stored upside down, the bottom row of
     * the window at v = 0.
     */
    BottomLeft
}
