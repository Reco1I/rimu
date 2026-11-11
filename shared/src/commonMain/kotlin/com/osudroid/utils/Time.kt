package com.osudroid.utils

import kotlin.time.Clock

/**
 * The current time, in milliseconds since the Unix epoch, as `System.currentTimeMillis` returns it.
 *
 * It's the wall clock, which can jump when the device's time changes. To measure how long something
 * takes, use [kotlin.time.TimeSource.Monotonic] instead.
 */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
