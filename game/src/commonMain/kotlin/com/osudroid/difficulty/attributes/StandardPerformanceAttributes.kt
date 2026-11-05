package com.osudroid.difficulty.attributes

import kotlin.jvm.JvmField

/**
 * A structure containing the osu!standard performance values of a score.
 */
class StandardPerformanceAttributes : PerformanceAttributes() {
    /**
     * The speed performance value.
     */
    @JvmField
    var speed = 0.0
}