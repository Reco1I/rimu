package com.osudroid.utils

actual fun sleepBlocking(millis: Long) {
    try {
        Thread.sleep(millis)
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
    }
}
