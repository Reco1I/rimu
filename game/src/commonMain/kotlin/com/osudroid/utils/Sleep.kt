package com.osudroid.utils

/**
 * Blocks the current thread for [millis] milliseconds. An interruption ends it early.
 */
expect fun sleepBlocking(millis: Long)
