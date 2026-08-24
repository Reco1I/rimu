package com.reco1l.verktex.graphics

interface GraphicsContext {

    val version: String

    /**
     * Makes this context the current one for rendering.
     */
    fun makeCurrent()

    /**
     * Swaps the front and back buffers, displaying the rendered content on the screen.
     */
    fun swapBuffers()

    /**
     * Destroys this context and releases any associated resources. After calling this method,
     * the context should not be used again.
     */
    fun destroy()

}