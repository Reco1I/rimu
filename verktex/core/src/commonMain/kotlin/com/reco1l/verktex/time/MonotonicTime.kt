package com.reco1l.verktex.time

import kotlin.time.TimeSource

private val origin = TimeSource.Monotonic.markNow()

/**
 * Nanoseconds of a monotonic clock, only meaningful compared with other values of it, like
 * `System.nanoTime()` on the JVM.
 */
internal fun monotonicNanos(): Long = origin.elapsedNow().inWholeNanoseconds
