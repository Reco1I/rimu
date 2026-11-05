package com.osudroid

object BuildSettings {

    /**
     * Whether to use fake multiplayer mode or not.
     */
    val MOCK_MULTIPLAYER = OsuPlatform.implementation.isDebug

    /**
     * Whether to use the debug playground scene or not.
     */
    const val DEBUG_PLAYGROUND = false
}


