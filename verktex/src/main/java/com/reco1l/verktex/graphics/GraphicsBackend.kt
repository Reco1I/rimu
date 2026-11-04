package com.reco1l.verktex.graphics

/**
 * The graphics backends the engine can render with, stored by [name] in
 * [com.reco1l.verktex.settings.VerktexSettings.GraphicsBackend].
 */
enum class GraphicsBackend(

    /**
     * The name shown to users, e.g. in the performance overlay.
     */
    val displayName: String,
) {

    /**
     * The most capable backend the device supports.
     */
    Auto("Automatic"),

    /**
     * OpenGL ES 3.0 and up, falling back to [OpenGLES2] where it's unavailable.
     */
    OpenGLES3("OpenGL ES 3"),

    /**
     * OpenGL ES 2.0.
     */
    OpenGLES2("OpenGL ES 2"),

    /**
     * Vulkan 1.1 (or 1.0 with VK_KHR_maintenance1), falling back to [OpenGLES3] where it can't be
     * initialized or crashed while starting the last time. Experimental: users opt in, [Auto] doesn't
     * pick it, see `verktex/docs/vulkan-backend.md`.
     */
    Vulkan("Vulkan");


    companion object {

        /**
         * The backend stored as [name], or [Auto] if there's none by that name.
         */
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: Auto
    }
}
