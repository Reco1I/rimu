package com.reco1l.verktex.graphics

interface GraphicsContext {

    val version: String

    /**
     * Prepares the window for a new frame on the calling thread, (re)creating whatever depends on
     * its surface when it changed.
     *
     * @return `false` if there is nothing to render to right now, in which case the frame is skipped.
     */
    fun acquireFrame(): Boolean

    /**
     * Displays the frame rendered since [acquireFrame] on the window.
     */
    fun present()

    /**
     * Destroys this context and releases any associated resources. After calling this method,
     * the context should not be used again.
     */
    fun destroy()

}
