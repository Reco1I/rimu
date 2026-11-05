package com.osudroid.math

/**
 * Rounds the exact binary value of [value] to [digits] decimal places, with ties going away from zero,
 * as a `BigDecimal` made from the [Double] itself rounds with `RoundingMode.HALF_UP`. Unlike
 * [preciseRoundBy], which rounds the shortest decimal representation of the number, a value such as
 * 2.675, which is slightly below that in binary, rounds down.
 */
expect fun roundExactHalfUp(value: Double, digits: Int): Float
