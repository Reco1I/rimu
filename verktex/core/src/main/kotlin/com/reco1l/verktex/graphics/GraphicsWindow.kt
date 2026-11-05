package com.reco1l.verktex.graphics

import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4

interface GraphicsWindow {

    /**
     * The width of the window in pixels.
     */
    var windowWidth: Int

    /**
     * The height of the window in pixels.
     */
    var windowHeight: Int

    /**
     * The insets of the window, which represent the safe area and offsets.
     */
    var insets: Insets

    /**
     * How many times per second the display shows a new frame, the most frames per second that
     * can be seen. It can change while running, e.g. when the device switches between 60 and
     * 120 Hz.
     */
    val refreshRate: Float
        get() = 60f

    /**
     * Whether the window should close.
     */
    fun shouldClose(): Boolean

    /**
     * Polls for window events, such as input events and window state changes. This method should
     * be called regularly to ensure that the application responds to user input and other events.
     */
    fun pollEvents()


    /**
     * Represents the insets of the window, which include the safe area and offsets. ,
     * while .
     */
    data class Insets(
        /**
         * The area of the window that is guaranteed to be visible and not obscured by system UI elements.
         */
        val safeArea: Vec4,

        /**
         * Represents any additional padding or margins that should be applied to main content within
         * the window (e.g., to avoid overlapping with system UI elements).
         */
        val offsets: Vec2
    )
}