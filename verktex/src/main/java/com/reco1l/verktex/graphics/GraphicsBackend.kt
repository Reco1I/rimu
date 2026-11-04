package com.reco1l.verktex.graphics

/**
 * The graphics backends the engine can render with, stored by [name] in
 * [com.reco1l.verktex.settings.VerktexSettings.GraphicsBackend].
 */
enum class GraphicsBackend {

    /**
     * The most capable backend the device supports.
     */
    Auto,

    /**
     * OpenGL ES 3.0 and up, falling back to [OpenGLES2] where it's unavailable.
     */
    OpenGLES3,

    /**
     * OpenGL ES 2.0.
     */
    OpenGLES2;


    companion object {

        /**
         * The backend stored as [name], or [Auto] if there's none by that name.
         */
        fun fromName(name: String?) = entries.firstOrNull { it.name == name } ?: Auto
    }
}
