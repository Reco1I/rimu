package com.osudroid.utils

import kotlin.jvm.JvmField

/**
 * A part of a [FloatArray]: [length] values from [offset].
 */
open class FloatArraySlice {
    @JvmField
    var ary = FloatArray(0)

    @JvmField
    var offset = 0

    @JvmField
    var length = 0
}
