package com.osudroid.beatmaps

import com.reco1l.verktex.data.Color4
import kotlin.jvm.JvmField

/**
 * A wrapper of [Color4] specifically for combo colors.
 */
data class ComboColor(
    /**
     * The index of this combo color.
     */
    @JvmField val index: Int,

    /**
     * The wrapped [Color4].
     */
    val color: Color4
)
