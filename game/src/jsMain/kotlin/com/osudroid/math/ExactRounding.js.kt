package com.osudroid.math

// Only compiled to check the common code, see build.gradle.kts. Rounds the decimal representation,
// which differs from the exact value in the rare cases where it ends in a tie.
actual fun roundExactHalfUp(value: Double, digits: Int) = value.preciseRoundBy(digits).toFloat()
