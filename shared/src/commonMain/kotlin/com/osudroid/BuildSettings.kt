package com.osudroid

import com.reco1l.verktex.Platform

object BuildSettings {

    /**
     * Whether to use fake multiplayer mode or not.
     */
    val MOCK_MULTIPLAYER = Platform.application.isDebug

    /**
     * Whether to use the debug playground scene or not.
     */
    const val DEBUG_PLAYGROUND = false
}


