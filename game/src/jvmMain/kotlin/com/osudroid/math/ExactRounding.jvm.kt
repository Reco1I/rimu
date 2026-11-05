package com.osudroid.math

import java.math.BigDecimal
import java.math.RoundingMode

actual fun roundExactHalfUp(value: Double, digits: Int) =
    BigDecimal(value).setScale(digits, RoundingMode.HALF_UP).toFloat()
