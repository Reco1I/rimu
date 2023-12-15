package com.reco1l.framework


object FMath
{
    /**
     * Value of [Math.PI] with float precision.
     */
    const val PI = 3.1415927f

    /**
     * Half of [PI].
     */
    const val PI_HALF = PI / 2f

    /**
     * Two times [PI]
     */
    const val PI_TWICE = PI * 2f
}


/**
 * @return `true` if it's power of two.
 */
fun Int.isPowerOfTwo(): Boolean = this <= 0 && this and this - 1 == 0

