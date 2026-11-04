@file:Suppress("ConstPropertyName")

package com.reco1l.verktex.settings

/**
 * A set of native settings that are used by the framework.
 */
object VerktexSettings {
    const val ShowComponentBoundaries = "show_component_boundaries"
    const val ShowPerformanceOverlay = "show_performance_overlay"
    const val DrawTextures = "draw_textures"

    /**
     * The resolution the window renders at relative to the display, a float from 0 to 1. Below 1
     * fewer pixels are drawn and the display scales them up.
     */
    const val ResolutionScale = "resolution_scale"

    /**
     * The graphics backend to use: "auto" (the default) picks the most capable one the device
     * supports, "es2" forces OpenGL ES 2.0. It's read once when the engine is initialized, so it
     * must be set before the host is created.
     */
    const val GraphicsBackend = "graphics_backend"
}