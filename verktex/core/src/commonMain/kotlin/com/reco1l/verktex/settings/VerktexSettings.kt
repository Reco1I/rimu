package com.reco1l.verktex.settings

import kotlin.jvm.JvmField

/**
 * The settings of the engine.
 */
object VerktexSettings {

    /**
     * Whether every component is outlined, to see its bounds.
     */
    @JvmField
    val ShowComponentBoundaries = setting("show_component_boundaries", false)

    /**
     * Whether the frame times and the other performance statistics are shown above everything.
     */
    @JvmField
    val ShowPerformanceOverlay = setting("show_performance_overlay", false)

    /**
     * The resolution the window renders at relative to the display, from 0 to 1. Below 1 fewer
     * pixels are drawn and the display scales them up.
     */
    @JvmField
    val ResolutionScale = setting("resolution_scale", 1f)

    /**
     * The name of the [com.reco1l.verktex.graphics.GraphicsBackend] to render with. It's read once
     * when the engine is initialized, so a change applies on the next start.
     */
    @JvmField
    val GraphicsBackend = setting("graphics_backend", "Auto")

    /**
     * Set while the Vulkan backend starts and cleared once it presented a frame. Found set when
     * starting, Vulkan crashed the last time, and OpenGL ES is used until Vulkan is picked again.
     */
    @JvmField
    val VulkanStarting = setting("vulkan_starting", false)
}
