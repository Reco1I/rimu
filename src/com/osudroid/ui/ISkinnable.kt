package com.osudroid.ui

/**
 * Interface for entities that change their appearance based on the current skin.
 */
fun interface ISkinnable {

    /**
     * Called when the skin is changed.
     */
    fun onSkinChanged()
}